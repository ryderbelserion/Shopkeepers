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

	/**
	 * Gets whether this trading recipe is marked as being out of stock.
	 * 
	 * @return <code>true</code> if this trading recipe is out of stock
	 */
	public boolean isOutOfStock();

	/**
	 * Checks if the given items are equal to the items of this trading recipe.
	 * 
	 * @param resultItem
	 *            the result item
	 * @param item1
	 *            the first item
	 * @param item2
	 *            the second item
	 * @return <code>true</code> if the items are equal
	 */
	public boolean areItemsEqual(ItemStack resultItem, ItemStack item1, ItemStack item2);

	/**
	 * Checks if the items of this and the given trading recipe are equal.
	 * 
	 * @param otherRecipe
	 *            the other trading recipe
	 * @return <code>true</code> if the items are equal
	 */
	public boolean areItemsEqual(TradingRecipe otherRecipe);
}
