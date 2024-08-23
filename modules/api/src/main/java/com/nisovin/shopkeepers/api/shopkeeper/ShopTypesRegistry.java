package com.nisovin.shopkeepers.api.shopkeeper;

import com.nisovin.shopkeepers.api.types.SelectableTypeRegistry;

/**
 * A {@link SelectableTypeRegistry} that keeps track of registered {@link ShopType shop types}.
 *
 * @param <T>
 *            the internal type of {@link ShopType} that is managed by this registry
 */
public interface ShopTypesRegistry<T extends ShopType<?>> extends SelectableTypeRegistry<T> {
}
