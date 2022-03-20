package com.nisovin.shopkeepers.api.shopobjects.citizens;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.entity.EntityShopObjectType;

/**
 * A {@link ShopObjectType} of shop objects that use Citizens NPCs to represent the shopkeepers.
 *
 * @param <T>
 *            the type of the shop objects that this represents
 */
public interface CitizensShopObjectType<T extends @NonNull CitizensShopObject>
		extends EntityShopObjectType<T> {
}
