package com.nisovin.shopkeepers.shopkeeper;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.util.ItemUtils;

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
		return (item2 == null ? null : item2.clone());
	}

	@Override
	public final boolean isOutOfStock() {
		return outOfStock;
	}
}
