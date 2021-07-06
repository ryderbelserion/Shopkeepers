package com.nisovin.shopkeepers.shopkeeper;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

// Shares its implementation with TradingRecipeDraft.
public class SKTradingRecipe extends TradingRecipeDraft implements TradingRecipe {

	private final boolean outOfStock;

	/**
	 * Creates a {@link SKTradingRecipe}.
	 * <p>
	 * The recipe is not out of stock.
	 * <p>
	 * If the given item stacks are {@link UnmodifiableItemStack}s, they are assumed to be immutable and therefore not
	 * copied before they are stored by the trading recipe. Otherwise, they are first copied.
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
	 * <p>
	 * If the given item stacks are {@link UnmodifiableItemStack}s, they are assumed to be immutable and therefore not
	 * copied before they are stored by the trading recipe. Otherwise, they are first copied.
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
		this(
				ItemUtils.unmodifiableCloneIfModifiable(resultItem),
				ItemUtils.unmodifiableCloneIfModifiable(item1),
				ItemUtils.unmodifiableCloneIfModifiable(item2),
				outOfStock
		);
	}

	/**
	 * Creates a {@link SKTradingRecipe}.
	 * <p>
	 * The recipe is not out of stock.
	 * <p>
	 * The given item stacks are assumed to be immutable and therefore not copied before they are stored by the trading
	 * recipe.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 */
	public SKTradingRecipe(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2) {
		this(resultItem, item1, item2, false);
	}

	/**
	 * Creates a {@link SKTradingRecipe}.
	 * <p>
	 * The given item stacks are assumed to be immutable and therefore not copied before they are stored by the trading
	 * recipe.
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
	public SKTradingRecipe(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2, boolean outOfStock) {
		super(resultItem, item1, item2);
		Validate.isTrue(!ItemUtils.isEmpty(resultItem), "resultItem is empty");
		Validate.isTrue(!ItemUtils.isEmpty(item1), "item1 is empty");
		this.outOfStock = outOfStock;
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
