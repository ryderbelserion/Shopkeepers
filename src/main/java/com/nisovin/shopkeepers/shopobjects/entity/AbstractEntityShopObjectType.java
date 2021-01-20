package com.nisovin.shopkeepers.shopobjects.entity;

import java.util.List;

import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;

public abstract class AbstractEntityShopObjectType<T extends AbstractEntityShopObject> extends AbstractShopObjectType<T> implements EntityShopObjectType<T> {

	protected AbstractEntityShopObjectType(String identifier, String permission) {
		super(identifier, permission);
	}

	protected AbstractEntityShopObjectType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	/**
	 * Whether shop objects of this type use the {@link DefaultEntityShopObjectIds default object ids}.
	 * <p>
	 * The return value of this method is expected to be fixed.
	 * 
	 * @return <code>true</code> if the shop objects of this type use the default object ids
	 */
	public boolean usesDefaultObjectIds() {
		return true; // We use the default object ids by default
	}

	/**
	 * Gets the object id for the given {@link Entity}.
	 * <p>
	 * This returns the {@link AbstractShopObject#getId() shop object id} that is or would be used by an active
	 * {@link ShopObject} of this type that represents the shopkeeper through the given entity.
	 * 
	 * @param entity
	 *            the entity, not <code>null</code>
	 * @return the object id, not <code>null</code>
	 */
	public Object getObjectId(Entity entity) {
		// // We use the default object ids by default:
		return DefaultEntityShopObjectIds.getObjectId(entity);
	}

	@Override
	public AbstractShopkeeper getShopkeeper(Entity entity) {
		Object objectId = this.getObjectId(entity);
		return this.getShopkeeperByObjectId(objectId);
	}

	@Override
	public boolean isShopkeeper(Entity entity) {
		return (this.getShopkeeper(entity) != null);
	}
}
