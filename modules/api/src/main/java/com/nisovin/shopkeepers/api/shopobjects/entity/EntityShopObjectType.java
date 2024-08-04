package com.nisovin.shopkeepers.api.shopobjects.entity;

import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * A {@link ShopObjectType} whose {@link ShopObject}s use an {@link Entity} to represent a
 * {@link Shopkeeper}.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface EntityShopObjectType<T extends EntityShopObject> extends ShopObjectType<T> {

	/**
	 * Gets the {@link Shopkeeper} that uses a {@link ShopObject} of this type and is currently
	 * represented by the given entity.
	 * 
	 * @param entity
	 *            the entity, not <code>null</code>
	 * @return the shopkeeper, or <code>null</code> if the given entity is not a shopkeeper
	 *         currently, or if the corresponding shopkeeper is not using this type of shop object
	 */
	public @Nullable Shopkeeper getShopkeeper(Entity entity);

	/**
	 * Checks if the given entity currently represents a {@link Shopkeeper} that uses a
	 * {@link ShopObject} of this type.
	 * 
	 * @param entity
	 *            the entity, not <code>null</code>
	 * @return <code>true</code> if the entity is currently a shopkeeper that uses this type of shop
	 *         object
	 */
	public boolean isShopkeeper(Entity entity);
}
