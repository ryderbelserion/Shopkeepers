package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.ShopCreationData;

// TODO remove this and replace it with a general event that is called for all shopkeeper types
// or let it extend a general event
/**
 * This event is called whenever a player attempts to create a player shopkeeper.
 * <p>
 * This is called before the max shops limit is checked for the player. The spawn location and player's max shops can be
 * modified.
 * <p>
 * If this event is cancelled, the shop will not be created.
 */
public class CreatePlayerShopkeeperEvent extends Event implements Cancellable {

	private final ShopCreationData creationData;

	private int maxShops;
	private boolean cancelled;

	public CreatePlayerShopkeeperEvent(ShopCreationData creationData, int maxShops) {
		this.creationData = creationData;
		this.maxShops = maxShops;
	}

	/**
	 * Gets the shop creation data.
	 * 
	 * @return the shop creation data
	 */
	public ShopCreationData getShopCreationData() {
		return creationData;
	}

	/**
	 * Gets the maximum number of shops the player can have.
	 * 
	 * @return the player's max shops
	 */
	public int getMaxShopsForPlayer() {
		return maxShops;
	}

	/**
	 * Sets the maximum number of shops the creating player can have.
	 * <p>
	 * If the player has more shops than this, the shop will not be created.
	 * 
	 * @param maxShops
	 *            the player's max shops
	 */
	public void setMaxShopsForPlayer(int maxShops) {
		this.maxShops = maxShops;
	}

	/**
	 * If cancelled the shop won't be created.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the shop won't be created.
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
