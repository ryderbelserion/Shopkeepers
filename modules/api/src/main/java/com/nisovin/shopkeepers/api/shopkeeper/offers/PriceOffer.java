package com.nisovin.shopkeepers.api.shopkeeper.offers;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.internal.ApiInternals;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * Stores information about an item stack being sold or bought for a certain price.
 * <p>
 * Instances of this are immutable. They can be created via {@link #create(ItemStack, int)}.
 */
public interface PriceOffer {

	/**
	 * Creates a new {@link PriceOffer}.
	 * <p>
	 * The given item stack is copied before it is stored by the price offer.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 */
	public static PriceOffer create(ItemStack item, int price) {
		return ApiInternals.getInstance().createPriceOffer(item, price);
	}

	/**
	 * Creates a new {@link PriceOffer}.
	 * <p>
	 * The given item stack is assumed to be immutable and therefore not copied before it is stored
	 * by the price offer.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 */
	public static PriceOffer create(UnmodifiableItemStack item, int price) {
		return ApiInternals.getInstance().createPriceOffer(item, price);
	}

	// ----

	/**
	 * Gets the traded item.
	 * 
	 * @return an unmodifiable view on the traded item, not <code>null</code> or empty
	 */
	public UnmodifiableItemStack getItem();

	/**
	 * Gets the price.
	 * 
	 * @return the price, a positive number
	 */
	public int getPrice();
}
