package com.nisovin.shopkeepers.api.util;

import org.apache.commons.lang.Validate;
import org.bukkit.inventory.ItemStack;

/**
 * An unmodifiable trading recipe.
 */
public class TradingRecipe {

	private final ItemStack resultItem; // not null/empty
	private final ItemStack item1; // not null/empty
	private final ItemStack item2; // can be null

	public TradingRecipe(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		Validate.isTrue(!ItemUtils.isEmpty(resultItem), "Result item cannot be empty!");
		Validate.isTrue(!ItemUtils.isEmpty(item1), "Item1 cannot be empty!");
		this.resultItem = resultItem.clone();
		this.item1 = item1.clone();
		this.item2 = ItemUtils.isEmpty(item2) ? null : item2.clone();
	}

	/**
	 * Gets the result item.
	 * 
	 * @return the result item (cloned), not <code>null</code> or empty
	 */
	public final ItemStack getResultItem() {
		return resultItem.clone();
	}

	/**
	 * Gets the first required item.
	 * 
	 * @return the first required item (cloned), not <code>null</code> or empty
	 */
	public final ItemStack getItem1() {
		return item1.clone();
	}

	/**
	 * Gets the second required item.
	 * 
	 * @return the second required item (cloned), can be <code>null</code>
	 */
	public final ItemStack getItem2() {
		return (item2 == null ? null : item2.clone());
	}
}
