package com.nisovin.shopkeepers;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shoptypes.ShopType;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Handling usage of the creation item.
 */
class CreateListener implements Listener {

	private final SKShopkeepersPlugin plugin;

	CreateListener(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	void onItemHeld(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) return;
		ItemStack newItemInHand = player.getInventory().getItem(event.getNewSlot());
		if (!Settings.isShopCreationItem(newItemInHand)) {
			return;
		}

		if (!plugin.hasCreatePermission(player)) {
			// player cannot create any shopkeeper at all
			return;
		}

		// print info message about usage:
		Utils.sendMessage(player, Settings.msgCreationItemSelected);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		// ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEvent) return;

		// ignore if the player isn't right-clicking:
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) return;

		// ignore creative mode players:
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) return;

		// make sure the item used is the shop creation item:
		ItemStack itemInHand = event.getItem();
		if (!Settings.isShopCreationItem(itemInHand)) {
			return;
		}

		// remember previous interaction result:
		Result useInteractedBlock = event.useInteractedBlock();

		// prevent regular usage:
		// TODO are there items which would require canceling the event for left clicks or physical interaction as well?
		if (Settings.preventShopCreationItemRegularUsage && !Utils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			Log.debug("Preventing normal shop creation item usage");
			event.setCancelled(true);
		}

		// ignore off-hand interactions from this point on:
		// -> the item will only act as shop creation item if it is held in the main hand
		if (!NMSManager.getProvider().isMainHandInteraction(event)) {
			Log.debug("Ignoring off-hand interaction with creation item");
			return;
		}

		// get shop type:
		ShopType<?> shopType = plugin.getShopTypeRegistry().getSelection(player);
		// get shop object type:
		ShopObjectType<?> shopObjType = plugin.getShopObjectTypeRegistry().getSelection(player);

		if (shopType == null || shopObjType == null) {
			// TODO maybe print different kind of no-permission message, because the player cannot create shops at all:
			Utils.sendMessage(player, Settings.msgNoPermission);
			return;
		}

		// check what the player is doing with the shop creation item in hand:
		if (action == Action.RIGHT_CLICK_AIR) {
			if (player.isSneaking()) {
				// cycle shop objects:
				plugin.getShopObjectTypeRegistry().selectNext(player);
			} else {
				// cycle shopkeeper types:
				plugin.getShopTypeRegistry().selectNext(player);
			}
		} else if (action == Action.RIGHT_CLICK_BLOCK) {
			Block clickedBlock = event.getClickedBlock();

			Block selectedChest = plugin.getSelectedChest(player);
			// validate old selected chest:
			if (selectedChest != null && !ItemUtils.isChest(selectedChest.getType())) {
				plugin.selectChest(player, null);
				selectedChest = null;
			}

			// chest for chest selection:
			if (ItemUtils.isChest(clickedBlock.getType()) && !clickedBlock.equals(selectedChest)) {
				// check if the clicked chest was recently placed:
				if (Settings.requireChestRecentlyPlaced && !plugin.isRecentlyPlaced(player, clickedBlock)) {
					// chest was not recently placed:
					Utils.sendMessage(player, Settings.msgChestNotPlaced);
				} else {
					boolean chestAccessDenied = (useInteractedBlock == Result.DENY);
					if (chestAccessDenied) {
						// making sure that the chest access is really denied, and that the event
						// is not cancelled because of denying usage with the item in hand:
						player.setItemInHand(null);
						TestPlayerInteractEvent fakeInteractEvent = new TestPlayerInteractEvent(player, event.getAction(), null, clickedBlock, event.getBlockFace());
						Bukkit.getPluginManager().callEvent(fakeInteractEvent);
						chestAccessDenied = (fakeInteractEvent.useInteractedBlock() == Result.DENY);

						// resetting item in hand:
						player.setItemInHand(itemInHand);
					}

					if (chestAccessDenied) {
						Log.debug("Right-click on chest prevented, player " + player.getName() + " at " + clickedBlock.getLocation().toString());
					} else {
						// select chest:
						plugin.selectChest(player, clickedBlock);
						Utils.sendMessage(player, Settings.msgSelectedChest);
					}
				}

				event.setCancelled(true);
				return;
			} else {
				// player shop creation:
				boolean shopkeeperCreated = this.handleShopkeeperCreation(player, shopType, shopObjType, selectedChest, clickedBlock, event.getBlockFace());
				if (shopkeeperCreated) {
					// manually remove creation item from player's hand after this event is processed:
					event.setCancelled(true);
					Bukkit.getScheduler().runTask(plugin, () -> {
						ItemStack newItemInHand = ItemUtils.descreaseItemAmount(itemInHand, 1);
						player.setItemInHand(newItemInHand);
					});
				}

				// TODO maybe always prevent normal usage, also for cases in which shop creation failed because of some
				// reason, and instead optionally allow normal usage when crouching? Or, if normal usage is not denied,
				// allow shop creation only when crouching?
				// Or handle chest selection and/or shop creation on left clicks only, instead of right clicks?
				// Or with MC 1.9: Normal usage if the item is in the off-hand
			}
		}
	}

	// returns true on success
	private boolean handleShopkeeperCreation(Player player, ShopType<?> shopType, ShopObjectType<?> shopObjType, Block selectedChest, Block clickedBlock, BlockFace clickedBlockFace) {
		assert shopType != null && shopObjType != null; // has been check already

		if (selectedChest == null) {
			// clicked a location without having a chest selected:
			Utils.sendMessage(player, Settings.msgMustSelectChest);
			return false;
		}
		assert ItemUtils.isChest(selectedChest.getType()); // we have checked that above

		// check for selected chest being too far away:
		if (!selectedChest.getWorld().equals(clickedBlock.getWorld())
				|| (int) selectedChest.getLocation().distanceSquared(clickedBlock.getLocation()) > (Settings.maxChestDistance * Settings.maxChestDistance)) {
			Utils.sendMessage(player, Settings.msgChestTooFar);
			return false;
		}

		if (!shopObjType.isValidSpawnBlockFace(clickedBlock, clickedBlockFace)) {
			// invalid targeted block face:
			Utils.sendMessage(player, Settings.msgShopCreateFail);
			return false;
		}

		Block spawnBlock = clickedBlock.getRelative(clickedBlockFace);
		// check if the shop can be placed there (enough space, etc.):
		if (!shopObjType.isValidSpawnBlock(spawnBlock)) {
			// invalid spawn location:
			Utils.sendMessage(player, Settings.msgShopCreateFail);
			return true;
		}
		Location spawnLocation = spawnBlock.getLocation();

		// create player shopkeeper:
		ShopCreationData creationData = PlayerShopCreationData.create(player, shopType, shopObjType, spawnLocation, clickedBlockFace, player, selectedChest);
		Shopkeeper shopkeeper = plugin.createShopkeeper(creationData);
		if (shopkeeper == null) {
			// something else prevented this shopkeeper from being created
			return false;
		}

		// shopkeeper creation was successful:

		// reset selected chest:
		plugin.selectChest(player, null);

		return true;
	}
}
