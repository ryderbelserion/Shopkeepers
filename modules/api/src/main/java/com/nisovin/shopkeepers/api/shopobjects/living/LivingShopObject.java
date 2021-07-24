package com.nisovin.shopkeepers.api.shopobjects.living;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObject;

/**
 * A {@link ShopObject} that uses a specific mob type to represent a {@link Shopkeeper}.
 */
public interface LivingShopObject extends EntityShopObject {

	/**
	 * Gets the {@link EntityType} which is used by this {@link LivingShopObject}.
	 * 
	 * @return the used entity type
	 */
	public EntityType getEntityType();
}
