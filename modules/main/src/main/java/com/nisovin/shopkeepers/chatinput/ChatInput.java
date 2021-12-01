package com.nisovin.shopkeepers.chatinput;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.util.bukkit.EventUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Manages requests for chat input from players.
 * <p>
 * Ideally, the plugin should only use a single instance of this class because in the presence of multiple instances it
 * would no longer be clear which request the player's next chat input is meant for. However, even then the same kind of
 * conflict can also occur between different plugins that request chat inputs if they do not coordinate. This class does
 * not provide a solution for that.
 */
public class ChatInput implements Listener {

	/**
	 * Callback for requested chat input.
	 */
	// Note: If a requester requires a reference to the player or other context, it has to keep track of that itself
	// within the Request implementation.
	@FunctionalInterface
	public interface Request {

		/**
		 * This method is invoked with the player's chat message when the request is fulfilled.
		 * <p>
		 * Requests are fulfilled on the server's main thread.
		 * 
		 * @param message
		 *            the chat message, not <code>null</code>
		 */
		public void onChatInput(String message);
	}

	private final Plugin plugin;
	// ConcurrentHashMap: Chat events can occur asynchronously.
	private final Map<UUID, Request> pendingRequests = new ConcurrentHashMap<>();

	public ChatInput(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
		// Ensure that our chat event handler is always executed first:
		// Otherwise, we might run into conflicts with other chat plugins that also use the lowest event priority and
		// that might modify the chat message before we were able to process it (for example by injecting color codes).
		EventUtils.enforceExecuteFirst(AsyncPlayerChatEvent.class, EventPriority.LOWEST, this);
	}

	public void onDisable() {
		pendingRequests.clear();
	}

	/**
	 * Request a chat input from the given player.
	 * <p>
	 * The next received chat message sent by the player will be passed to the given {@link Request}.
	 * <p>
	 * Any already pending chat input request is replaced by this request.
	 * 
	 * @param player
	 *            the player
	 * @param request
	 *            the Request that receives the chat input
	 */
	public void request(Player player, Request request) {
		Validate.notNull(player, "player is null");
		Validate.notNull(request, "request is null");
		pendingRequests.put(player.getUniqueId(), request);
	}

	/**
	 * Gets any pending {@link Request} for chat input from the given player.
	 * 
	 * @param player
	 *            the player
	 * @return the request, or <code>null</code> if there is no request
	 */
	public Request getRequest(Player player) {
		Validate.notNull(player, "player is null");
		return pendingRequests.get(player.getUniqueId());
	}

	/**
	 * Checks if there is a pending request for chat input from the specified player.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if there is a pending request for chat input
	 */
	public boolean isPending(Player player) {
		return pendingRequests.containsKey(player.getUniqueId());
	}

	/**
	 * Aborts any currently pending request for chat input from the given player.
	 * <p>
	 * This has no effect if there is no pending request.
	 * <p>
	 * If you want to abort a specific type of request, make sure that the {@link #getRequest(Player) current request}
	 * is still of that type before you abort it with this method.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 */
	public void abortRequest(Player player) {
		Validate.notNull(player, "player is null");
		pendingRequests.remove(player.getUniqueId());
	}

	/**
	 * Gets and removes any pending {@link Request} for chat input from the given player.
	 * 
	 * @param player
	 *            the player
	 * @return the request, or <code>null</code> if there has been no request
	 */
	private Request getAndRemoveRequest(Player player) {
		assert player != null;
		return pendingRequests.remove(player.getUniqueId());
	}

	// Not ignoring cancelled chat: We process the chat event in all cases (even if the player has been muted, etc.).
	// Priority LOWEST: We intend to cancel the event as early as possible, so that other plugins can ignore it. We also
	// want to retrieve the chat message without other plugins modifying it first.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onChat(AsyncPlayerChatEvent event) {
		// Check if there is a request for chat input from the player:
		// If there is, the request is removed and then subsequently processed.
		Player player = event.getPlayer();
		Request request = this.getAndRemoveRequest(player);
		if (request == null) return; // There is no request for the player

		// Cancel the event so that other event handlers can ignore it:
		event.setCancelled(true);

		// Get the message:
		String message = event.getMessage();

		// Process the request on the server's main thread:
		Bukkit.getScheduler().runTask(plugin, () -> request.onChatInput(message));
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		this.abortRequest(player);
	}
}
