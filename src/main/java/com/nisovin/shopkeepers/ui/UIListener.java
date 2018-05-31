package com.nisovin.shopkeepers.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.util.Log;

class UIListener implements Listener {

	private final UIRegistry uiRegistry;

	UIListener(UIRegistry uiRegistry) {
		this.uiRegistry = uiRegistry;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	void onInventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer().getType() != EntityType.PLAYER) return;
		Player player = (Player) event.getPlayer();
		UISession session = uiRegistry.getSession(player);
		if (session != null) {
			Log.debug("Player " + player.getName() + " closed " + session.getUIType().getIdentifier());
			// inform uiManager so that it can cleanup player data:
			uiRegistry.onInventoryClose(player);
			// inform uiHandler so that it can react to it:
			if (session.getUIHandler().isWindow(event.getInventory())) {
				session.getUIHandler().onInventoryClose(event, player);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked().getType() != EntityType.PLAYER) return;
		Player player = (Player) event.getWhoClicked();
		UISession session = uiRegistry.getSession(player);
		if (session != null) {
			// inform uiHandler so that it can react to it:
			Inventory inventory = event.getInventory();
			if (session.getUIHandler().isWindow(inventory)) {
				if (!session.getShopkeeper().isUIActive() || !session.getShopkeeper().isValid()) {
					// shopkeeper deleted, or the UIs got deactivated: ignore this click
					Log.debug("Inventory click by " + player.getName() + " ignored, because the window is about to get closed,"
							+ " or the shopkeeper got deleted.");
					event.setCancelled(true);
					return;
				}

				// debug information:
				Log.debug("Inventory click: player=" + player.getName()
						+ ", inventory-type=" + inventory.getType() + ", inventory-title=" + inventory.getTitle()
						+ ", raw-slot-id=" + event.getRawSlot() + ", slot-id=" + event.getSlot() + ", slot-type=" + event.getSlotType()
						+ ", shift=" + event.isShiftClick() + ", hotbar key=" + event.getHotbarButton()
						+ ", left-or-right=" + (event.isLeftClick() ? "left" : (event.isRightClick() ? "right" : "unknown"))
						+ ", click-type=" + event.getClick() + ", action=" + event.getAction());

				// let the UIHandler handle the click:
				session.getUIHandler().onInventoryClick(event, player);
			} else {
				// the player probably has some other inventory open, but an active session.. let's close it:
				Log.debug("Closing inventory for " + player.getName() + ", because different open inventory was expected."
						+ " Open inventory: " + inventory.getType() + " with name '" + inventory.getTitle() + "'");
				event.setCancelled(true);
				Bukkit.getScheduler().runTask(SKShopkeepersPlugin.getInstance(), () -> {
					uiRegistry.onInventoryClose(player); // cleanup
					player.closeInventory();
				});
			}
		}
	}
}
