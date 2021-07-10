package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.Event;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * Base class for events involving a shopkeeper.
 * <p>
 * Note: Not all events involving a shopkeeper may actually inherit from this class.
 */
public abstract class ShopkeeperEvent extends Event {

	protected final Shopkeeper shopkeeper;

	public ShopkeeperEvent(Shopkeeper shopkeeper) {
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
