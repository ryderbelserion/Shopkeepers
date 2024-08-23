package com.nisovin.shopkeepers.api.shopobjects.living;

import java.util.Collection;

import org.bukkit.entity.EntityType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides access to the default shop object types that use mobs to represent the shopkeeper.
 */
public interface LivingShopObjectTypes {

	/**
	 * Gets all {@link LivingShopObjectType living shop object types}.
	 * 
	 * @return the shop object types
	 */
	public Collection<? extends LivingShopObjectType<?>> getAll();

	/**
	 * Gets the {@link LivingShopObjectType} for the given {@link EntityType}.
	 * 
	 * @param entityType
	 *            the entity type
	 * @return the corresponding {@link LivingShopObjectType}, or <code>null</code> if there is none
	 *         for the given entity type
	 */
	public @Nullable LivingShopObjectType<?> get(EntityType entityType);
}
