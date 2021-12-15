package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;
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

	/**
	 * Creates a new {@link PlayerCreateShopkeeperEvent}.
	 * 
	 * @param creationData
	 *            the shop creation data, not <code>null</code>
	 */
	public PlayerCreateShopkeeperEvent(ShopCreationData creationData) {
		Preconditions.checkNotNull(creationData, "creationData is null");
		this.creationData = creationData;
	}

	/**
	 * Gets the {@link ShopCreationData}.
	 * 
	 * @return the shop creation data, not <code>null</code>
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

	/**
	 * Gets the {@link HandlerList} of this event.
	 * 
	 * @return the handler list
	 */
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
