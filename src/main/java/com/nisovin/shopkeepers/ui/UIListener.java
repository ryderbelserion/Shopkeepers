package com.nisovin.shopkeepers.ui;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;

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
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.interaction.TestPlayerInteractEvent;

class UIListener implements Listener {

	private final SKUIRegistry uiRegistry;

	// The relation between early and late event handling are maintained via stacks, in case something (a plugin) is
	// calling these inventory interaction events recursively from within an event handler. The DUMMY_UI_HANDLER on the
	// stack indicates that the event is not being processed by any UI handler.
	private static final AbstractUIType DUMMY_UI_TYPE = new AbstractUIType("dummy", null) {
	};
	private static final UIHandler DUMMY_UI_HANDLER = new UIHandler(DUMMY_UI_TYPE) {
		@Override
		protected boolean openWindow(Player player) {
			return false;
		}

		@Override
		public boolean isWindow(InventoryView view) {
			return false;
		}

		@Override
		public boolean canOpen(Player player, boolean silent) {
			return false;
		}
	};
	private final Deque<UIHandler> clickHandlerStack = new ArrayDeque<>();
	private final Deque<UIHandler> dragHandlerStack = new ArrayDeque<>();

	UIListener(SKUIRegistry uiRegistry) {
		this.uiRegistry = uiRegistry;
	}

	private SKUISession getUISession(HumanEntity human) {
		if (human.getType() != EntityType.PLAYER) return null;
		Player player = (Player) human;
		return uiRegistry.getUISession(player);
	}

	private boolean validateSession(InventoryInteractEvent event, Player player, SKUISession session) {
		InventoryView view = event.getView();
		UIHandler uiHandler = session.getUIHandler();

		// Check if the UI got deactivated:
		if (!session.isUIActive()) {
			Log.debug(() -> "Ignoring inventory interaction by " + player.getName()
					+ ": The UI got deactivated (UI is probably about to get closed).");
			event.setCancelled(true);
			return false;
		}

		// Check if the shopkeeper still exists:
		Shopkeeper shopkeeper = session.getShopkeeper();
		if (shopkeeper != null && !shopkeeper.isValid()) {
			Log.debug(() -> "Ignoring inventory interaction by " + player.getName()
					+ ": The associated shopkeeper got deleted.");
			event.setCancelled(true);
			return false;
		}

		// Check if the inventory view matches the expected view:
		if (!Settings.disableInventoryVerification && !uiHandler.isWindow(view)) {
			// The player probably has some other inventory open, but an active session.. let's close it:
			Log.debug(() -> "Closing inventory of type " + view.getType() + " with title '" + view.getTitle()
					+ "' for " + player.getName() + ", because a different open inventory was expected for '"
					+ uiHandler.getUIType().getIdentifier() + "'.");
			event.setCancelled(true);
			session.abortDelayed();
			return false;
		}

		return true;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) {
			return;
		}
		Player player = (Player) event.getPlayer();

		// Inform UI registry so that it can cleanup session data:
		uiRegistry.onInventoryClose(player, event);
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	void onInventoryEarly(InventoryClickEvent event) {
		// The UI handler processing this click, or DUMMY_UI_HANDLER if none is processing the event:
		UIHandler uiHandler = DUMMY_UI_HANDLER;
		Player player = null; // The player, or null if there is no session
		SKUISession session = this.getUISession(event.getWhoClicked());
		if (session != null) {
			player = (Player) event.getWhoClicked();
			Player finalPlayer = player;
			assert player.equals(session.getPlayer());
			// Validate session:
			if (this.validateSession(event, player, session)) {
				uiHandler = session.getUIHandler();

				// Debug information:
				InventoryView view = event.getView();
				Log.debug(() -> "Inventory click: player=" + finalPlayer.getName()
						+ ", view-type=" + view.getType() + ", view-title=" + view.getTitle()
						+ ", raw-slot-id=" + event.getRawSlot() + ", slot-id=" + event.getSlot() + ", slot-type=" + event.getSlotType()
						+ ", shift=" + event.isShiftClick() + ", hotbar key=" + event.getHotbarButton()
						+ ", left-or-right=" + (event.isLeftClick() ? "left" : (event.isRightClick() ? "right" : "unknown"))
						+ ", click-type=" + event.getClick() + ", action=" + event.getAction()
						+ ", time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
			}
		}

		// Keep track of the processing UI handler (can be dummy):
		clickHandlerStack.push(uiHandler);

		if (uiHandler != DUMMY_UI_HANDLER) {
			// Let the UIHandler handle the click:
			uiHandler.informOnInventoryClickEarly(event, player);
		}
	}

	// Priority HIGH instead of HIGHEST, since we might cancel the event and other plugins might want to react to that
	// (see TradingHandler).
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = false)
	void onInventoryClickLate(InventoryClickEvent event) {
		UIHandler uiHandler = clickHandlerStack.pop(); // Not expected to be empty
		if (uiHandler == DUMMY_UI_HANDLER) return; // Ignore
		// It is expected that the session and UI handler determined at the beginning of the event processing are still
		// valid at this point.

		// Let the UIHandler handle the click:
		Player player = (Player) event.getWhoClicked();
		uiHandler.informOnInventoryClickLate(event, player);
	}

	@EventHandler(priority = EventPriority.LOW)
	void onInventoryDragEarly(InventoryDragEvent event) {
		// The UI handler processing this click, or DUMMY_UI_HANDLER if none is processing the event:
		UIHandler uiHandler = DUMMY_UI_HANDLER;
		Player player = null; // The player, or null if there is no session
		SKUISession session = this.getUISession(event.getWhoClicked());
		if (session != null) {
			player = (Player) event.getWhoClicked();
			Player finalPlayer = player;
			assert player.equals(session.getPlayer());
			// Validate session:
			if (this.validateSession(event, player, session)) {
				uiHandler = session.getUIHandler();

				// Debug information:
				InventoryView view = event.getView();
				Log.debug(() -> "Inventory dragging: player=" + finalPlayer.getName()
						+ ", view-type=" + view.getType() + ", view-title=" + view.getTitle()
						+ ", drag-type=" + event.getType());
			}
		}

		// Keep track of the processing UI handler (can be dummy):
		dragHandlerStack.push(uiHandler);

		if (uiHandler != DUMMY_UI_HANDLER) {
			// Let the UIHandler handle the click:
			uiHandler.informOnInventoryDragEarly(event, player);
		}
	}

	// Priority HIGH instead of HIGHEST, since we might cancel the event and other plugins might want to react to that
	// (see TradingHandler).
	@EventHandler(priority = EventPriority.HIGH)
	void onInventoryDragLate(InventoryDragEvent event) {
		UIHandler uiHandler = dragHandlerStack.pop(); // Not expected to be empty
		if (uiHandler == DUMMY_UI_HANDLER) return; // Ignore
		// It is expected that the session and UI handler determined at the beginning of the event processing are still
		// valid at this point.

		// Let the UIHandler handle the dragging:
		Player player = (Player) event.getWhoClicked();
		uiHandler.informOnInventoryDragLate(event, player);
	}

	// TODO SPIGOT-5610: The event is not firing under certain circumstances.
	// Cannot ignore cancelled events here, because the cancellation state only considers useInteractedBlock.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		// Ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEvent) return;

		// When a player interacts with a shopkeeper entity while holding an item in hand, we may first receive the
		// entity interaction event, which starts an UI session, and then the interaction event for the item.
		// To not start any item actions for the held item, we cancel any interaction events while an UI session is
		// active.
		Player player = event.getPlayer();
		SKUISession session = this.getUISession(player);
		if (session != null) {
			Log.debug(() -> "Canceling interaction of player '" + player.getName() + "' during active UI session.");
			event.setCancelled(true);
		}
	}
}
