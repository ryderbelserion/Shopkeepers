package com.nisovin.shopkeepers.api.shopkeeper;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.types.SelectableTypeRegistry;

/**
 * A {@link SelectableTypeRegistry} that keeps track of registered {@link ShopType shop types}.
 *
 * @param <T>
 *            the internal type of {@link ShopType} that is managed by this registry
 */
public interface ShopTypesRegistry<T extends @NonNull ShopType<?>>
		extends SelectableTypeRegistry<T> {
}
