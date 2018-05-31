package com.nisovin.shopkeepers;

import org.apache.commons.lang.Validate;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.types.AbstractSelectableType;

public abstract class AbstractShopType<T extends AbstractShopkeeper> extends AbstractSelectableType implements ShopType<T> {

	protected AbstractShopType(String identifier, String permission) {
		super(identifier, permission);
	}

	public abstract String getCreatedMessage();

	/**
	 * Creates a shopkeeper of this type.
	 * <p>
	 * This has to check that all data needed for the shop creation are given and valid. For example that for player
	 * shops an owner and a shop chest has been specified.
	 * 
	 * @param data
	 *            a container holding the necessary arguments (spawn location, object type, owner, etc.) for creating
	 *            this shopkeeper
	 * @return the created Shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created
	 */
	public abstract T createShopkeeper(ShopCreationData data) throws ShopkeeperCreateException;

	/**
	 * Creates the shopkeeper of this type by loading the needed data from the given configuration section.
	 * 
	 * @param config
	 *            the config section to load the shopkeeper data from
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be loaded
	 */
	public abstract T loadShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException;

	/**
	 * This needs to be called right after the creation or loading of a shopkeeper.
	 * 
	 * @param shopkeeper
	 *            the freshly created shopkeeper
	 */
	protected void registerShopkeeper(T shopkeeper) {
		shopkeeper.shopObject.onInit();
		SKShopkeepersPlugin.getInstance().registerShopkeeper(shopkeeper);
	}

	// common functions that might be useful for sub-classes:

	protected void validateCreationData(ShopCreationData creationData) throws ShopkeeperCreateException {
		Validate.notNull(creationData, "CreationData is null!");
	}

	protected void validateConfigSection(ConfigurationSection section) throws ShopkeeperCreateException {
		Validate.notNull(section, "Config section is null!");
	}
}
