package com.nisovin.shopkeepers.api.shopobjects;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.types.SelectableTypeRegistry;

/**
 * A {@link SelectableTypeRegistry} that keeps track of registered {@link ShopObjectType shop object
 * types}.
 *
 * @param <T>
 *            the internal type of {@link ShopObjectType} that is managed by this registry
 */
public interface ShopObjectTypesRegistry<T extends @NonNull ShopObjectType<?>>
		extends SelectableTypeRegistry<T> {
}
