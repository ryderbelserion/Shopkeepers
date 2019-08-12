package com.nisovin.shopkeepers.api.shopkeeper.player.book;

import java.util.List;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;

/**
 * Sells copies of written books in exchange for currency items.
 * <p>
 * Books are identified solely based on their title. There exists at most one offer for a certain book. If there are
 * multiple books with the same title in the chest, the shopkeeper uses only the first book it finds.
 */
public interface BookPlayerShopkeeper extends PlayerShopkeeper {

	// OFFERS

	/**
	 * Gets the offers of this shopkeeper.
	 * <p>
	 * Contains at most one offer for a certain book.
	 * 
	 * @return an unmodifiable view on the shopkeeper's offers
	 */
	public List<BookOffer> getOffers();

	/**
	 * Gets the offer for a specific book item.
	 * 
	 * @param bookItem
	 *            the book item
	 * @return the offer, or <code>null</code> if there is none for the specified item
	 */
	public BookOffer getOffer(ItemStack bookItem);

	/**
	 * Gets the offer for a specific book title.
	 * 
	 * @param bookTitle
	 *            the book title
	 * @return the offer, or <code>null</code> if there is none for the specified book title
	 */
	public BookOffer getOffer(String bookTitle);

	/**
	 * Removes the offer for a specific book title.
	 * <p>
	 * This has no effect if no such offer exists.
	 * 
	 * @param bookTitle
	 *            the book title
	 */
	public void removeOffer(String bookTitle);

	/**
	 * Clears the shopkeeper's offers.
	 */
	public void clearOffers();

	/**
	 * Sets the shopkeeper's offers.
	 * <p>
	 * This replaces the shopkeeper's previous offers. Duplicate offers for the same book will replace each other.
	 * 
	 * @param offers
	 *            the new offers
	 */
	public void setOffers(List<BookOffer> offers);

	/**
	 * Adds the given offer to the shopkeeper.
	 * <p>
	 * This will replace any previous offer for the same book.
	 * <p>
	 * The offer gets added to the end of the current offers. If you want to insert, replace or reorder offers, use
	 * {@link #setOffers(List)} instead.
	 * 
	 * @param offer
	 *            the offer to add
	 */
	public void addOffer(BookOffer offer);

	/**
	 * Adds the given offers to the shopkeeper.
	 * <p>
	 * For every offer, this will replace any previous offer for the same book. Duplicate offers for the same book will
	 * replace each other.
	 * <p>
	 * The offers get added to the end of the current offers. If you want to insert, replace or reorder offers, use
	 * {@link #setOffers(List)} instead.
	 * 
	 * @param offers
	 *            the offers to add
	 */
	public void addOffers(List<BookOffer> offers);
}
