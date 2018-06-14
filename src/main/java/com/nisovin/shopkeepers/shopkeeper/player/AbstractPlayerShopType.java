package com.nisovin.shopkeepers.shopkeeper.player;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.events.PlayerCreatePlayerShopkeeperEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.pluginhandlers.TownyHandler;
import com.nisovin.shopkeepers.pluginhandlers.WorldGuardHandler;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractPlayerShopType<T extends AbstractPlayerShopkeeper> extends AbstractShopType<T> implements PlayerShopType<T> {

	protected AbstractPlayerShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	@Override
	protected boolean handleSpecificShopkeeperCreation(ShopCreationData shopCreationData) {
		Validate.isTrue(shopCreationData instanceof PlayerShopCreationData,
				"Expecting " + PlayerShopCreationData.class.getName() + ", got " + shopCreationData.getClass().getName());
		PlayerShopCreationData playerShopCreationData = (PlayerShopCreationData) shopCreationData;

		// receives messages, can be null:
		Player creator = shopCreationData.getCreator();

		// check if this chest is already used by some other shopkeeper:
		if (SKShopkeepersPlugin.getInstance().getProtectedChests().isChestProtected(playerShopCreationData.getShopChest(), null)) {
			Utils.sendMessage(creator, Settings.msgShopCreateFail);
			return false;
		}
		Player owner = playerShopCreationData.getOwner();
		Location spawnLocation = shopCreationData.getSpawnLocation();

		// check worldguard:
		if (Settings.enableWorldGuardRestrictions) {
			if (!WorldGuardHandler.isShopAllowed(owner, spawnLocation)) {
				Utils.sendMessage(creator, Settings.msgShopCreateFail);
				return false;
			}
		}

		// check towny:
		if (Settings.enableTownyRestrictions) {
			if (!TownyHandler.isCommercialArea(spawnLocation)) {
				Utils.sendMessage(creator, Settings.msgShopCreateFail);
				return false;
			}
		}

		int maxShopsLimit = Settings.getMaxShops(owner);
		// call event:
		PlayerCreatePlayerShopkeeperEvent createEvent = new PlayerCreatePlayerShopkeeperEvent(shopCreationData, maxShopsLimit);
		Bukkit.getPluginManager().callEvent(createEvent);
		if (createEvent.isCancelled()) {
			Log.debug("PlayerShopkeeperCreateEvent was cancelled!");
			return false;
		} else {
			maxShopsLimit = createEvent.getMaxShopsLimit();
		}

		// count owned shops:
		if (maxShopsLimit > 0) {
			int count = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().countShopsOfPlayer(owner);
			if (count >= maxShopsLimit) {
				Utils.sendMessage(creator, Settings.msgTooManyShops);
				return false;
			}
		}
		return true;
	}

	// common functions that might be useful for sub-classes:

	@Override
	protected void validateCreationData(ShopCreationData shopCreationData) {
		super.validateCreationData(shopCreationData);
		Validate.isTrue(shopCreationData instanceof PlayerShopCreationData,
				"Expecting PlayerShopCreationData, got " + shopCreationData.getClass().getName());
	}
}
