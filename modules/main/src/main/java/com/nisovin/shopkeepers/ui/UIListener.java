package com.nisovin.shopkeepers.ui;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryView;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.bukkit.EventUtils;
import com.nisovin.shopkeepers.util.interaction.TestPlayerInteractEvent;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

class UIListener implements Listener {

	/**
	 * The default handled types of inventory events. Any other types of inventory events need to be explicitly
	 * requested via {@link UIHandler#getAdditionalInventoryEvents()}.
	 */
	private static final Set<Class<? extends InventoryEvent>> DEFAULT_INVENTORY_EVENTS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
			InventoryClickEvent.class,
			InventoryDragEvent.class,
			InventoryCloseEvent.class
	)));

	// This object indicates on the eventHandlerStack that no UI session is handling a particular inventory event.
	private static final Object NO_UI_SESSION = new Object();

	private final ShopkeepersPlugin plugin;
	private final SKUIRegistry uiRegistry;
	private final Set<Class<? extends Event>> handledEventTypes = new HashSet<>();

	// Stores the UI session (or NO_UI_SESSION) that handles the currently processed inventory event.
	// The handling UI session is determined once during the early processing of the event, added to the stack, and then
	// retrieved from the stack during the late processing of the event. We use a stack here to account for plugins that
	// might recursively call other inventory events from within their event handler.
	// Usually, the UI sessions are expected to still be valid during the late event handling. This assumption is in
	// accordance with the description of the InventoryClickEvent, which states that event handlers are supposed to not
	// invoke any operations that might close the player's current inventory view. However, in order to guard against
	// plugins that ignore this Bukkit API note, we check if the UI session is still valid and skip the late event
	// handling if it is not.
	private final Deque<Object> eventHandlerStack = new ArrayDeque<>();

	UIListener(ShopkeepersPlugin plugin, SKUIRegistry uiRegistry) {
		Validate.notNull(plugin, "plugin is null");
		Validate.notNull(uiRegistry, "uiRegistry is null");
		this.plugin = plugin;
		this.uiRegistry = uiRegistry;
	}

	void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		DEFAULT_INVENTORY_EVENTS.forEach(this::registerEventType);
	}

	void onDisable() {
		HandlerList.unregisterAll(this);
	}

	// Note: It is safe to dynamically register event handlers for new types of not yet handled events: Bukkit takes a
	// snapshot of the currently registered event handlers when it calls an event. Even if an event of the particular
	// type is already being processed by the server, and has already passed the early event priority phase without
	// having been noticed by this listener and accounted for in the eventHandlerStack, our newly registered event
	// handlers won't be called yet for this particular event instance, not even during the late event priority phase.
	void registerEventType(Class<? extends InventoryEvent> eventClass) {
		Validate.notNull(eventClass, "eventClass is null");
		if (handledEventTypes.contains(eventClass)) return; // Already handled
		Class<? extends Event> registrationClass = EventUtils.getEventRegistrationClass(eventClass);
		if (!handledEventTypes.add(registrationClass)) return; // Already handled as part of a parent class
		handledEventTypes.add(eventClass); // Also remember the original event class for faster lookups in the future

		// Register two new event handlers, at low and high priority, for the specified event type, and any other parent
		// event types that share the same registration class. Just in case that the registration class is unexpectedly
		// a parent instead of a subclass of InventoryEvent, the created event executors filter for InventoryEvent.
		Bukkit.getPluginManager().registerEvent(registrationClass, this, EventPriority.LOW,
				EventUtils.eventExecutor(InventoryEvent.class, this::onInventoryEventEarly), plugin, false);
		// Priority HIGH instead of HIGHEST, because we might cancel the event and other plugins might want to react to
		// that (see for example the TradingHandler).
		Bukkit.getPluginManager().registerEvent(registrationClass, this, EventPriority.HIGH,
				EventUtils.eventExecutor(InventoryEvent.class, this::onInventoryEventLate), plugin, false);
	}

	private SKUISession getUISession(HumanEntity human) {
		if (human.getType() != EntityType.PLAYER) return null;
		Player player = (Player) human;
		return uiRegistry.getUISession(player);
	}

	// Returns true if the session is valid and able to process the event.
	private boolean validateSession(SKUISession session, InventoryEvent event) {
		UIHandler uiHandler = session.getUIHandler();
		Player player = session.getPlayer();
		InventoryView view = event.getView();
		assert player.equals(view.getPlayer());

		// Check if the UI has been deactivated:
		if (!session.isUIActive()) {
			Log.debug(() -> "Ignoring inventory event of " + player.getName()
					+ ": The UI has been deactivated and is probably about to get closed.");
			EventUtils.setCancelled(event, true);
			return false;
		}

		// Check if the shopkeeper still exists:
		Shopkeeper shopkeeper = session.getShopkeeper();
		if (shopkeeper != null && !shopkeeper.isValid()) {
			Log.debug(() -> "Ignoring inventory event of " + player.getName()
					+ ": The associated shopkeeper no longer exists.");
			EventUtils.setCancelled(event, true);
			return false;
		}

		// Check if the inventory view matches the expected view:
		if (!Settings.disableInventoryVerification && !uiHandler.isWindow(view)) {
			// Something went wrong: The player seems to have an unexpected inventory open. Let's close it to prevent
			// any potential damage:
			Log.debug(() -> "Closing inventory of type " + view.getType() + " with title '" + view.getTitle()
					+ "' for " + player.getName() + ", because a different open inventory was expected for '"
					+ uiHandler.getUIType().getIdentifier() + "'.");
			EventUtils.setCancelled(event, true);
			session.abortDelayed();
			return false;
		}

		return true;
	}

	private void onInventoryEventEarly(InventoryEvent event) {
		// Check if there is an active UI session that handles this event:
		SKUISession uiSession = this.getUISession(event.getView().getPlayer());
		if (uiSession != null) {
			if (!this.validateSession(uiSession, event)) {
				uiSession = null;
			}
		}

		// Keep track of the UI session:
		eventHandlerStack.push(uiSession != null ? uiSession : NO_UI_SESSION);

		if (uiSession != null) {
			this.debugInventoryEvent(event);

			// Inform the UI handler:
			UIHandler uiHandler = uiSession.getUIHandler();
			uiHandler.informOnInventoryEventEarly(uiSession, event);
		}
	}

	private void onInventoryEventLate(InventoryEvent event) {
		Object handlingUISession = eventHandlerStack.pop(); // Not expected to be empty
		if (handlingUISession == NO_UI_SESSION) return; // Ignore the event
		SKUISession uiSession = (SKUISession) handlingUISession;

		// Check if the UI session is still valid. This is usually expected to be the case, but this assumption can be
		// violated by plugins that (incorrectly) close the inventory view during the handling of this event.
		if (!uiSession.isValid()) {
			Log.debug(() -> "Ignoring late inventory event (" + event.getClass().getSimpleName()
					+ "): UI session '" + uiSession.getUIType().getIdentifier() + "' of player "
					+ uiSession.getPlayer().getName() + " is no longer valid. Some plugin might have "
					+ "unexpectedly closed the inventory while the event was still being processed!");
			return;
		}

		// Inform the UI handler:
		UIHandler uiHandler = uiSession.getUIHandler();
		uiHandler.informOnInventoryEventLate(uiSession, event);
	}

	private void debugInventoryEvent(InventoryEvent event) {
		if (event instanceof InventoryClickEvent) {
			this.debugInventoryClickEvent((InventoryClickEvent) event);
		} else if (event instanceof InventoryDragEvent) {
			this.debugInventoryDragEvent((InventoryDragEvent) event);
		} else {
			this.debugOtherInventoryEvent(event);
		}
	}

	private void debugInventoryClickEvent(InventoryClickEvent event) {
		InventoryView view = event.getView();
		Player player = (Player) view.getPlayer();
		Log.debug(() -> "Inventory click: player=" + player.getName()
				+ ", view-type=" + view.getType() + ", view-title=" + view.getTitle()
				+ ", raw-slot-id=" + event.getRawSlot() + ", slot-id=" + event.getSlot() + ", slot-type=" + event.getSlotType()
				+ ", shift=" + event.isShiftClick() + ", hotbar key=" + event.getHotbarButton()
				+ ", left-or-right=" + (event.isLeftClick() ? "left" : (event.isRightClick() ? "right" : "unknown"))
				+ ", click-type=" + event.getClick() + ", action=" + event.getAction()
				+ ", time: " + TimeUnit.NANOSECONDS.toMillis(System.nanoTime()));
	}

	private void debugInventoryDragEvent(InventoryDragEvent event) {
		InventoryView view = event.getView();
		Player player = (Player) view.getPlayer();
		Log.debug(() -> "Inventory dragging: player=" + player.getName()
				+ ", view-type=" + view.getType() + ", view-title=" + view.getTitle()
				+ ", drag-type=" + event.getType());
	}

	private void debugOtherInventoryEvent(InventoryEvent event) {
		InventoryView view = event.getView();
		Player player = (Player) view.getPlayer();
		Log.debug(() -> "Inventory event (" + event.getClass().getSimpleName()
				+ "): player=" + player.getName() + ", view-type=" + view.getType()
				+ ", view-title=" + view.getTitle());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onInventoryClose(InventoryCloseEvent event) {
		// Inform the UI registry so that it can cleanup any corresponding UI session:
		uiRegistry.onInventoryClose(event);
	}

	// TODO SPIGOT-5610: The event is not firing under certain circumstances.
	// Cannot ignore cancelled events here, because the cancellation state only considers useInteractedBlock.
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		// Ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEvent) return;

		// When a player interacts with a shopkeeper entity while holding an item in hand, we may first receive the
		// entity interaction event, which starts an UI session, and then the interaction event for the item.
		// In order to not trigger any item actions for the held item, we cancel any interaction events while an UI
		// session is active.
		Player player = event.getPlayer();
		SKUISession session = this.getUISession(player);
		if (session != null) {
			Log.debug(() -> "Canceling interaction of player '" + player.getName() + "' during active UI session.");
			event.setCancelled(true);
		}
	}
}
