package com.nisovin.shopkeepers.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryInteractEvent;
import org.bukkit.inventory.InventoryView;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.Log;

class UIListener implements Listener {

	private final ShopkeepersPlugin plugin;
	private final SKUIRegistry uiRegistry;

	UIListener(ShopkeepersPlugin plugin, SKUIRegistry uiRegistry) {
		this.plugin = plugin;
		this.uiRegistry = uiRegistry;
	}

	private SKUISession getUISession(HumanEntity human) {
		if (human.getType() != EntityType.PLAYER) return null;
		Player player = (Player) human;
		return uiRegistry.getSession(player);
	}

	private boolean validateSession(InventoryInteractEvent event, Player player, SKUISession session) {
		InventoryView view = event.getView();
		UIHandler uiHandler = session.getUIHandler();

		// validate open inventory:
		if (!uiHandler.isWindow(view)) {
			// the player probably has some other inventory open, but an active session.. let's close it:
			Log.debug("Closing inventory of type " + view.getType() + " with title '" + view.getTitle()
					+ "' for " + player.getName() + ", because a different open inventory was expected for '"
					+ uiHandler.getUIType().getIdentifier() + "'.");
			event.setCancelled(true);
			Bukkit.getScheduler().runTask(plugin, () -> {
				player.closeInventory();
			});
			return false;
		}

		// validate shopkeeper:
		if (!session.getShopkeeper().isUIActive() || !session.getShopkeeper().isValid()) {
			// shopkeeper deleted, or the UIs got deactivated: ignore this click
			Log.debug("Inventory interaction by " + player.getName() + " ignored, because the window is about to get closed,"
					+ " or the shopkeeper got deleted.");
			event.setCancelled(true);
			return false;
		}

		return true;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) {
			return;
		}
		Player player = (Player) event.getPlayer();

		// inform ui registry so that it can cleanup session data:
		uiRegistry.onInventoryClose(player, event);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void onInventoryClick(InventoryClickEvent event) {
		SKUISession session = this.getUISession(event.getWhoClicked());
		if (session == null) return;
		Player player = (Player) event.getWhoClicked();

		// validate session:
		if (!this.validateSession(event, player, session)) {
			return;
		}

		InventoryView view = event.getView();
		UIHandler uiHandler = session.getUIHandler();

		// debug information:
		Log.debug("Inventory click: player=" + player.getName()
				+ ", view-type=" + view.getType() + ", view-title=" + view.getTitle()
				+ ", raw-slot-id=" + event.getRawSlot() + ", slot-id=" + event.getSlot() + ", slot-type=" + event.getSlotType()
				+ ", shift=" + event.isShiftClick() + ", hotbar key=" + event.getHotbarButton()
				+ ", left-or-right=" + (event.isLeftClick() ? "left" : (event.isRightClick() ? "right" : "unknown"))
				+ ", click-type=" + event.getClick() + ", action=" + event.getAction());

		// let the UIHandler handle the click:
		uiHandler.onInventoryClick(event, player);
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	void onInventoryDrag(InventoryDragEvent event) {
		SKUISession session = this.getUISession(event.getWhoClicked());
		if (session == null) return;
		Player player = (Player) event.getWhoClicked();

		// validate session:
		if (!this.validateSession(event, player, session)) {
			return;
		}

		InventoryView view = event.getView();
		UIHandler uiHandler = session.getUIHandler();

		// debug information:
		Log.debug("Inventory dragging: player=" + player.getName()
				+ ", view-type=" + view.getType() + ", view-title=" + view.getTitle()
				+ ", drag-type=" + event.getType());

		// let the UIHandler handle the dragging:
		uiHandler.onInventoryDrag(event, player);
	}
}
