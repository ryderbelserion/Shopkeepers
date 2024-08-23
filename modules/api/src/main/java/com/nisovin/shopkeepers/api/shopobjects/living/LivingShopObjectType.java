package com.nisovin.shopkeepers.api.shopobjects.living;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;

/**
 * A {@link ShopObjectType} whose {@link ShopObject ShopObjects} use a specific mob type to
 * represent the shopkeepers.
 *
 * @param <T>
 *            the type of the shop objects this represents
 */
public interface LivingShopObjectType<T extends LivingShopObject> extends EntityShopObjectType<T> {

	/**
	 * Gets the {@link EntityType} that is used by the {@link ShopObject ShopObjects} of this
	 * {@link LivingShopObjectType}.
	 * 
	 * @return the used entity type
	 */
	public EntityType getEntityType();
}
