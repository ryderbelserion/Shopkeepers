package com.nisovin.shopkeepers.api.shopkeeper.offers;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;

/**
 * Stores information about an item stack being sold or bought for a certain price.
 * <p>
 * Instances of this can be created via {@link ShopkeepersAPI#createPriceOffer(ItemStack, int)}.
 */
public interface PriceOffer {

	/**
	 * Gets the traded item.
	 * 
	 * @return a copy of the traded item, not <code>null</code> or empty
	 */
	public ItemStack getItem();

	/**
	 * Gets the price.
	 * 
	 * @return the price, a positive number
	 */
	public int getPrice();
}
