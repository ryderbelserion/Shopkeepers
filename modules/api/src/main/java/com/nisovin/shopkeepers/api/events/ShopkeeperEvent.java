package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.Event;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * Base class for events involving a shopkeeper.
 * <p>
 * Note: Not all events involving a shopkeeper may actually inherit from this class.
 */
public abstract class ShopkeeperEvent extends Event {

	private final Shopkeeper shopkeeper;

	/**
	 * Creates a new {@link ShopkeeperEvent}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 */
	protected ShopkeeperEvent(Shopkeeper shopkeeper) {
		Preconditions.checkNotNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	/**
	 * Gets the shopkeeper involved in this event.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	public Shopkeeper getShopkeeper() {
		return shopkeeper;
	}
}
