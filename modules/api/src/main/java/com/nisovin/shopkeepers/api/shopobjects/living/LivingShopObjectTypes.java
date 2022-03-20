package com.nisovin.shopkeepers.api.shopobjects.living;

import java.util.Collection;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides access to the default shop object types that use mobs to represent the shopkeeper.
 */
public interface LivingShopObjectTypes {

	/**
	 * Gets the aliases for the {@link LivingShopObjectType} that corresponds to the given
	 * {@link EntityType}.
	 * 
	 * @param entityType
	 *            the entity type
	 * @return the aliases, or an empty list of there is no shop object type for the given entity
	 *         type
	 * @deprecated Use {@link #get(EntityType)} and {@link LivingShopObjectType#getAliases()}
	 *             instead.
	 */
	@Deprecated
	public List<? extends @NonNull String> getAliases(EntityType entityType);

	/**
	 * Gets all {@link LivingShopObjectType living shop object types}.
	 * 
	 * @return the shop object types
	 */
	public Collection<? extends @NonNull LivingShopObjectType<?>> getAll();

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
