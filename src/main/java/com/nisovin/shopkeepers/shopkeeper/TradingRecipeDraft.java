package com.nisovin.shopkeepers.shopkeeper;

import org.bukkit.inventory.ItemStack;

import com.google.common.base.Objects;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.util.ItemUtils;

/**
 * Similar to {@link TradingRecipe}, but each item can be empty.
 * <p>
 * Empty items get normalized to <code>null</code>.
 * <p>
 * To avoid extensive item cloning this class manages and returns references to the original items directly and is
 * therefore not suited for long term storage.
 */
public class TradingRecipeDraft {

	protected final ItemStack resultItem;
	protected final ItemStack item1;
	protected final ItemStack item2;

	/**
	 * Creates a TradingRecipe.
	 * <p>
	 * If <code>item1</code> is empty, item2 will take its place.
	 * 
	 * @param resultItem
	 *            the result item
	 * @param item1
	 *            the first buy item
	 * @param item2
	 *            the second buy item
	 */
	public TradingRecipeDraft(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		this.resultItem = ItemUtils.getNullIfEmpty(resultItem);
		item1 = ItemUtils.getNullIfEmpty(item1);
		item2 = ItemUtils.getNullIfEmpty(item2);
		// swap items if item1 is empty:
		if (item1 == null) {
			item1 = item2;
			item2 = null;
		}
		this.item1 = item1;
		this.item2 = item2;
	}

	/**
	 * Gets the result item.
	 * 
	 * @return the result item, can be <code>null</code>
	 */
	public ItemStack getResultItem() {
		return resultItem;
	}

	/**
	 * Gets the first required item.
	 * 
	 * @return the first required item, can be <code>null</code>
	 */
	public ItemStack getItem1() {
		return item1;
	}

	/**
	 * Gets the second required item.
	 * 
	 * @return the second required item, can be <code>null</code>
	 */
	public ItemStack getItem2() {
		return item2;
	}

	/**
	 * Checks if this draft represents a valid trading recipe.
	 *
	 * @return <code>true</code> if valid
	 */
	public boolean isValid() {
		return resultItem != null && item1 != null;
	}

	/**
	 * Creates a {@link TradingRecipe} based on this draft.
	 * 
	 * @param outOfStock
	 *            <code>true</code> if out of stock
	 * @return the trading recipe, or <code>null</code> if this draft does not represent a valid recipe
	 */
	public TradingRecipe toRecipe(boolean outOfStock) {
		if (resultItem == null) return null;
		if (item1 == null) return null;
		return ShopkeepersAPI.createTradingRecipe(resultItem, item1, item2, outOfStock);
	}

	public boolean areItemsEqual(TradingRecipeDraft otherRecipe) {
		if (otherRecipe == null) return false;
		if (!Objects.equal(resultItem, otherRecipe.resultItem)) return false;
		if (!Objects.equal(item1, otherRecipe.item1)) return false;
		if (!Objects.equal(item2, otherRecipe.item2)) return false;
		return true;
	}

	public boolean areItemsEqual(TradingRecipe otherRecipe) {
		// this is true for TradingRecipes based on SKTradingRecipe
		if (!(otherRecipe instanceof TradingRecipeDraft)) return false; // also checks for null
		return this.areItemsEqual((TradingRecipeDraft) otherRecipe);
	}
}
