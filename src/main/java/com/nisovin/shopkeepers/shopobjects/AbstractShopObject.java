package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;

/**
 * Abstract base class for all shop object implementations.
 * <p>
 * Implementation hints:<br>
 * <ul>
 * <li>Make sure to call {@link Shopkeeper#markDirty()} on every change of data that might need to be persisted.
 * </ul>
 */
public abstract class AbstractShopObject implements ShopObject {

	protected final AbstractShopkeeper shopkeeper;

	// fresh creation
	protected AbstractShopObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
	}

	@Override
	public abstract AbstractShopObjectType<?> getType();

	public void load(ConfigurationSection configSection) {
	}

	/**
	 * Saves the shop object's data to the specified configuration section.
	 * <p>
	 * Note: The serialization of the inserted data may happen asynchronously, so make sure that this is not a problem
	 * (ex. only insert immutable objects, or always create copies of the data you insert and/or make sure to not modify
	 * the inserted objects).
	 * 
	 * @param configSection
	 *            the config section
	 */
	public void save(ConfigurationSection configSection) {
		configSection.set("type", this.getType().getIdentifier());
	}

	/**
	 * This gets called at the end of shopkeeper construction, when the shopkeeper has been loaded and setup.
	 * <p>
	 * The shopkeeper has not yet been registered at this point!
	 * <p>
	 * This can be used to perform any remaining initial shop object setup.
	 */
	public void setup() {
	}

	// LIFE CYCLE

	/**
	 * This gets called when the {@link ShopObject} is meant to be removed.
	 * <p>
	 * This can for example be used to disable any active components (ex. listeners) for this shop object.
	 */
	public void remove() {
	}

	/**
	 * This gets called when the {@link ShopObject} is meant to be permanently deleted.
	 * <p>
	 * This gets called after {@link #remove()}.
	 * <p>
	 * This can for example be used to cleanup any persistent data corresponding to this shop object.
	 */
	public void delete() {
	}

	// ACTIVATION

	public void onChunkLoad(boolean worldSaving) {
	}

	public void onChunkUnload(boolean worldSaving) {
	}

	@Override
	public abstract boolean isActive();

	@Override
	public abstract String getId();

	@Override
	public abstract boolean spawn();

	@Override
	public abstract void despawn();

	@Override
	public abstract Location getLocation();

	/**
	 * This is periodically called for active shopkeepers.
	 * <p>
	 * It makes sure that everything is still alright with the shop object.<br>
	 * Ex: Attempts to respawn shop entities, teleports them back into place, informs about their removal.
	 * 
	 * @return <code>true</code> to if the shop object might no longer be active or its id has changed
	 */
	public abstract boolean check();

	// NAMING

	@Override
	public int getNameLengthLimit() {
		return AbstractShopkeeper.MAX_NAME_LENGTH;
	}

	@Override
	public String prepareName(String name) {
		if (name == null) return null;
		// trim to max name length:
		int lengthLimit = this.getNameLengthLimit();
		if (name.length() > lengthLimit) name = name.substring(0, lengthLimit);
		return name;
	}

	@Override
	public abstract void setName(String name);

	@Override
	public abstract String getName();

	// SUB TYPES

	@Override
	public ItemStack getSubTypeItem() {
		// not supported by default
		return null;
	}

	@Override
	public void cycleSubType() {
		// not supported by default
	}

	// OTHER PROPERTIES

	@Override
	public void equipItem(ItemStack item) {
		// not supported by default
	}
}
