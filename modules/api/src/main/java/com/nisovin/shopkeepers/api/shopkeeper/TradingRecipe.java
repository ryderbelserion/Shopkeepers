package com.nisovin.shopkeepers.api.shopkeeper;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * A trading recipe.
 * <p>
 * Instances of this are immutable.
 */
public interface TradingRecipe {

	/**
	 * Gets the result item.
	 * 
	 * @return an unmodifiable view on the result item, not <code>null</code> or empty
	 */
	public UnmodifiableItemStack getResultItem();

	/**
	 * Gets the first required item.
	 * 
	 * @return an unmodifiable view on the first required item, not <code>null</code> or empty
	 */
	public UnmodifiableItemStack getItem1();

	/**
	 * Gets the second required item.
	 * 
	 * @return an unmodifiable view on the second required item, can be <code>null</code>
	 */
	public UnmodifiableItemStack getItem2();

	/**
	 * Checks whether this trading recipe requires two input items.
	 * <p>
	 * This is a shortcut for checking if {@link #getItem2()} is not <code>null</code>.
	 * 
	 * @return <code>true</code> if this is a trading recipe with two input items
	 */
	public boolean hasItem2();

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
	public boolean areItemsEqual(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2);

	/**
	 * Checks if the items of this and the given trading recipe are equal.
	 * 
	 * @param otherRecipe
	 *            the other trading recipe
	 * @return <code>true</code> if the items are equal
	 */
	public boolean areItemsEqual(TradingRecipe otherRecipe);
}
