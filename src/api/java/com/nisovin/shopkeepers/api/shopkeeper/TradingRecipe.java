package com.nisovin.shopkeepers.api.shopkeeper;

import org.bukkit.inventory.ItemStack;

/**
 * An unmodifiable trading recipe.
 */
public interface TradingRecipe {

	/**
	 * Gets the result item.
	 * 
	 * @return a copy of the result item, not <code>null</code> or empty
	 */
	public ItemStack getResultItem();

	/**
	 * Gets the first required item.
	 * 
	 * @return a copy of the first required item, not <code>null</code> or empty
	 */
	public ItemStack getItem1();

	/**
	 * Gets the second required item.
	 * 
	 * @return a copy of the second required item, can be <code>null</code>
	 */
	public ItemStack getItem2();
}
