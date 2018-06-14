package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * This event is called whenever a player has finished editing a {@link Shopkeeper} in some way.
 */
public class ShopkeeperEditedEvent extends ShopkeeperEvent {

	private final Player player;

	public ShopkeeperEditedEvent(Shopkeeper shopkeeper, Player player) {
		super(shopkeeper);
		this.player = player;
	}

	/**
	 * The editing player.
	 * 
	 * @return the editing player
	 */
	public Player getPlayer() {
		return player;
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
