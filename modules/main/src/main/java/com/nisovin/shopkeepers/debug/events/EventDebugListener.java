package com.nisovin.shopkeepers.debug.events;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.EventExecutor;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Registers event handlers at every event priority for a particular type of event and then invokes a callback whenever
 * one of these event handlers is invoked.
 * <p>
 * The callback can for example be used to step by step debug the behavior of event handlers at different priorities for
 * a particular type of event.
 *
 * @param <E>
 *            the event type
 */
public class EventDebugListener<E extends Event> implements Listener {

	public interface EventHandler<E extends Event> {
		public void handleEvent(EventPriority priority, E event);
	}

	private final Class<E> eventClass;
	private final Map<EventPriority, EventExecutor> executors = new EnumMap<>(EventPriority.class);

	public EventDebugListener(Class<E> eventClass, EventHandler<E> eventHandler) {
		Validate.notNull(eventClass, "eventClass is null");
		Validate.notNull(eventHandler, "eventHandler is null");
		this.eventClass = eventClass;

		// Create event executors for every event priority:
		for (EventPriority priority : EventPriority.values()) {
			executors.put(priority, new EventExecutor() {
				@SuppressWarnings("unchecked")
				@Override
				public void execute(Listener listener, Event event) throws EventException {
					eventHandler.handleEvent(priority, (E) event);
				}
			});
		}
	}

	public void register() {
		for (Entry<EventPriority, EventExecutor> entry : executors.entrySet()) {
			EventPriority priority = entry.getKey();
			EventExecutor executor = entry.getValue();
			Bukkit.getPluginManager().registerEvent(eventClass, this, priority, executor, SKShopkeepersPlugin.getInstance(), false);
		}
	}

	public void unregister() {
		HandlerList.unregisterAll(this);
	}
}
