package com.nisovin.shopkeepers.input;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.Box;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Manages requests for input from players.
 * <p>
 * This abstract base class implements the common portions of managing input requests but is
 * agnostic of the exact type of input that is requested. Requested inputs could be anything that
 * the player can do in-game, such as for example sending a chat message, interacting with a block
 * or entity, walking to some location or area, or invoking a command. It is the responsibility of
 * concrete subclasses to detect these 'inputs' and convert them to some type of result that is then
 * processed by the input request.
 * <p>
 * For most types of inputs there should usually only exist a single component (e.g. a single
 * {@link InputManager}) that manages requests and reacts to this type of input, because otherwise
 * it would not be clear which component is responsible to process a specific input. However, this
 * same kind of conflict can also occur between different plugins that request the same types of
 * inputs if they do not coordinate. This class does not provide a solution for that.
 * 
 * @param <T>
 *            the result type for the requested input
 */
// TODO Generalize this to also support inputs from other types of actors (e.g. console).
public abstract class InputManager<@NonNull T> {

	private class PlayerQuitListener implements Listener {

		PlayerQuitListener() {
		}

		@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
		void onPlayerQuitEvent(PlayerQuitEvent event) {
			Player player = event.getPlayer();
			onPlayerQuit(player);
		}
	}

	protected final Plugin plugin;

	private final Listener playerQuitListener = new PlayerQuitListener();
	private final Map<UUID, InputRequest<@NonNull T>> pendingRequests;

	public InputManager(Plugin plugin) {
		this(plugin, false);
	}

	public InputManager(Plugin plugin, boolean threadSafe) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;

		if (threadSafe) {
			// ConcurrentHashMap: Supports handling requests asynchronously.
			pendingRequests = new ConcurrentHashMap<>();
		} else {
			pendingRequests = new HashMap<>();
		}
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(playerQuitListener, plugin);
	}

	public void onDisable() {
		HandlerList.unregisterAll(playerQuitListener);
		pendingRequests.values().forEach(InputRequest::onAborted);
		pendingRequests.clear();
	}

	/**
	 * This is called when a player quits the server.
	 * <p>
	 * This can be used to perform certain cleanup related to the player. By default, this aborts
	 * any pending request for the player.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 */
	protected void onPlayerQuit(Player player) {
		this.abortRequest(player);
	}

	/**
	 * Awaits input from the given player and forwards it to the given request.
	 * <p>
	 * Any already pending request is aborted and replaced by this request.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @param request
	 *            the new request, not <code>null</code>
	 */
	public void request(Player player, InputRequest<@NonNull T> request) {
		Validate.notNull(player, "player is null");
		Validate.notNull(request, "request is null");
		InputRequest<@NonNull T> previousRequest = pendingRequests.put(player.getUniqueId(), request);
		if (previousRequest != null) {
			previousRequest.onAborted();
		}
	}

	/**
	 * Gets any pending input request for the given player.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return the request, or <code>null</code> if there is no pending request
	 */
	public @Nullable InputRequest<@NonNull T> getRequest(Player player) {
		Validate.notNull(player, "player is null");
		return pendingRequests.get(player.getUniqueId());
	}

	/**
	 * Checks if there is a pending input request for the given player.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return <code>true</code> if there is a pending request
	 */
	public boolean hasPendingRequest(Player player) {
		return pendingRequests.containsKey(player.getUniqueId());
	}

	/**
	 * Aborts any currently pending input request for the given player.
	 * <p>
	 * This has no effect if there is no pending request.
	 * <p>
	 * Be aware that any previously placed input request may have been replaced by a different
	 * request in the meantime. If you want to abort a specific request, make sure that the
	 * {@link #getRequest(Player) current request} still matches the request that you want to abort
	 * and then use {@link #abortRequest(Player, InputRequest)}.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return the aborted request, or <code>null</code> if there was none
	 */
	public @Nullable InputRequest<@NonNull T> abortRequest(Player player) {
		InputRequest<@NonNull T> request = this.removeRequest(player);
		if (request != null) {
			request.onAborted();
		}
		return request;
	}

	/**
	 * Aborts the given input request if it matches the currently pending input request of the given
	 * player.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @param request
	 *            the request to abort, not <code>null</code>
	 */
	public void abortRequest(Player player, InputRequest<@NonNull T> request) {
		Validate.notNull(player, "player is null");
		Validate.notNull(request, "request is null");
		if (pendingRequests.remove(player.getUniqueId(), request)) {
			request.onAborted();
		}
	}

	/**
	 * Gets and removes any pending input request for the given player.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return the removed request, or <code>null</code> if there was no pending request
	 */
	protected final @Nullable InputRequest<@NonNull T> removeRequest(Player player) {
		Validate.notNull(player, "player is null");
		return pendingRequests.remove(player.getUniqueId());
	}

	/**
	 * Gets and removes any pending input request for the given player if it satisfies the given
	 * {@link Predicate}.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @param predicate
	 *            the predicate that returns <code>true</code> if the request shall be removed, not
	 *            <code>null</code>
	 * @return the removed request, or <code>null</code> if there was no pending request that
	 *         satisfied the predicate
	 */
	protected final @Nullable InputRequest<@NonNull T> removeRequestIf(
			Player player,
			Predicate<? super InputRequest<@NonNull T>> predicate
	) {
		Validate.notNull(player, "player is null");
		Validate.notNull(predicate, "predicate is null");
		Box<@Nullable InputRequest<@NonNull T>> removedRequest = new Box<>();
		pendingRequests.computeIfPresent(player.getUniqueId(), (id, request) -> {
			if (predicate.test(request)) {
				// Remove:
				removedRequest.setValue(request);
				return Unsafe.uncheckedNull();
			} else {
				// Keep the previous value:
				return request;
			}
		});
		return removedRequest.getValue();
	}
}
