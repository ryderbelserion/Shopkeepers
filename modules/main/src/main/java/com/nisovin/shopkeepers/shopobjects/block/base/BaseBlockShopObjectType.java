package com.nisovin.shopkeepers.shopobjects.block.base;

import java.util.List;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.shopobjects.block.AbstractBlockShopObjectType;

/**
 * An {@link AbstractBlockShopObjectType} whose block shops inherit the default behaviors of
 * {@link BaseBlockShopObject}.
 *
 * @param <T>
 *            the shop object type
 */
public abstract class BaseBlockShopObjectType<T extends BaseBlockShopObject>
		extends AbstractBlockShopObjectType<T> {

	protected BaseBlockShopObjectType(
			String identifier,
			List<? extends String> aliases,
			@Nullable String permission,
			Class<@NonNull T> shopObjectType
	) {
		super(identifier, aliases, permission, shopObjectType);
	}
}
