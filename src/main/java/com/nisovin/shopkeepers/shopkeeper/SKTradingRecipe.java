package com.nisovin.shopkeepers.shopkeeper;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SKTradingRecipe extends TradingRecipeDraft implements TradingRecipe {

	private final boolean outOfStock;

	/**
	 * Creates a trading recipe.
	 * <p>
	 * The recipe is not out of stock.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 */
	public SKTradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		this(resultItem, item1, item2, false);
	}

	/**
	 * Creates a TradingRecipe.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @param outOfStock
	 *            <code>true</code> if the recipe is out of stock
	 */
	public SKTradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2, boolean outOfStock) {
		super(ItemUtils.cloneOrNullIfEmpty(resultItem), ItemUtils.cloneOrNullIfEmpty(item1), ItemUtils.cloneOrNullIfEmpty(item2));
		Validate.isTrue(!ItemUtils.isEmpty(resultItem), "Result item cannot be empty!");
		Validate.isTrue(!ItemUtils.isEmpty(item1), "Item1 cannot be empty!");
		this.outOfStock = outOfStock;
	}

	@Override
	public final ItemStack getResultItem() {
		return resultItem.clone();
	}

	@Override
	public final ItemStack getItem1() {
		return item1.clone();
	}

	@Override
	public final ItemStack getItem2() {
		return (item2 == null) ? null : item2.clone();
	}

	@Override
	public final boolean isOutOfStock() {
		return outOfStock;
	}

	@Override
	public boolean areItemsEqual(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		return super.areItemsEqual(resultItem, item1, item2);
	}

	@Override
	public boolean areItemsEqual(TradingRecipe otherRecipe) {
		return super.areItemsEqual(otherRecipe);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SKTradingRecipe [resultItem=");
		builder.append(resultItem);
		builder.append(", item1=");
		builder.append(item1);
		builder.append(", item2=");
		builder.append(item2);
		builder.append(", outOfStock=");
		builder.append(outOfStock);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (outOfStock ? 1231 : 1237);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!super.equals(obj)) return false;
		if (!(obj instanceof SKTradingRecipe)) return false;
		SKTradingRecipe other = (SKTradingRecipe) obj;
		if (outOfStock != other.outOfStock) return false;
		return true;
	}
}
