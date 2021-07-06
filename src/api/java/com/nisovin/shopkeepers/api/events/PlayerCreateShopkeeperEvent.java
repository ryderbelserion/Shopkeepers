package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * This event is called whenever a player attempts to create a {@link Shopkeeper}.
 * <p>
 * If this event is cancelled, the shopkeeper will not be created.
 * <p>
 * If you want to react to all shopkeeper creations, take a look at {@link ShopkeeperAddedEvent}.
 */
public class PlayerCreateShopkeeperEvent extends Event implements Cancellable {

	private final ShopCreationData creationData;
	private boolean cancelled = false;

	public PlayerCreateShopkeeperEvent(ShopCreationData creationData) {
		this.creationData = creationData;
	}

	/**
	 * Gets the {@link ShopCreationData}.
	 * 
	 * @return the shop creation data
	 */
	public ShopCreationData getShopCreationData() {
		return creationData;
	}

	/**
	 * If cancelled the shopkeeper won't be created.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the shopkeeper won't be created.
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
