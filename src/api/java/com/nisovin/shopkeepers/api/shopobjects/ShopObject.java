package com.nisovin.shopkeepers.api.shopobjects;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.Shopkeeper;

public interface ShopObject {

	public ShopObjectType<?> getObjectType();

	// ACTIVATION

	public boolean isActive();

	/**
	 * Gets an unique id that can be used to identify this shop object while it is active.
	 * <p>
	 * The returned id may only be valid while the shop object is active, and it may change whenever the shop object
	 * gets respawned.
	 * 
	 * @return the id of the shop object, possibly <code>null</code> if it is not active currently
	 */
	public String getId();

	public boolean spawn();

	public void despawn();

	/**
	 * Gets the location this shop object is currently located at.
	 * <p>
	 * The returned location may only be valid while the shop object is active, might not match the location of the
	 * corresponding {@link Shopkeeper}, and might change over time if the shop object is able to move.
	 * 
	 * @return the location of the shop object, or <code>null</code> of the shop object is not active currently
	 */
	public Location getLocation();

	// NAMING

	/**
	 * Gets the maximum name length this shop object supports.
	 * 
	 * @return the maximum supported name length
	 */
	public int getNameLengthLimit();

	/**
	 * Prepares the given name for use for this shop object.
	 * <p>
	 * This might for example remove unsupported characters and trim the name to the maximum supported length.
	 * <p>
	 * The {@link Shopkeeper} might use this to adjust its set name during naming.
	 * 
	 * @param name
	 *            the name to prepare
	 * @return the prepared name
	 */
	public String prepareName(String name);

	/**
	 * Sets the name of the shop object.
	 * <p>
	 * This may not be supported if the shop object is not active currently.
	 * <p>
	 * The final name used might be different from the given name, due to {@link #prepareName(String) preparation} and
	 * other transformations being applied.
	 * <p>
	 * Naming of the {@link Shopkeeper} should usually be done via {@link Shopkeeper#setName(String)}. Some shop objects
	 * might not support naming at all, or setting a name different from the corresponding {@link Shopkeeper}.
	 * 
	 * @param name
	 *            the name, empty or <code>null</code> to remove the name
	 */
	public void setName(String name);

	/**
	 * Gets the name this shop object is currently using.
	 * 
	 * @return the name of this shop object, or <code>null</code> or empty if the shop object is not active or has no
	 *         name
	 */
	public String getName();

	// SUB TYPES

	public ItemStack getSubTypeItem();

	public void cycleSubType();

	// OTHER PROPERTIES

	/**
	 * Equips the given item.
	 * 
	 * <p>
	 * Might not be supported by all types of shop objects.
	 * 
	 * @param item
	 *            the item, or <code>null</code> to unequip any currently equipped item
	 */
	public void equipItem(ItemStack item);
}
