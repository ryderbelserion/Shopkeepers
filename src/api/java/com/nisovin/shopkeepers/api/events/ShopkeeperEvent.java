package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.Event;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * Represents an event involving a shopkeeper.
 */
public abstract class ShopkeeperEvent extends Event {

	protected final Shopkeeper shopkeeper;

	public ShopkeeperEvent(Shopkeeper shopkeeper) {
		this.shopkeeper = shopkeeper;
	}

	/**
	 * Gets the shopkeeper involved in this event.
	 * 
	 * @return the shopkeeper
	 */
	public Shopkeeper getShopkeeper() {
		return shopkeeper;
	}
}
