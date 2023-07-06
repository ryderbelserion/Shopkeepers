package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.ui.UIType;

/**
 * A {@link PlayerOpenUIEvent} that involves a specific {@link Shopkeeper}.
 */
public class ShopkeeperOpenUIEvent extends PlayerOpenUIEvent {

	private final Shopkeeper shopkeeper;

	/**
	 * Creates a new {@link ShopkeeperOpenUIEvent}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param uiType
	 *            the UI type, not <code>null</code>
	 * @param player
	 *            the player, not <code>null</code>
	 * @param silentRequest
	 *            <code>true</code> if this is a silent UI request
	 */
	public ShopkeeperOpenUIEvent(
			Shopkeeper shopkeeper,
			UIType uiType,
			Player player,
			boolean silentRequest
	) {
		super(uiType, player, silentRequest);
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
