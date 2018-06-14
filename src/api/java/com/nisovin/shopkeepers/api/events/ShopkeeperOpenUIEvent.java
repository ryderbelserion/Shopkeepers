package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.ui.UIType;

/**
 * This event is called whenever a {@link Shopkeeper} is about to open a user interface of a certain {@link UIType} for
 * a player.
 * <p>
 * If the event is cancelled, the UI will not open. However, event if the event does not get cancelled, the UI might not
 * get opened if there are other reasons preventing that.
 * <p>
 * Do not attempt to open any UIs for the player or shopkeeper during the handling of this event!
 */
public class ShopkeeperOpenUIEvent extends ShopkeeperEvent implements Cancellable {

	private final UIType uiType;
	private final Player player;
	private boolean cancelled = false;

	public ShopkeeperOpenUIEvent(Shopkeeper shopkeeper, UIType uiType, Player player) {
		super(shopkeeper);
		this.uiType = uiType;
		this.player = player;
	}

	/**
	 * Gets the {@link UIType} the player tries to open the interface for.
	 * 
	 * @return the ui type
	 */
	public UIType getUIType() {
		return uiType;
	}

	/**
	 * Gets the player attempting to open the UI.
	 * 
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * If cancelled the shop won't be opened.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the shop won't be opened.
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
