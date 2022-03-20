package com.nisovin.shopkeepers.api.shopobjects.entity;

import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;

/**
 * A {@link ShopObject} that uses an {@link Entity} to represent a {@link Shopkeeper} in the world.
 */
public interface EntityShopObject extends ShopObject {

	/**
	 * Gets the entity.
	 * 
	 * @return the entity, or <code>null</code> if the shop object is not spawned currently
	 */
	public @Nullable Entity getEntity();
}
