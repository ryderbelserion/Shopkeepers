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
import com.nisovin.shopkeepers.util.Utils;

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
		if (event.getHand() != EquipmentSlot.HAND) {
			Log.debug("Ignoring off-hand interaction with creation item");
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

			Block selectedChest = shopkeeperCreation.getSelectedChest(player);
			// validate old selected chest:
			if (selectedChest != null && !ItemUtils.isChest(selectedChest.getType())) {
				shopkeeperCreation.selectChest(player, null);
				selectedChest = null;
			}

			// handle chest selection:
			if (ItemUtils.isChest(clickedBlock.getType()) && !clickedBlock.equals(selectedChest)) {
				// if chest access is denied, test again without items in hands to make sure that the event is not
				// cancelled because of denying usage with the items in hands:
				boolean chestAccessDenied = (useInteractedBlock == Result.DENY);
				if (shopkeeperCreation.handleCheckChest(player, clickedBlock, chestAccessDenied)) {
					// select chest:
					shopkeeperCreation.selectChest(player, clickedBlock);
					Utils.sendMessage(player, Settings.msgSelectedChest);
				}
			} else {
				// player shop creation:
				if (selectedChest == null) {
					// clicked a location without having a chest selected:
					Utils.sendMessage(player, Settings.msgMustSelectChest);
					return;
				}
				assert ItemUtils.isChest(selectedChest.getType()); // we have checked that above already

				// validate the selected shop type:
				if (!(shopType instanceof PlayerShopType)) {
					// only player shop types are allowed here:
					Utils.sendMessage(player, Settings.msgNoPlayerShopTypeSelected);
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
}
