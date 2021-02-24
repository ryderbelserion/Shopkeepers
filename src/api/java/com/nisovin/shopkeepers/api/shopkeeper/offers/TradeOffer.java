package com.nisovin.shopkeepers.api.shopkeeper.offers;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;

/**
 * Stores information about up to two items being traded for another item.
 * <p>
 * Instances of this can be created via {@link ShopkeepersAPI#createTradeOffer(ItemStack, ItemStack, ItemStack)}.
 */
public interface TradeOffer {

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
	 * Checks if the given items are equal to the items of this trade offer.
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
	 * Checks if the items of this offer and the given trading recipe are equal.
	 * 
	 * @param tradingRecipe
	 *            the trading recipe to compare with
	 * @return <code>true</code> if the items are equal
	 */
	public boolean areItemsEqual(TradingRecipe tradingRecipe);
}
