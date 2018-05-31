package com.nisovin.shopkeepers.api;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

public interface ShopObject {

	public ShopObjectType<?> getObjectType();

	public boolean spawn();

	public boolean isActive();

	/**
	 * Gets an unique id for this shop object.
	 * <p>
	 * This id can change when the shop object (ex. shop entity) gets respawned.
	 * 
	 * @return the shop object id, or <code>null</code> if the shopkeeper is currently not active
	 */
	public String getId();

	public Location getActualLocation();

	// sets the name of the shop object directly
	// naming of the shopkeeper should usually be through the Shopkeeper
	public void setName(String name);

	public int getNameLengthLimit();

	/**
	 * Equips the given item.
	 * 
	 * <p>
	 * Might not be supported by all types of shop objects.
	 * 
	 * @param item
	 *            the item
	 */
	public void setItem(ItemStack item);

	public void despawn();

	public void delete();

	public ItemStack getSubTypeItem();

	public void cycleSubType();
}
