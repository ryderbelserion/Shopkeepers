package com.nisovin.shopkeepers.shopobjects;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;

public abstract class AbstractShopObject implements ShopObject {

	protected final AbstractShopkeeper shopkeeper;

	// fresh creation
	protected AbstractShopObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
	}

	@Override
	public abstract AbstractShopObjectType<?> getObjectType();

	public void load(ConfigurationSection config) {
		// nothing to load by default
	}

	public void save(ConfigurationSection config) {
		config.set("object", this.getObjectType().getIdentifier());
	}

	/**
	 * Called after the ShopObject and the Shopkeeper was fully created and loaded.
	 * Called before the underlying shopkeeper gets registered.
	 * Ideal to initialize any remaining things, like creating the citizens npc
	 * for citizens shopkeepers, if none was loaded before / is existent.
	 */
	public void onInit() {
		// nothing to do by default
	}

	public void onChunkLoad() {
		// nothing by default
	}

	public void onChunkUnload() {
		// nothing by default
	}

	@Override
	public abstract boolean spawn();

	@Override
	public abstract boolean isActive();

	@Override
	public abstract String getId();

	@Override
	public abstract Location getActualLocation();

	@Override
	public abstract void setName(String name);

	@Override
	public abstract int getNameLengthLimit();

	@Override
	public String trimToNameLength(String name) {
		if (name == null) return null;
		int lengthLimit = this.getNameLengthLimit();
		if (name.length() > lengthLimit) name = name.substring(0, lengthLimit);
		return name;
	}

	@Override
	public abstract void setItem(ItemStack item);

	/**
	 * This is periodically called for active shopkeepers.
	 * It makes sure that everything is still alright with the shop object.
	 * Ex: Attempts to respawn shop entities, teleports them back into place, informs about their removal.
	 * 
	 * @return <code>true</code> if the shopkeeper needs to be removed or freshly added to the active shopkeepers
	 */
	public abstract boolean check();

	@Override
	public abstract void despawn();

	@Override
	public abstract void delete();

	@Override
	public abstract ItemStack getSubTypeItem();

	@Override
	public abstract void cycleSubType();
}
