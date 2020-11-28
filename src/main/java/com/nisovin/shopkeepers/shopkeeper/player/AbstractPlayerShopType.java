package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerCreatePlayerShopkeeperEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.pluginhandlers.TownyHandler;
import com.nisovin.shopkeepers.pluginhandlers.WorldGuardHandler;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopType;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class AbstractPlayerShopType<T extends AbstractPlayerShopkeeper> extends AbstractShopType<T> implements PlayerShopType<T> {

	protected AbstractPlayerShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected AbstractPlayerShopType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	@Override
	protected boolean handleSpecificShopkeeperCreation(ShopCreationData shopCreationData) {
		assert shopCreationData instanceof PlayerShopCreationData; // Shop creation data gets validated first
		PlayerShopCreationData playerShopCreationData = (PlayerShopCreationData) shopCreationData;

		// The creator, not null here:
		Player creator = shopCreationData.getCreator();

		Location spawnLocation = shopCreationData.getSpawnLocation();

		// Validate container block:
		Block containerBlock = playerShopCreationData.getShopContainer();
		if (!ShopContainers.isSupportedContainer(containerBlock.getType())) {
			// The block is not / no longer a supported type of container:
			if (ItemUtils.isContainer(containerBlock.getType())) {
				TextUtils.sendMessage(creator, Messages.unsupportedContainer);
			} else {
				TextUtils.sendMessage(creator, Messages.invalidContainer);
			}
			return false;
		}

		// Check if the selected container is too far away:
		if (!containerBlock.getWorld().equals(spawnLocation.getWorld())
				|| (int) containerBlock.getLocation().distanceSquared(spawnLocation) > (Settings.maxContainerDistance * Settings.maxContainerDistance)) {
			TextUtils.sendMessage(creator, Messages.containerTooFarAway);
			return false;
		}

		// Check selected container:
		if (!SKShopkeepersPlugin.getInstance().getShopkeeperCreation().handleCheckContainer(creator, containerBlock)) {
			return false;
		}

		// Check WorldGuard:
		if (Settings.enableWorldGuardRestrictions) {
			if (!WorldGuardHandler.isShopAllowed(creator, spawnLocation)) {
				TextUtils.sendMessage(creator, Messages.shopCreateFail);
				return false;
			}
		}

		// Check Towny:
		if (Settings.enableTownyRestrictions) {
			if (!TownyHandler.isCommercialArea(spawnLocation)) {
				TextUtils.sendMessage(creator, Messages.shopCreateFail);
				return false;
			}
		}

		int maxShopsLimit = Settings.getMaxShopsLimit(creator);
		// Call event:
		PlayerCreatePlayerShopkeeperEvent createEvent = new PlayerCreatePlayerShopkeeperEvent(shopCreationData, maxShopsLimit);
		Bukkit.getPluginManager().callEvent(createEvent);
		if (createEvent.isCancelled()) {
			Log.debug("PlayerShopkeeperCreateEvent was cancelled!");
			return false;
		} else {
			maxShopsLimit = createEvent.getMaxShopsLimit();
		}

		// Count owned shops:
		if (maxShopsLimit != Integer.MAX_VALUE) {
			int count = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().getPlayerShopkeepersByOwner(creator.getUniqueId()).size();
			if (count >= maxShopsLimit) {
				TextUtils.sendMessage(creator, Messages.tooManyShops);
				return false;
			}
		}
		return true;
	}

	// Common functions that might be useful for sub-classes:

	@Override
	protected void validateCreationData(ShopCreationData shopCreationData) {
		super.validateCreationData(shopCreationData);
		Validate.isTrue(shopCreationData instanceof PlayerShopCreationData,
				"Expecting PlayerShopCreationData, got " + shopCreationData.getClass().getName());
	}
}
