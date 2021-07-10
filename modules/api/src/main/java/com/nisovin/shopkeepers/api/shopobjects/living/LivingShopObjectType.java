package com.nisovin.shopkeepers.api.shopobjects.living;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;

public interface LivingShopObjectType<T extends LivingShopObject> extends EntityShopObjectType<T> {

	/**
	 * Gets the {@link EntityType} which is used by this {@link LivingShopObjectType}.
	 * 
	 * @return the used entity type
	 */
	public EntityType getEntityType();
}
