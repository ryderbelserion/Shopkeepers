package com.nisovin.shopkeepers.playershops;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.playershops.inactivity.PlayerInactivity;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Functionality related to player shops.
 */
public class PlayerShops {

	private final PlayerShopsLimit playerShopsLimit;
	private final PlayerInactivity playerInactivity;
	private final ShopOwnerNameUpdates shopOwnerNameUpdates;

	public PlayerShops(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.playerShopsLimit = new PlayerShopsLimit();
		this.playerInactivity = new PlayerInactivity(plugin);
		this.shopOwnerNameUpdates = new ShopOwnerNameUpdates(plugin);
	}

	public void onEnable() {
		playerShopsLimit.onEnable();
		playerInactivity.onEnable();
		shopOwnerNameUpdates.onEnable();
	}

	public void onDisable() {
		playerShopsLimit.onDisable();
		playerInactivity.onDisable();
		shopOwnerNameUpdates.onDisable();
	}

	public PlayerShopsLimit getPlayerShopsLimit() {
		return playerShopsLimit;
	}

	public PlayerInactivity getPlayerInactivity() {
		return playerInactivity;
	}

	public ShopOwnerNameUpdates getShopOwnerNameUpdates() {
		return shopOwnerNameUpdates;
	}
}
