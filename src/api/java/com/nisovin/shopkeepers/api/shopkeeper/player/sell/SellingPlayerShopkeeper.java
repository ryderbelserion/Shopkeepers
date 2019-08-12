package com.nisovin.shopkeepers.api.shopkeeper.player.sell;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;

/**
 * Sells items in exchange for currency items.
 * <p>
 * There exists at most one offer for a certain type of item.
 */
public interface SellingPlayerShopkeeper extends PlayerShopkeeper {

	// OFFERS

	/**
	 * Gets the offers of this shopkeeper.
	 * <p>
	 * Contains at most one offer for a certain type of item.
	 * 
	 * @return an unmodifiable view on the shopkeeper's offers
	 */
	public List<PriceOffer> getOffers();

	/**
	 * Gets the offer for a specific type of item.
	 * 
	 * @param tradedItem
	 *            the item being sold
	 * @return the offer, or <code>null</code> if there is none for the specified item
	 */
	public PriceOffer getOffer(ItemStack tradedItem);

	/**
	 * Removes the offer for a specific type of item.
	 * <p>
	 * This has no effect if no such offer exists.
	 * 
	 * @param tradedItem
	 *            the item being sold
	 */
	public void removeOffer(ItemStack tradedItem);

	/**
	 * Clears the shopkeeper's offers.
	 */
	public void clearOffers();

	/**
	 * Sets the shopkeeper's offers.
	 * <p>
	 * This replaces the shopkeeper's previous offers. Duplicate offers for the same item will replace each other.
	 * 
	 * @param offers
	 *            the new offers
	 */
	public void setOffers(List<PriceOffer> offers);

	/**
	 * Adds the given offer to the shopkeeper.
	 * <p>
	 * This will replace any previous offer for the same item.
	 * <p>
	 * The offer gets added to the end of the current offers. If you want to insert, replace or reorder offers, use
	 * {@link #setOffers(List)} instead.
	 * 
	 * @param offer
	 *            the offer to add
	 */
	public void addOffer(PriceOffer offer);

	/**
	 * Adds the given offers to the shopkeeper.
	 * <p>
	 * For every offer, this will replace any previous offer for the same item. Duplicate offers for the same item will
	 * replace each other.
	 * <p>
	 * The offers get added to the end of the current offers. If you want to insert, replace or reorder offers, use
	 * {@link #setOffers(List)} instead.
	 * 
	 * @param offers
	 *            the offers to add
	 */
	public void addOffers(List<PriceOffer> offers);
}
