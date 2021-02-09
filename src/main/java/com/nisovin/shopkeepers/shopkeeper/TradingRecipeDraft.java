package com.nisovin.shopkeepers.shopkeeper;

import java.util.Objects;

import org.bukkit.inventory.ItemStack;

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

	public static final TradingRecipeDraft EMPTY = new TradingRecipeDraft(null, null, null);

	protected final ItemStack resultItem;
	protected final ItemStack item1;
	protected final ItemStack item2;

	/**
	 * Creates a TradingRecipe.
	 * <p>
	 * If <code>item1</code> is empty, <code>item2</code> will take its place.
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
		// Swap items if item1 is empty:
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
	 * Checks if all the items of this recipe are empty.
	 * 
	 * @return <code>true</code> if empty
	 */
	public boolean isEmpty() {
		return resultItem == null && item1 == null && item2 == null;
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

	public boolean areItemsEqual(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		if (!Objects.equals(this.resultItem, resultItem)) return false;
		if (!Objects.equals(this.item1, item1)) return false;
		if (!Objects.equals(this.item2, item2)) return false;
		return true;
	}

	public boolean areItemsEqual(TradingRecipeDraft otherRecipe) {
		if (otherRecipe == null) return false;
		return this.areItemsEqual(otherRecipe.resultItem, otherRecipe.item1, otherRecipe.item2);
	}

	public boolean areItemsEqual(TradingRecipe otherRecipe) {
		if (otherRecipe == null) return false;
		if (otherRecipe instanceof TradingRecipeDraft) {
			// This is true for TradingRecipes based on SKTradingRecipe:
			return this.areItemsEqual((TradingRecipeDraft) otherRecipe); // Avoids copying the items
		} else {
			return this.areItemsEqual(otherRecipe.getResultItem(), otherRecipe.getItem1(), otherRecipe.getItem2());
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TradingRecipeDraft [resultItem=");
		builder.append(resultItem);
		builder.append(", item1=");
		builder.append(item1);
		builder.append(", item2=");
		builder.append(item2);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resultItem == null) ? 0 : resultItem.hashCode());
		result = prime * result + ((item1 == null) ? 0 : item1.hashCode());
		result = prime * result + ((item2 == null) ? 0 : item2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof TradingRecipeDraft)) return false;
		TradingRecipeDraft other = (TradingRecipeDraft) obj;
		if (!Objects.equals(resultItem, other.resultItem)) return false;
		if (!Objects.equals(item1, other.item1)) return false;
		if (!Objects.equals(item2, other.item2)) return false;
		return true;
	}
}
