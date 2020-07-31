package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.ui.UIType;

/**
 * This event is called whenever a player is about to open a user interface of a certain {@link UIType}.
 * <p>
 * If the event is cancelled, the UI will not open. However, even if the event does not get cancelled, the UI might not
 * open if there are other reasons preventing that.
 * <p>
 * Do not attempt to open any UIs for the player during the handling of this event!
 */
public class PlayerOpenUIEvent extends Event implements Cancellable {

	private final UIType uiType;
	private final Player player;
	private boolean cancelled = false;

	public PlayerOpenUIEvent(UIType uiType, Player player) {
		this.uiType = uiType;
		this.player = player;
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
	 * If cancelled the UI won't open.
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

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
