package com.nisovin.shopkeepers.shopkeeper;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerCreateShopkeeperEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.types.AbstractSelectableType;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractShopType<T extends AbstractShopkeeper> extends AbstractSelectableType implements ShopType<T> {

	protected AbstractShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	/**
	 * Recreates a shopkeeper of this type by loading its previously saved data from the given config section.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param configSection
	 *            the config section
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created (ex. due to invalid or missing data)
	 */
	public abstract T loadShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException;

	/**
	 * Creates a new shopkeeper of this type by using the data from the given {@link ShopCreationData}.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param shopCreationData
	 *            the shop creation data
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created (ex. due to invalid or missing data)
	 */
	public abstract T createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException;

	// handles of shopkeeper creation by players
	// return null in case of failure
	public T handleShopkeeperCreation(ShopCreationData shopCreationData) {
		this.validateCreationData(shopCreationData);
		SKShopkeeperRegistry shopkeeperRegistry = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry();

		// receives messages, can be null:
		Player creator = shopCreationData.getCreator();
		try {
			// shop type specific handling:
			if (!this.handleSpecificShopkeeperCreation(shopCreationData)) {
				return null;
			}

			// create and spawn the shopkeeper:
			@SuppressWarnings("unchecked")
			T shopkeeper = (T) shopkeeperRegistry.createShopkeeper(shopCreationData);
			assert shopkeeper != null;

			// send creation message to creator:
			Utils.sendMessage(creator, this.getCreatedMessage());

			// save:
			shopkeeper.save();

			return shopkeeper;
		} catch (ShopkeeperCreateException e) {
			// TODO translation for unknown issues
			Utils.sendMessage(creator, "Couldn't create shopkeeper: " + e.getMessage());
			return null;
		}
	}

	// shop type specific handling of shopkeeper creation by players
	// return null in case of failure
	protected boolean handleSpecificShopkeeperCreation(ShopCreationData creationData) {
		// call event:
		PlayerCreateShopkeeperEvent createEvent = new PlayerCreateShopkeeperEvent(creationData);
		Bukkit.getPluginManager().callEvent(createEvent);
		if (createEvent.isCancelled()) {
			Log.debug("ShopkeeperCreateEvent was cancelled!");
			return false;
		}
		return true;
	}

	protected abstract String getCreatedMessage();

	// common functions that might be useful for sub-classes:

	protected void validateCreationData(ShopCreationData shopCreationData) {
		Validate.notNull(shopCreationData, "ShopCreationData is null!");
		ShopType<?> shopType = shopCreationData.getShopType();
		Validate.isTrue(this == shopType, "Expecting shop type " + this.getClass().getName()
				+ ", but got " + shopType.getClass().getName());
	}

	protected void validateConfigSection(ConfigurationSection configSection) {
		Validate.notNull(configSection, "Config section is null!");
	}
}
