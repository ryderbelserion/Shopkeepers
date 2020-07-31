package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.ui.UIType;

/**
 * A {@link PlayerOpenUIEvent} which involves a specific {@link Shopkeeper}.
 */
public class ShopkeeperOpenUIEvent extends PlayerOpenUIEvent {

	private final Shopkeeper shopkeeper;

	public ShopkeeperOpenUIEvent(Shopkeeper shopkeeper, UIType uiType, Player player) {
		super(uiType, player);
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
