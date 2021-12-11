package com.nisovin.shopkeepers.shopobjects.entity;

import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;

/**
 * Produces the default {@link AbstractShopObject#getId() shop object ids} used by {@link EntityShopObjectType entity
 * shop object types}.
 * <p>
 * Even though there this is not guaranteed or required, it is recommended that entity shop object types use these
 * shared default object ids, since it allows for a quick lookup of the shopkeeper for a given entity (without having to
 * query all registered entity shop object types individually). Shop object types can indicate whether they use these
 * default ids via {@link AbstractEntityShopObjectType#usesDefaultObjectIds()}.
 */
public class DefaultEntityShopObjectIds {

	private DefaultEntityShopObjectIds() {
	}

	/**
	 * Gets the default object id for the given {@link Entity}.
	 * 
	 * @param entity
	 *            the entity, not <code>null</code>
	 * @return the object id, not <code>null</code>
	 */
	public static Object getObjectId(Entity entity) {
		// We use the entity's unique id (requires no computations).
		// This is expected to be universally unique, even across UUIDs that identify other types of shop objects
		// (possibly not even entities). We also expect that a specific entity represents at most one shopkeeper
		// (regardless of the actual entity shop object type).
		// Note: We cannot use the Bukkit entity itself, since it uses the entity id for entity comparisons, which may
		// change when the entity is teleported across worlds.
		return entity.getUniqueId();
	}
}
