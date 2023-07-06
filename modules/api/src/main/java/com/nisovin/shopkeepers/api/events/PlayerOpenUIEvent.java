package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.ui.UIType;

/**
 * This event is called whenever a player is about to open a user interface of a certain
 * {@link UIType}.
 * <p>
 * If the event is cancelled, the UI will not open. However, even if the event does not get
 * cancelled, the UI might not open if there are other reasons preventing that.
 * <p>
 * Do not attempt to open any UIs for the player during the handling of this event!
 */
public class PlayerOpenUIEvent extends Event implements Cancellable {

	private final UIType uiType;
	private final Player player;
	private final boolean silentRequest;
	private boolean cancelled = false;

	/**
	 * Creates a new {@link PlayerOpenUIEvent}.
	 * 
	 * @param uiType
	 *            the UI type, not <code>null</code>
	 * @param player
	 *            the player, not <code>null</code>
	 * @param silentRequest
	 *            <code>true</code> if this is a silent UI request
	 */
	public PlayerOpenUIEvent(UIType uiType, Player player, boolean silentRequest) {
		Preconditions.checkNotNull(uiType, "uiType is null");
		Preconditions.checkNotNull(player, "player is null");
		this.uiType = uiType;
		this.player = player;
		this.silentRequest = silentRequest;
	}

	/**
	 * Gets the {@link UIType} the player tries to open.
	 * 
	 * @return the UI type, not <code>null</code>
	 */
	public UIType getUIType() {
		return uiType;
	}

	/**
	 * Gets the player attempting to open the UI.
	 * 
	 * @return the player, not <code>null</code>
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Checks if this UI request is silent.
	 * <p>
	 * A silent request is not supposed to produce any output if it fails. For example, the player
	 * is not supposed to receive a 'You do not have the permission to access this UI'-message if
	 * access is denied due to a missing permission.
	 * <p>
	 * Silent UI requests may for example be used if there is a default fallback behavior that is
	 * supposed to be used for players who do not have access to a specific type of UI.
	 * 
	 * @return <code>true</code> if this is a silent UI request
	 */
	public boolean isSilentRequest() {
		return silentRequest;
	}

	/**
	 * If cancelled, the UI won't open.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the UI won't open.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * Gets the {@link HandlerList} of this event.
	 * 
	 * @return the handler list
	 */
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
