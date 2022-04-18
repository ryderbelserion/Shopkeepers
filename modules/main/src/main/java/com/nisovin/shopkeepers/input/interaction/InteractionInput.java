package com.nisovin.shopkeepers.input.interaction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.input.InputManager;
import com.nisovin.shopkeepers.input.InputRequest;
import com.nisovin.shopkeepers.util.bukkit.EventUtils;
import com.nisovin.shopkeepers.util.interaction.TestPlayerInteractEntityEvent;
import com.nisovin.shopkeepers.util.interaction.TestPlayerInteractEvent;

/**
 * Manages requests for interaction input from players.
 * <p>
 * Requests can react to {@link PlayerInteractEvent}s and/or {@link PlayerInteractEntityEvent}s,
 * including some supported subtypes such as {@link PlayerInteractAtEntityEvent}s. The special
 * {@link TestPlayerInteractEvent} and {@link TestPlayerInteractEntityEvent} are always ignored and
 * not used to fulfill interaction requests.
 * <p>
 * Often, requests are only interested in some kinds of interactions and may want to ignore all
 * other interactions. The dedicated {@link Request} type allows the request to filter the
 * interactions that it is interested in. Additionally, this type provides type-safe callback
 * methods depending on whether the request is fulfilled by a {@link PlayerInteractEvent}s or a
 * {@link PlayerInteractEntityEvent}.
 * <p>
 * The interaction events are forwarded to the request immediately from within the event handling
 * context, so that the request can decide how to process it. This includes the decision on whether
 * the event shall be cancelled or not: The events are not automatically cancelled.
 */
public class InteractionInput extends InputManager<@NonNull Event>
		implements Listener {

	/**
	 * An {@link InputRequest} for interaction inputs with support for interaction filtering and
	 * type-safe callback methods.
	 * <p>
	 * By default, {@link Request} only reacts to {@link PlayerInteractEvent}s.
	 */
	public interface Request extends InputRequest<@NonNull Event> {

		/**
		 * Tests if this request accepts the given {@link PlayerInteractEvent}.
		 * <p>
		 * When an interaction is accepted, it is used to fulfill this request. When an interaction
		 * is not accepted, this request remains active and waits for the next interaction.
		 * 
		 * @param event
		 *            the event, not <code>null</code>
		 * @return <code>true</code> if the given event is accepted by this request
		 */
		public default boolean accepts(PlayerInteractEvent event) {
			return true;
		}

		/**
		 * Tests if this request accepts the given {@link PlayerInteractEntityEvent}.
		 * <p>
		 * When an interaction is accepted, it is used to fulfill this request. When an interaction
		 * is not accepted, this request remains active and waits for the next interaction.
		 * 
		 * @param event
		 *            the event, not <code>null</code>
		 * @return <code>true</code> if the given event is accepted by this request
		 */
		public default boolean accepts(PlayerInteractEntityEvent event) {
			return false; // Ignored by default
		}

		/**
		 * This is invoked when the request is fulfilled by a {@link PlayerInteractEvent}.
		 * 
		 * @param event
		 *            the {@link PlayerInteractEvent}, not <code>null</code>
		 */
		public default void onInteract(PlayerInteractEvent event) {
		}

		/**
		 * This is invoked when the request is fulfilled by a {@link PlayerInteractEntityEvent}.
		 * 
		 * @param event
		 *            the {@link PlayerInteractEntityEvent}, not <code>null</code>
		 */
		public default void onEntityInteract(PlayerInteractEntityEvent event) {
		}

		/**
		 * {@inheritDoc}
		 * <p>
		 * The default implementation forwards the interaction event based on its type to the
		 * respective callback method.
		 */
		@Override
		public default void onInput(Event input) {
			if (input instanceof PlayerInteractEvent) {
				this.onInteract((PlayerInteractEvent) input);
			} else if (input instanceof PlayerInteractEntityEvent) {
				this.onEntityInteract((PlayerInteractEntityEvent) input);
			} else {
				throw new IllegalArgumentException("Invalid interaction input: "
						+ input.getClass().getName());
			}
		}
	}

	public InteractionInput(Plugin plugin) {
		super(plugin);
	}

	@Override
	public void onEnable() {
		super.onEnable();
		Bukkit.getPluginManager().registerEvents(this, plugin);

		// Ensure that our event handlers are always executed first:
		// This also ensures that our event handlers are executed before other event handlers of the
		// Shopkeepers plugin (e.g. shopkeeper creation and sign shop interactions).
		EventUtils.enforceExecuteFirst(PlayerInteractEvent.class, EventPriority.LOWEST, this);
		EventUtils.enforceExecuteFirst(PlayerInteractEntityEvent.class, EventPriority.LOWEST, this);
		EventUtils.enforceExecuteFirst(PlayerInteractAtEntityEvent.class, EventPriority.LOWEST, this);
	}

	@Override
	public void onDisable() {
		super.onDisable();
		HandlerList.unregisterAll(this);
	}

	// We don't ignore cancelled events.
	// Priority LOWEST: The request may want to cancel the event as early as possible, so that other
	// plugins can ignore it.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		if (event instanceof TestPlayerInteractEvent) return;

		this.handleInteraction(event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		if (event instanceof TestPlayerInteractEntityEvent) return;

		this.handleInteraction(event);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
		this.onPlayerInteractEntity(event);
	}

	private void handleInteraction(PlayerEvent event) {
		// Check if there is a request for the player that reacts to the event:
		// If there is a request, it is removed and then subsequently processed.
		Player player = event.getPlayer();
		InputRequest<@NonNull Event> request = this.removeRequestIf(player, req -> {
			return this.isInteractionAccepted(req, event);
		});
		if (request == null) return; // There is no request for the player

		// Process the request, immediately, so that the request can affect the outcome of the
		// event:
		// We don't automatically cancel the event here, because the request may want to not cancel
		// it. Also, the event may already be cancelled when it is called, so there would be no way
		// for the request to distinguish whether the event was cancelled by us or whether it was
		// already cancelled beforehand.
		request.onInput(event);
	}

	private boolean isInteractionAccepted(InputRequest<@NonNull Event> request, Event event) {
		if (!(request instanceof Request)) return true; // No filters

		Request interactRequest = (Request) request;
		if (event instanceof PlayerInteractEvent) {
			return interactRequest.accepts((PlayerInteractEvent) event);
		} else if (event instanceof PlayerInteractEntityEvent) {
			return interactRequest.accepts((PlayerInteractEntityEvent) event);
		} else {
			throw new IllegalArgumentException("Unexpected interaction event: "
					+ event.getClass().getName());
		}
	}
}
