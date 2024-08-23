package com.nisovin.shopkeepers.api.shopkeeper.player.buy;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * Buys items in exchange for currency items.
 * <p>
 * There exists at most one offer for a certain type of item.
 */
public interface BuyingPlayerShopkeeper extends PlayerShopkeeper {

	// OFFERS

	/**
	 * Gets the offers of this shopkeeper.
	 * <p>
	 * Contains at most one offer for a certain type of item.
	 * 
	 * @return an unmodifiable view on the shopkeeper's offers
	 */
	public List<? extends PriceOffer> getOffers();

	/**
	 * Gets the offer for the given type of item.
	 * 
	 * @param tradedItem
	 *            the item being bought, not <code>null</code>
	 * @return the offer, or <code>null</code> if there is none for the given item
	 */
	public @Nullable PriceOffer getOffer(ItemStack tradedItem);

	/**
	 * Gets the offer for the given type of item.
	 * 
	 * @param tradedItem
	 *            the item being bought, not <code>null</code>
	 * @return the offer, or <code>null</code> if there is none for the given item
	 */
	public @Nullable PriceOffer getOffer(UnmodifiableItemStack tradedItem);

	/**
	 * Removes the offer for the given type of item.
	 * <p>
	 * This has no effect if no such offer exists.
	 * 
	 * @param tradedItem
	 *            the item being bought, not <code>null</code>
	 */
	public void removeOffer(ItemStack tradedItem);

	/**
	 * Removes the offer for the given type of item.
	 * <p>
	 * This has no effect if no such offer exists.
	 * 
	 * @param tradedItem
	 *            the item being bought, not <code>null</code>
	 */
	public void removeOffer(UnmodifiableItemStack tradedItem);

	/**
	 * Clears the shopkeeper's offers.
	 */
	public void clearOffers();

	/**
	 * Sets the shopkeeper's offers.
	 * <p>
	 * This replaces the shopkeeper's previous offers. Duplicate offers for the same item will
	 * replace each other.
	 * 
	 * @param offers
	 *            the new offers
	 */
	public void setOffers(List<? extends PriceOffer> offers);

	/**
	 * Adds the given offer to the shopkeeper.
	 * <p>
	 * This will replace any previous offer for the same item.
	 * <p>
	 * The offer gets added to the end of the current offers. If you want to insert, replace or
	 * reorder offers, use {@link #setOffers(List)} instead.
	 * 
	 * @param offer
	 *            the offer to add
	 */
	public void addOffer(PriceOffer offer);

	/**
	 * Adds the given offers to the shopkeeper.
	 * <p>
	 * For every offer, this will replace any previous offer for the same item. Duplicate offers for
	 * the same item will replace each other.
	 * <p>
	 * The offers get added to the end of the current offers. If you want to insert, replace or
	 * reorder offers, use {@link #setOffers(List)} instead.
	 * 
	 * @param offers
	 *            the offers to add
	 */
	public void addOffers(List<? extends PriceOffer> offers);
}
