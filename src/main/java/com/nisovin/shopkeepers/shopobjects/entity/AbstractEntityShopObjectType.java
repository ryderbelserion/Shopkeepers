package com.nisovin.shopkeepers.shopobjects.entity;

import java.util.List;

import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;

public abstract class AbstractEntityShopObjectType<T extends AbstractEntityShopObject> extends AbstractShopObjectType<T> implements EntityShopObjectType<T> {

	protected AbstractEntityShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected AbstractEntityShopObjectType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	public String createObjectId(Entity entity) {
		if (entity == null) return null;
		return this.getIdentifier() + ":" + entity.getUniqueId();
	}
}
