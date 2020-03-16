package com.nisovin.shopkeepers.ui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.Log;

/**
 * The component which handles one specific type of user interface window for one specific shopkeeper.
 */
public abstract class UIHandler {

	private final AbstractUIType uiType;
	private final AbstractShopkeeper shopkeeper;

	// heuristic detection of automatically triggered shift left-clicks:
	private static final long AUTOMATIC_SHIFT_LEFT_CLICK_MS = 250L;
	private long lastManualClick = 0L;
	private int lastManualClickedSlotId = -1;
	private boolean isAutomaticShiftLeftClick = false;

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

	// handling of interface window interactions

	/**
	 * Returns whether the currently handled inventory click is, according to our heuristic, an automatically triggered
	 * shift left-click due to a shift double left-click by the player.
	 * <p>
	 * Shift double left-clicks are supposed to move all matching items to the other inventory. Minecraft implements
	 * this by triggering a shift left-click with {@link InventoryAction#MOVE_TO_OTHER_INVENTORY} for all inventory
	 * slots that contain a matching item. Plugins cannot differentiate between these automatically triggered clicks and
	 * regular shift left-clicks by the player.
	 * <p>
	 * We use a heuristic to detect (and then possibly ignore) these automatically triggered clicks: We assume that any
	 * shift left-clicks that occur within {@link UIHandler#AUTOMATIC_SHIFT_LEFT_CLICK_MS} on a slot different to the
	 * previously clicked slot are automatically triggered.
	 * <p>
	 * Limitations (TODO): We cannot use a much lower time span (eg. limiting it to 1 or 2 ticks), because the
	 * automatically triggered clicks may arrive quite some time later (up to 150 ms later on a local server and
	 * possibly more with network delay involved). Also, this does not work for automatic clicks triggered for the same
	 * slot. Since the automatically triggered clicks may arrive quite some time later, we cannot differentiate them
	 * from manual fast clicking.
	 * 
	 * @return <code>true</code> if we detected an automatically triggered shift left-click
	 */
	protected boolean isAutomaticShiftLeftClick() {
		return isAutomaticShiftLeftClick;
	}

	// Called by UIListener
	void informOnInventoryClickEarly(InventoryClickEvent event, Player player) {
		// heuristic detection of automatically triggered shift left-clicks:
		isAutomaticShiftLeftClick = false; // reset
		final long now = (System.nanoTime() / 1000000L);
		if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			if (event.getRawSlot() != lastManualClickedSlotId && (now - lastManualClick) < AUTOMATIC_SHIFT_LEFT_CLICK_MS) {
				isAutomaticShiftLeftClick = true;
				Log.debug("  Detected automatically triggered shift left-click! (on different slot)");
			}
		}
		// Note: We reset these for all types of clicks, because when quickly switching between shift and non-shift
		// clicking we sometimes receive non-shift clicks that are followed by the automatic shift-clicks:
		if (!isAutomaticShiftLeftClick) {
			lastManualClick = now;
			lastManualClickedSlotId = event.getRawSlot();
		}

		this.onInventoryClickEarly(event, player);
	}

	/**
	 * Called early ({@link EventPriority#LOW} for InventoryClickEvent's for inventories for which
	 * {@link #isWindow(InventoryView)} returned true.
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

	// Called by UIListener
	void informOnInventoryClickLate(InventoryClickEvent event, Player player) {
		this.onInventoryClickLate(event, player);
	}

	/**
	 * Called late ({@link EventPriority#HIGH} for InventoryClickEvent's for inventories for which
	 * {@link #isWindow(InventoryView)} returned true.
	 * 
	 * @param event
	 *            the inventory click event
	 * @param player
	 *            the clicking player
	 * @see #onInventoryClickEarly(InventoryClickEvent, Player)
	 */
	protected void onInventoryClickLate(InventoryClickEvent event, Player player) {
	}

	// Called by UIListener
	void informOnInventoryDragEarly(InventoryDragEvent event, Player player) {
		this.onInventoryDragEarly(event, player);
	}

	/**
	 * Called early ({@link EventPriority#LOW} for InventoryDragEvent's for inventories for which
	 * {@link #isWindow(InventoryView)} returned true.
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

	// Called by UIListener
	void informOnInventoryDragLate(InventoryDragEvent event, Player player) {
		this.onInventoryDragLate(event, player);
	}

	/**
	 * Called late ({@link EventPriority#HIGH} for InventoryDragEvent's for inventories for which
	 * {@link #isWindow(InventoryView)} returned true.
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
