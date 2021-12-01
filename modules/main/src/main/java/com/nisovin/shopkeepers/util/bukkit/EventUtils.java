package com.nisovin.shopkeepers.util.bukkit;

import java.lang.reflect.Method;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.SimplePluginManager;

import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class EventUtils {

	private EventUtils() {
	}

	/**
	 * Sets the {@link Cancellable#setCancelled(boolean) cancellation state} of the given {@link Event} if it is
	 * {@link Cancellable}.
	 * 
	 * @param event
	 *            the event, can be <code>null</code>
	 * @param cancel
	 *            the cancellation state to set
	 * @return <code>true</code> if the cancellation state has been set, <code>false</code> otherwise
	 */
	public static boolean setCancelled(Event event, boolean cancel) {
		if (event instanceof Cancellable) {
			((Cancellable) event).setCancelled(cancel);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Creates a new {@link EventExecutor} that forwards events of the specified type to the given {@link Consumer}.
	 * 
	 * @param <E>
	 *            the event type
	 * @param eventClass
	 *            the event class, not <code>null</code>
	 * @param eventConsumer
	 *            the consumer that handles the events, not <code>null</code>
	 * @return the event executor, not <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public static <E extends Event> EventExecutor eventExecutor(Class<E> eventClass, Consumer<E> eventConsumer) {
		Validate.notNull(eventClass, "eventClass is null");
		Validate.notNull(eventConsumer, "eventConsumer is null");
		return (listener, event) -> {
			if (!eventClass.isAssignableFrom(event.getClass())) {
				return;
			}
			// We already checked that the event class is assignment compatible, so this unchecked cast is safe:
			eventConsumer.accept((E) event);
		};
	}

	/**
	 * Gets the {@link HandlerList} for the specified type of event.
	 * <p>
	 * This mimics Bukkit's implementation to find and retrieve the HandlerList (see {@link SimplePluginManager}).
	 * 
	 * @param eventClass
	 *            the event class, not <code>null</code>
	 * @return the handler list, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if we cannot find or retrieve the handler list for the given event class
	 * @see #getEventRegistrationClass(Class)
	 */
	public static HandlerList getHandlerList(Class<? extends Event> eventClass) {
		// The following call checks if the event class is null, so this is not required here:
		Class<? extends Event> eventRegistrationClass = getEventRegistrationClass(eventClass);
		assert eventRegistrationClass != null;

		HandlerList handlerList;
		try {
			Method method = eventRegistrationClass.getDeclaredMethod("getHandlerList");
			method.setAccessible(true);
			handlerList = (HandlerList) method.invoke(null);
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not retrieve the handler list from the event registration class for event "
					+ eventClass.getName());
		}
		if (handlerList == null) {
			throw new IllegalArgumentException("The event registration class for event " + eventClass.getName()
					+ " returned a null handler list!");
		}
		return handlerList;
	}

	/**
	 * Gets the class that provides the {@link HandlerList} at which handlers for the specified type of event are
	 * registered.
	 * <p>
	 * This searches the given class and its parent classes for a class that provides the handler list.
	 * <p>
	 * This mimics Bukkit's implementation (see {@link SimplePluginManager}).
	 * 
	 * @param eventClass
	 *            the event class, not <code>null</code>
	 * @return the event registration class, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the event registration class cannot be found
	 */
	public static Class<? extends Event> getEventRegistrationClass(Class<? extends Event> eventClass) {
		Validate.notNull(eventClass, "eventClass is null");
		try {
			eventClass.getDeclaredMethod("getHandlerList");
			return eventClass;
		} catch (NoSuchMethodException e) {
			if (eventClass.getSuperclass() != null
					&& !eventClass.getSuperclass().equals(Event.class)
					&& Event.class.isAssignableFrom(eventClass.getSuperclass())) {
				return getEventRegistrationClass(eventClass.getSuperclass().asSubclass(Event.class));
			} else {
				throw new IllegalArgumentException("Could not find the event registration class for event "
						+ eventClass.getName());
			}
		}
	}

	/**
	 * Ensures that the event handlers of the specified {@link Listener} for the specified event at the specified
	 * {@link EventPriority} are executed prior to any other event handlers.
	 * 
	 * @param eventClass
	 *            the event class, not <code>null</code>
	 * @param eventPriority
	 *            the affected event priority, not <code>null</code>
	 * @param listener
	 *            the listener whose event handlers are to be executed first, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if we cannot find or retrieve the handler list for the given event class
	 * @see #enforceExecuteFirst(Class, EventPriority, Predicate)
	 */
	public static void enforceExecuteFirst(Class<? extends Event> eventClass, EventPriority eventPriority, Listener listener) {
		Validate.notNull(listener, "listener is null");
		enforceExecuteFirst(eventClass, eventPriority, registeredListener -> registeredListener.getListener() == listener);
	}

	/**
	 * Ensures that the event handlers of the specified {@link Plugin} for the specified event at the specified
	 * {@link EventPriority} are executed prior to any other event handlers.
	 * 
	 * @param eventClass
	 *            the event class, not <code>null</code>
	 * @param eventPriority
	 *            the affected event priority, not <code>null</code>
	 * @param plugin
	 *            the plugin whose event handlers are to be executed first, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if we cannot find or retrieve the handler list for the given event class
	 * @see #enforceExecuteFirst(Class, EventPriority, Predicate)
	 */
	public static void enforceExecuteFirst(Class<? extends Event> eventClass, EventPriority eventPriority, Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		enforceExecuteFirst(eventClass, eventPriority, registeredListener -> registeredListener.getPlugin() == plugin);
	}

	/**
	 * Ensures that the event handlers that match the specified {@link Predicate} for the specified event at the
	 * specified {@link EventPriority} are executed prior to any other event handlers.
	 * <p>
	 * This moves any other event handlers to the back of the handler list by unregistering and re-register them. The
	 * order of these other event handlers, as well as the order of the matching event handlers, preserve their
	 * execution order among each other.
	 * 
	 * @param eventClass
	 *            the event class, not <code>null</code>
	 * @param eventPriority
	 *            the affected event priority, not <code>null</code>
	 * @param affectedEventHandlers
	 *            the Predicate that specifies the event handlers that are to be executed first, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if we cannot find or retrieve the handler list for the given event class
	 */
	public static void enforceExecuteFirst(	Class<? extends Event> eventClass, EventPriority eventPriority,
											Predicate<RegisteredListener> affectedEventHandlers) {
		Validate.notNull(eventClass, "eventClass is null");

		// Retrieve the handler list:
		HandlerList handlerList = getHandlerList(eventClass);
		assert handlerList != null;

		enforceExecuteFirst(handlerList, eventClass, eventPriority, affectedEventHandlers, true);
	}

	/**
	 * Ensures that the event handlers that match the specified {@link Predicate} in the given {@link HandlerList} at
	 * the specified {@link EventPriority} are executed prior to any other event handlers.
	 * <p>
	 * This moves any other event handlers to the back of the handler list by unregistering and re-register them. The
	 * order of these other event handlers, as well as the order of the matching event handlers, preserve their
	 * execution order among each other.
	 * 
	 * @param handlerList
	 *            the handler list, not <code>null</code>
	 * @param eventClass
	 *            the event class, or <code>null</code> if unknown (this is used for additional context in log messages)
	 * @param eventPriority
	 *            the affected event priority, not <code>null</code>
	 * @param affectedEventHandlers
	 *            the Predicate that specifies the event handlers that are to be executed first, not <code>null</code>
	 * @param verbose
	 *            <code>true</code> to print additional debug output
	 */
	public static void enforceExecuteFirst(	HandlerList handlerList, Class<? extends Event> eventClass, EventPriority eventPriority,
											Predicate<RegisteredListener> affectedEventHandlers, boolean verbose) {
		Validate.notNull(handlerList, "handlerList is null");
		Validate.notNull(eventPriority, "eventPriority is null");
		Validate.notNull(affectedEventHandlers, "affectedEventHandlers is null");

		// Synchronization on the HandlerList guards against a race condition that would otherwise make it possible for
		// the listeners to miss an async event, or process it the wrong order, while the listeners are being reordered.
		synchronized (handlerList) {
			// Note: The HandlerList creates a new array whenever it detects changes to the registered listeners. We can
			// therefore safely iterate this array while doing changes to the HandlerList without being affected by
			// concurrent modifications.
			RegisteredListener[] registeredListeners = handlerList.getRegisteredListeners();
			final int eventHandlerCount = registeredListeners.length;

			// We first check if a reorder of the event handlers is even required:
			int lastAffectedEventHandlerIndex = -1;
			boolean foundUnaffectedEventHandler = false;
			boolean reorderRequired = false;
			for (int i = 0; i < eventHandlerCount; ++i) {
				RegisteredListener registeredListener = registeredListeners[i];
				// Ignore event handlers at other event priorities:
				if (registeredListener.getPriority() != eventPriority) continue;

				if (affectedEventHandlers.test(registeredListener)) {
					// We found an affected event handler:
					lastAffectedEventHandlerIndex = i;
					if (foundUnaffectedEventHandler) {
						// We already found an unaffected event handler earlier, so a reorder is required:
						reorderRequired = true;
						// No break here because we also want to find the index of the last affected event handler.
					}
				} else {
					foundUnaffectedEventHandler = true;
				}
			}
			if (!reorderRequired) {
				return;
			}
			assert lastAffectedEventHandlerIndex >= 0; // Otherwise we would not require a reorder

			// Reorder:
			for (int i = 0; i < eventHandlerCount; ++i) {
				RegisteredListener registeredListener = registeredListeners[i];
				// Ignore event handlers at other event priorities:
				if (registeredListener.getPriority() != eventPriority) continue;

				// Only unregister and re-register unaffected event handlers:
				if (affectedEventHandlers.test(registeredListener)) continue;

				// Log a debug notice:
				if (i < lastAffectedEventHandlerIndex && verbose) {
					Log.debug(() -> "Moving a handler for event "
							+ ((eventClass != null) ? "'" + eventClass.getSimpleName() + "'" : "<unspecified>")
							+ " at priority " + eventPriority.name() + " in front of an event handler of plugin "
							+ registeredListener.getPlugin().getName());
				}
				// Else: The reorder has no noticeable effect for this event handler nor the affected ones, but is still
				// required to preserve the order among all reordered event handlers.

				// Unregister and register: This moves the event handler to the back of the handler list.
				handlerList.unregister(registeredListener);
				handlerList.register(registeredListener);
			}
		}
	}

	public static void printRegisteredListeners(Event event) {
		HandlerList handlerList = event.getHandlers();
		Log.info("Registered listeners for event " + event.getEventName() + ":");
		for (RegisteredListener rl : handlerList.getRegisteredListeners()) {
			Log.info(" - " + rl.getPlugin().getName() + " (" + rl.getListener().getClass().getName() + ")"
					+ ", priority: " + rl.getPriority() + ", ignoring cancelled: " + rl.isIgnoringCancelled());
		}
	}
}
