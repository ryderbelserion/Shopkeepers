package com.nisovin.shopkeepers.shopobjects.entity;

import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;

/**
 * Produces the default {@link ShopObject#getId() shop object ids} used by {@link EntityShopObjectType entity shop
 * object types}.
 * <p>
 * Even though there is no guarantee, it is recommended that entity shop object types use these shared default object
 * ids, since it allows for quick lookup of the shopkeeper for a given entity (without having to query all registered
 * entity shop object types). Shop object types can indicate whether they uses these default ids via
 * {@link AbstractEntityShopObjectType#usesDefaultObjectIds()}.
 */
public class DefaultEntityShopObjectIds {

	private DefaultEntityShopObjectIds() {
	}

	public static String getObjectId(Entity entity) {
		if (entity == null) return null;
		return "entity:" + entity.getUniqueId();
	}
}
