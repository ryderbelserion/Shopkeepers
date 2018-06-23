package com.nisovin.shopkeepers.shopobjects.entity;

import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;

public abstract class AbstractEntityShopObjectType<T extends AbstractEntityShopObject> extends AbstractShopObjectType<T> implements EntityShopObjectType<T> {

	protected AbstractEntityShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	public String createObjectId(Entity entity) {
		if (entity == null) return null;
		return this.getIdentifier() + ":" + entity.getUniqueId();
	}
}
