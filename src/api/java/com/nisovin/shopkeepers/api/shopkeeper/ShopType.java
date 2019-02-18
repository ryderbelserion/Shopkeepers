package com.nisovin.shopkeepers.api.shopkeeper;

import com.nisovin.shopkeepers.api.types.SelectableType;

public interface ShopType<T extends Shopkeeper> extends SelectableType {

	// override to enforce that each subtype actually specifies a non-default display name
	@Override
	public abstract String getDisplayName();

	/**
	 * Gets a user-friendly short description of this shop type.
	 *
	 * @return a description
	 */
	public String getDescription();

	/**
	 * Gets a user-friendly short (but possibly multi-line) description of how to setup this shop type after creation.
	 *
	 * @return a setup description
	 */
	public String getSetupDescription();
}
