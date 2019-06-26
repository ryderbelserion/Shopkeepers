package com.nisovin.shopkeepers.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;

/**
 * The component which handles one specific type of user interface window for one specific shopkeeper.
 */
public abstract class UIHandler {

	private final AbstractUIType uiType;
	private final AbstractShopkeeper shopkeeper;

	protected UIHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		this.uiType = uiType;
		this.shopkeeper = shopkeeper;
	}

	public AbstractUIType getUIType() {
		return uiType;
	}

	/**
	 * Gets the shopkeeper for which this object is handling the specific interface type for.
	 * 
	 * @return the shopkeeper
	 */
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	/**
	 * Temporary deactivates UIs for the affected shopkeeper and closes the window (inventory) for the given player
	 * after a tiny delay.
	 * 
	 * @param player
	 *            the player
	 */
	protected void closeDelayed(Player player) {
		// temporary deactivate ui and close open window delayed for this player:
		shopkeeper.deactivateUI();
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
			player.closeInventory();

			// reactivate ui:
			shopkeeper.activateUI();
		});
	}

	/**
	 * Checks whether or not the given player can open the handled interface for this shopkeeper.
	 * <p>
	 * This for example gets called when a player requests the interface for this shopkeeper. It should perform the
	 * necessary permission checks.
	 * 
	 * @param player
	 *            a player
	 * @return <code>true</code> if the given player is allowed to open the interface window type this class is handling
	 */
	protected abstract boolean canOpen(Player player);

	/**
	 * This method should open the interface window for the given player.
	 * <p>
	 * Generally {@link #canOpen(Player) canOpen} should be checked before this method gets called, however this method
	 * should not rely on that.
	 * 
	 * @param player
	 *            a player
	 * @return <code>true</code> if the interface window was successfully opened
	 */
	protected abstract boolean openWindow(Player player);

	/**
	 * Checks whether or not the given inventory view is a custom inventory created by this handler (for example by
	 * comparing the titles).
	 * <p>
	 * The UI registry already keeps track of players' currently open UI. This is an additional check that aims to
	 * verify that the inventory the player is interacting with actually corresponds to the expected UI. The result of
	 * this method gets checked before any inventory events are passed through to this handler.
	 * 
	 * @param view
	 *            an inventory view
	 * @return <code>true</code> if the given inventory view is representing a custom interface window created and
	 *         handled by this handler
	 */
	protected abstract boolean isWindow(InventoryView view);

	/**
	 * Checks if the player has this UI open currently.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if this UI is open currently
	 */
	protected final boolean isOpen(Player player) {
		SKUISession session = SKShopkeepersPlugin.getInstance().getUIRegistry().getSession(player);
		return (session != null && session.getUIHandler() == this && this.isWindow(player.getOpenInventory()));
	}

	/**
	 * Gets called when this UI gets closed for a player.
	 * <p>
	 * The corresponding inventory close event might be <code>null</code> if the UI session gets ended for a different
	 * reason.
	 * 
	 * @param player
	 *            the player
	 * @param closeEvent
	 *            the inventory closing event, can be <code>null</code>
	 */
	protected void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
	}

	// handling of interface window interaction

	/**
	 * Called early ({@link EventPriority#LOW} for InventoryClickEvent's for inventories for which
	 * {@link #isWindow(Inventory)} returned true.
	 * <p>
	 * Any UI potentially canceling the event should consider doing so early in order for other plugins to ignore the
	 * event.
	 * 
	 * @param event
	 *            the inventory click event
	 * @param player
	 *            the clicking player
	 * @see #onInventoryClickLate(InventoryClickEvent, Player)
	 */
	protected void onInventoryClickEarly(InventoryClickEvent event, Player player) {
	}

	/**
	 * Called late ({@link EventPriority#HIGH} for InventoryClickEvent's for inventories for which
	 * {@link #isWindow(Inventory)} returned true.
	 * 
	 * @param event
	 *            the inventory click event
	 * @param player
	 *            the clicking player
	 * @see #onInventoryClickEarly(InventoryClickEvent, Player)
	 */
	protected void onInventoryClickLate(InventoryClickEvent event, Player player) {
	}

	/**
	 * Called early ({@link EventPriority#LOW} for InventoryDragEvent's for inventories for which
	 * {@link #isWindow(Inventory)} returned true.
	 * <p>
	 * Any UI potentially canceling the event should consider doing so early in order for other plugins to ignore the
	 * event.
	 * 
	 * @param event
	 *            the inventory drag event
	 * @param player
	 *            the dragging player
	 * @see #onInventoryDragLate(InventoryDragEvent, Player)
	 */
	protected void onInventoryDragEarly(InventoryDragEvent event, Player player) {
	}

	/**
	 * Called late ({@link EventPriority#HIGH} for InventoryDragEvent's for inventories for which
	 * {@link #isWindow(Inventory)} returned true.
	 * 
	 * @param event
	 *            the inventory drag event
	 * @param player
	 *            the dragging player
	 * @see #onInventoryDragEarly(InventoryDragEvent, Player)
	 */
	protected void onInventoryDragLate(InventoryDragEvent event, Player player) {
	}
}
