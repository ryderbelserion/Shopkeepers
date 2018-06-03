package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.Shopkeeper;

/**
 * This event is called whenever a player attempts to open the trading window of a shopkeeper.
 * <p>
 * If the event is cancelled, the trading window will not open.
 */
public class OpenTradeEvent extends Event implements Cancellable {

	private final Player player;
	private final Shopkeeper shopkeeper;

	private boolean cancelled;

	public OpenTradeEvent(Player player, Shopkeeper shopkeeper) {
		this.player = player;
		this.shopkeeper = shopkeeper;
	}

	/**
	 * Gets the player attempting to trade.
	 * 
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the shopkeeper the player is attempting to trade with.
	 * 
	 * @return the shopkeeper
	 */
	public Shopkeeper getShopkeeper() {
		return shopkeeper;
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
