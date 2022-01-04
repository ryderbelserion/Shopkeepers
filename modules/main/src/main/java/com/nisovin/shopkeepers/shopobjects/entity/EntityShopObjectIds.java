package com.nisovin.shopkeepers.shopobjects.entity;

import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObject;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;

/**
 * Produces the {@link AbstractShopObject#getId() shop object ids} used by {@link EntityShopObject}s.
 * <p>
 * All shop objects that represent their shopkeepers by entities are required to use these common shop object ids. This
 * ensures that lookups of the shopkeeper for a given entity are quick and do not require to individually query all
 * registered {@link EntityShopObjectType}s.
 */
public final class EntityShopObjectIds {

	/**
	 * Gets the object id for the given {@link Entity}.
	 * 
	 * @param entity
	 *            the entity, not <code>null</code>
	 * @return the object id, not <code>null</code>
	 */
	public static Object getObjectId(Entity entity) {
		// We use the entity's unique id (requires no computations).
		// This is expected to be universally unique, even across UUIDs that identify other types of shop objects
		// (possibly not even entities). We also expect that a specific entity represents at most one shopkeeper,
		// regardless of the actual entity shop object type.
		// Note: We cannot use the Bukkit entity itself as object id, because it uses the entity id for entity
		// comparisons, which can change when the entity is transformed or teleported across worlds.
		return entity.getUniqueId();
	}

	private EntityShopObjectIds() {
	}
}
