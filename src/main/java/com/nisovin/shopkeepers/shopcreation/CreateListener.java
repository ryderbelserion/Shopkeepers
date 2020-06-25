package com.nisovin.shopkeepers.shopcreation;

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
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TestPlayerInteractEvent;
import com.nisovin.shopkeepers.util.TextUtils;

/**
 * Handling usage of the creation item.
 */
class CreateListener implements Listener {

	private final SKShopkeepersPlugin plugin;
	private final ShopkeeperCreation shopkeeperCreation;

	CreateListener(SKShopkeepersPlugin plugin, ShopkeeperCreation shopkeeperCreation) {
		this.plugin = plugin;
		this.shopkeeperCreation = shopkeeperCreation;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
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
		TextUtils.sendMessage(player, Settings.msgCreationItemSelected);
	}

	// Since this might check chest access by calling another dummy interaction event, we handle (cancel) this event as
	// early as possible, so that other plugins (eg. protection plugins) can ignore it and don't handle it twice. In
	// case some other event handler managed to already cancel the event on LOWEST priority, we ignore the interaction.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		// ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEvent) return;

		// ignore if the player isn't right-clicking, or left-clicking air:
		Action action = event.getAction();
		if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK && action != Action.LEFT_CLICK_AIR) return;

		// make sure the item used is the shop creation item:
		ItemStack itemInHand = event.getItem();
		if (!Settings.isShopCreationItem(itemInHand)) {
			return;
		}

		Player player = event.getPlayer();
		Log.debug(() -> "Player " + player.getName() + " is interacting with the shop creation item");

		// ignore creative mode players:
		if (player.getGameMode() == GameMode.CREATIVE) {
			Log.debug("  Ignoring creative mode player");
			return;
		}

		// capture event's cancellation state:
		Result useItemInHand = event.useItemInHand();

		// prevent regular usage:
		// TODO are there items which would require canceling the event for all left clicks or physical interaction as
		// well?
		if (Settings.preventShopCreationItemRegularUsage && !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			Log.debug("  Preventing normal shop creation item usage");
			event.setCancelled(true);
		}

		// ignore off-hand interactions from this point on:
		// -> the item will only act as shop creation item if it is held in the main hand
		if (event.getHand() != EquipmentSlot.HAND) {
			Log.debug("  Ignoring off-hand interaction");
			return;
		}

		// Ignore if already cancelled. Resolves conflicts with other event handlers running at LOWEST priority (eg.
		// Shopkeepers' sign shop listener acts on LOWEST priority as well).
		if (useItemInHand == Result.DENY) {
			Log.debug("  Ignoring already cancelled item interaction");
			return;
		}

		// cancel interactions with our custom shop creation item:
		event.setCancelled(true);

		// get shop type:
		ShopType<?> shopType = plugin.getShopTypeRegistry().getSelection(player);
		// get shop object type:
		ShopObjectType<?> shopObjType = plugin.getShopObjectTypeRegistry().getSelection(player);

		if (shopType == null || shopObjType == null) {
			// the player cannot create shops at all:
			TextUtils.sendMessage(player, Settings.msgNoPermission);
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
		} else if (action == Action.LEFT_CLICK_AIR) {
			if (player.isSneaking()) {
				// cycle shop objects backwards:
				plugin.getShopObjectTypeRegistry().selectPrevious(player);
			} else {
				// cycle shopkeeper types backwards:
				plugin.getShopTypeRegistry().selectPrevious(player);
			}
		} else if (action == Action.RIGHT_CLICK_BLOCK) {
			Block clickedBlock = event.getClickedBlock();

			Block selectedChest = shopkeeperCreation.getSelectedChest(player);
			// validate old selected chest:
			if (selectedChest != null && !ItemUtils.isChest(selectedChest.getType())) {
				shopkeeperCreation.selectChest(player, null);
				selectedChest = null;
			}

			// handle chest selection:
			if (ItemUtils.isChest(clickedBlock.getType()) && !clickedBlock.equals(selectedChest)) {
				// check if the chest can be used for a shop:
				if (shopkeeperCreation.handleCheckChest(player, clickedBlock)) {
					// select chest:
					shopkeeperCreation.selectChest(player, clickedBlock);
					TextUtils.sendMessage(player, Settings.msgSelectedChest);
				}
			} else {
				// player shop creation:
				if (selectedChest == null) {
					// clicked a location without having a chest selected:
					TextUtils.sendMessage(player, Settings.msgMustSelectChest);
					return;
				}
				assert ItemUtils.isChest(selectedChest.getType()); // we have checked that above already

				// validate the selected shop type:
				if (!(shopType instanceof PlayerShopType)) {
					// only player shop types are allowed here:
					TextUtils.sendMessage(player, Settings.msgNoPlayerShopTypeSelected);
					return;
				}

				// determine spawn location:
				BlockFace clickedBlockFace = event.getBlockFace();
				Location spawnLocation = shopkeeperCreation.determineSpawnLocation(player, clickedBlock, clickedBlockFace);

				// create player shopkeeper:
				ShopCreationData creationData = PlayerShopCreationData.create(player, shopType, shopObjType, spawnLocation, clickedBlockFace, selectedChest);
				Shopkeeper shopkeeper = plugin.handleShopkeeperCreation(creationData);
				if (shopkeeper != null) {
					// shopkeeper creation was successful:

					// reset selected chest:
					shopkeeperCreation.selectChest(player, null);

					// manually remove creation item from player's hand after this event is processed:
					Bukkit.getScheduler().runTask(plugin, () -> {
						ItemStack newItemInMainHand = ItemUtils.descreaseItemAmount(itemInHand, 1);
						player.getInventory().setItemInMainHand(newItemInMainHand);
					});
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockDispense(BlockDispenseEvent event) {
		if (Settings.preventShopCreationItemRegularUsage && Settings.isShopCreationItem(event.getItem())) {
			Log.debug(() -> "Preventing dispensing of shop creation item at " + TextUtils.getLocationString(event.getBlock()));
			event.setCancelled(true);
			// TODO drop item instead
			// TODO only prevent it for items that have a special dispense behavior
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		this.handleEntityInteraction(event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		this.handleEntityInteraction(event);
	}

	private void handleEntityInteraction(PlayerInteractEntityEvent event) {
		if (!Settings.preventShopCreationItemRegularUsage) return;
		Player player = event.getPlayer();
		if (player.getGameMode() == GameMode.CREATIVE) return; // creative mode players are ignored
		// We check the permission first since this check is fast:
		if (PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) return;
		ItemStack itemInHand = ItemUtils.getItem(player.getInventory(), event.getHand());
		if (!Settings.isShopCreationItem(itemInHand)) return;

		// Prevent the entity interaction:
		// TODO Only prevent the entity interaction if the item actually has a special entity interaction behavior.
		// The interaction result may also depend on the interacted entity. However, there is no Bukkit API yet to
		// check for this.
		Log.debug(() -> {
			if (event instanceof PlayerInteractAtEntityEvent) {
				return "Preventing interaction at entity with shop creation item for player " + TextUtils.getPlayerString(player);
			} else {
				return "Preventing entity interaction with shop creation item for player " + TextUtils.getPlayerString(player);
			}
		});
		event.setCancelled(true);
	}
}
