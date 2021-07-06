package com.nisovin.shopkeepers.ui;

import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Handles one specific type of user interface window.
 */
public abstract class UIHandler {

	private final AbstractUIType uiType;

	// Heuristic detection of automatically triggered shift left-clicks:
	private static final long AUTOMATIC_SHIFT_LEFT_CLICK_NANOS = TimeUnit.MILLISECONDS.toNanos(250L);
	private long lastManualClickNanos = 0L;
	private int lastManualClickedSlotId = -1;
	private boolean isAutomaticShiftLeftClick = false;

	protected UIHandler(AbstractUIType uiType) {
		Validate.notNull(uiType, "uiType is null");
		this.uiType = uiType;
	}

	/**
	 * Gets the {@link UIType}.
	 * 
	 * @return the UI type, not <code>null</code>
	 */
	public final AbstractUIType getUIType() {
		return uiType;
	}

	/**
	 * A shortcut for getting the given player's current UI session.
	 * 
	 * @param player
	 *            the player
	 * @return the UI session, or <code>null</code> if there is none
	 * @see UIRegistry#getUISession(Player)
	 */
	protected final UISession getUISession(Player player) {
		return ShopkeepersPlugin.getInstance().getUIRegistry().getUISession(player);
	}

	/**
	 * Checks whether or not the given player can open this interface.
	 * <p>
	 * This may for example perform any necessary permission checks.
	 * <p>
	 * This gets for example called when a player requests this interface.
	 * 
	 * @param player
	 *            the player
	 * @param silent
	 *            <code>false</code> to inform the player when the access is denied
	 * @return <code>true</code> if the given player is allowed to open this interface
	 */
	public abstract boolean canOpen(Player player, boolean silent);

	/**
	 * Opens the interface window for the given player.
	 * <p>
	 * Generally {@link #canOpen(Player, boolean)} should be checked before calling this method. However, this method
	 * should not rely on that.
	 * 
	 * @param player
	 *            a player
	 * @return <code>true</code> if the interface window was successfully opened
	 */
	protected abstract boolean openWindow(Player player);

	/**
	 * Checks whether or not the given inventory view is managed by this UI handler.
	 * <p>
	 * The UI registry already keeps track of a player's currently open UI. This additional check verifies,
	 * heuristically, in a best-effort manner, that the inventory view the player is interacting with actually
	 * corresponds to the inventory view expected by this UI handler. The result of this method is checked before any
	 * inventory events are passed through to this handler.
	 * 
	 * @param view
	 *            an inventory view
	 * @return <code>true</code> if the given inventory view has been opened and is handled by this UI handler
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
		SKUISession session = SKShopkeepersPlugin.getInstance().getUIRegistry().getUISession(player);
		if (session == null || session.getUIHandler() != this) return false;
		return (Settings.disableInventoryVerification || this.isWindow(player.getOpenInventory()));
	}

	/**
	 * Gets called when this UI gets closed for a player.
	 * <p>
	 * The corresponding inventory close event might be <code>null</code> if the UI session is ended for a different
	 * reason (eg. due to an {@link UISession#abort()}).
	 * 
	 * @param player
	 *            the player
	 * @param closeEvent
	 *            the inventory closing event, can be <code>null</code>
	 */
	protected void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
	}

	// Handling of interface window interactions

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
	 * shift left-clicks that occur within {@link UIHandler#AUTOMATIC_SHIFT_LEFT_CLICK_NANOS} on a slot different to the
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

	// Called by UIListener.
	void informOnInventoryClickEarly(InventoryClickEvent event, Player player) {
		// Heuristic detection of automatically triggered shift left-clicks:
		isAutomaticShiftLeftClick = false; // reset
		final long nowNanos = System.nanoTime();
		if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			if (event.getRawSlot() != lastManualClickedSlotId && (nowNanos - lastManualClickNanos) < AUTOMATIC_SHIFT_LEFT_CLICK_NANOS) {
				isAutomaticShiftLeftClick = true;
				Log.debug("  Detected automatically triggered shift left-click! (on different slot)");
			}
		}
		// Note: We reset these for all types of clicks, because when quickly switching between shift and non-shift
		// clicking we sometimes receive non-shift clicks that are followed by the automatic shift-clicks:
		if (!isAutomaticShiftLeftClick) {
			lastManualClickNanos = nowNanos;
			lastManualClickedSlotId = event.getRawSlot();
		}

		this.onInventoryClickEarly(event, player);
	}

	/**
	 * Called early ({@link EventPriority#LOW}) for {@link InventoryClickEvent InventoryClickEvents} for inventory views
	 * that are {@link #isOpen(Player) managed} by this UI handler.
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

	// Called by UIListener.
	void informOnInventoryClickLate(InventoryClickEvent event, Player player) {
		this.onInventoryClickLate(event, player);
	}

	/**
	 * Called late ({@link EventPriority#HIGH}) for {@link InventoryClickEvent InventoryClickEvents} for inventory views
	 * that are {@link #isOpen(Player) managed} by this UI handler.
	 * 
	 * @param event
	 *            the inventory click event
	 * @param player
	 *            the clicking player
	 * @see #onInventoryClickEarly(InventoryClickEvent, Player)
	 */
	protected void onInventoryClickLate(InventoryClickEvent event, Player player) {
	}

	// Called by UIListener.
	void informOnInventoryDragEarly(InventoryDragEvent event, Player player) {
		this.onInventoryDragEarly(event, player);
	}

	/**
	 * Called early ({@link EventPriority#LOW}) for {@link InventoryDragEvent InventoryDragEvents} for inventory views
	 * that are {@link #isOpen(Player) managed} by this UI handler.
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

	// Called by UIListener.
	void informOnInventoryDragLate(InventoryDragEvent event, Player player) {
		this.onInventoryDragLate(event, player);
	}

	/**
	 * Called late ({@link EventPriority#HIGH}) for {@link InventoryDragEvent InventoryDragEvents} for inventory views
	 * that are {@link #isOpen(Player) managed} by this UI handler.
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
