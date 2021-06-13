package com.nisovin.shopkeepers.shopkeeper;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;

// Shares its implementation with TradingRecipeDraft, but returns copies of its items.
public class SKTradingRecipe extends TradingRecipeDraft implements TradingRecipe {

	private final boolean outOfStock;

	/**
	 * Creates a {@link SKTradingRecipe}.
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
	public SKTradingRecipe(@ReadOnly ItemStack resultItem, @ReadOnly ItemStack item1, @ReadOnly ItemStack item2) {
		this(resultItem, item1, item2, false);
	}

	/**
	 * Creates a {@link SKTradingRecipe}.
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
	public SKTradingRecipe(@ReadOnly ItemStack resultItem, @ReadOnly ItemStack item1, @ReadOnly ItemStack item2, boolean outOfStock) {
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

	/**
	 * Gets the result item without making a copy of it first.
	 * <p>
	 * For internal use only. The item is expected to not be modified.
	 * 
	 * @return the result item, not <code>null</code> or empty
	 */
	public ItemStack getInternalResultItem() {
		return super.getResultItem();
	}

	/**
	 * Gets the first required item without making a copy of it first.
	 * <p>
	 * For internal use only. The item is expected to not be modified.
	 * 
	 * @return the first required item, not <code>null</code> or empty
	 */
	public ItemStack getInternalItem1() {
		return super.getItem1();
	}

	/**
	 * Gets the second required item without making a copy of it first.
	 * <p>
	 * For internal use only. The item is expected to not be modified.
	 * 
	 * @return the second required item, can be <code>null</code>
	 */
	public ItemStack getInternalItem2() {
		return super.getItem2();
	}

	@Override
	public final boolean isOutOfStock() {
		return outOfStock;
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
