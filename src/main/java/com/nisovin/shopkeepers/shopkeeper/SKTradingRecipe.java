package com.nisovin.shopkeepers.shopkeeper;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.util.ItemUtils;

public class SKTradingRecipe implements TradingRecipe {

	private final ItemStack resultItem; // not empty
	private final ItemStack item1; // not empty
	private final ItemStack item2; // can be null
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
		Validate.isTrue(!ItemUtils.isEmpty(resultItem), "Result item cannot be empty!");
		Validate.isTrue(!ItemUtils.isEmpty(item1), "Item1 cannot be empty!");
		this.resultItem = resultItem.clone();
		this.item1 = item1.clone();
		this.item2 = ItemUtils.isEmpty(item2) ? null : item2.clone();
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
