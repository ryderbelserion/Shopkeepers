package com.nisovin.shopkeepers.api.shopobjects;

import org.bukkit.Location;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObject;

/**
 * Represents a {@link Shopkeeper} in the world.
 * <p>
 * A special case is {@link VirtualShopObject}, which is used if the shopkeeper is not represented
 * by any object in the world.
 */
public interface ShopObject {

	/**
	 * Gets the {@link ShopObjectType} of this shop object.
	 * 
	 * @return the shop object type
	 */
	public ShopObjectType<?> getType();

	/**
	 * Checks if this shop object is currently spawned.
	 * <p>
	 * This does not verify the state of the spawned shop object. For instance, the object used to
	 * represent the shop in the world might have been externally modified or removed since it was
	 * spawned. Use {@link #isActive()} to check for that.
	 * 
	 * @return <code>true</code> if the shop object is spawned
	 */
	public boolean isSpawned();

	/**
	 * Checks if this shop object has been {@link #isSpawned() spawned} and is still valid and
	 * present in the world.
	 * 
	 * @return <code>true</code> if the shop object is active
	 */
	public boolean isActive();

	/**
	 * Gets the current location of this shop object.
	 * <p>
	 * The returned location might not match the location of the corresponding {@link Shopkeeper},
	 * and it may change over time if the shop object is able to move.
	 * 
	 * @return the location of the shop object, or <code>null</code> if it is not spawned currently
	 */
	public @Nullable Location getLocation();

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
	 * This might for example remove unsupported characters and trim the name to the maximum
	 * supported length.
	 * 
	 * @param name
	 *            the name to prepare
	 * @return the prepared name
	 */
	public @Nullable String prepareName(@Nullable String name);

	/**
	 * Sets the name of the shop object.
	 * <p>
	 * This may have no effect if the shop object is not spawned currently.
	 * <p>
	 * The final name used might be different from the given name, due to
	 * {@link #prepareName(String) preparation} and other transformations being applied.
	 * <p>
	 * Naming of the {@link Shopkeeper} should usually be done via
	 * {@link Shopkeeper#setName(String)}. Some shop objects might not support naming at all, or
	 * setting a name different from the corresponding {@link Shopkeeper}.
	 * 
	 * @param name
	 *            the name, empty or <code>null</code> to remove the name
	 */
	public void setName(@Nullable String name);

	/**
	 * Gets the name this shop object is currently using.
	 * 
	 * @return the name of this shop object, or <code>null</code> or empty if the shop object is not
	 *         spawned or has no name
	 */
	public @Nullable String getName();
}
