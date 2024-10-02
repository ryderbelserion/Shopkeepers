package com.nisovin.shopkeepers.api.internal;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperSnapshot;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * Internal (non-API) components and functionality that needs to be accessible from within the API.
 * <p>
 * Only for internal use by the Shopkeepers API!
 */
public interface ApiInternals {

	/**
	 * Gets the {@link ApiInternals}.
	 * 
	 * @return the internals, not <code>null</code>
	 * @throws IllegalStateException
	 *             if the API is not enabled currently, e.g. because the plugin is not enabled
	 *             currently
	 */
	public static ApiInternals getInstance() {
		return InternalShopkeepersAPI.getPlugin().getApiInternals();
	}

	// FACTORIES

	/**
	 * Creates an {@link UnmodifiableItemStack} for the given {@link ItemStack}.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the unmodifiable item stack, or <code>null</code> if the given item stack is
	 *         <code>null</code>
	 * @see UnmodifiableItemStack#of(ItemStack)
	 */
	public @PolyNull UnmodifiableItemStack createUnmodifiableItemStack(
			@PolyNull ItemStack itemStack
	);

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
	 * @see PriceOffer#create(ItemStack, int)
	 */
	public PriceOffer createPriceOffer(ItemStack item, int price);

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
	 * @see PriceOffer#create(UnmodifiableItemStack, int)
	 */
	public PriceOffer createPriceOffer(UnmodifiableItemStack item, int price);

	/**
	 * Creates a new {@link TradeOffer}.
	 * <p>
	 * The given item stacks are copied before they are stored by the trade offer.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @return the new offer
	 * @see TradeOffer#create(ItemStack, ItemStack, ItemStack)
	 */
	public TradeOffer createTradeOffer(
			ItemStack resultItem,
			ItemStack item1,
			@Nullable ItemStack item2
	);

	/**
	 * Creates a new {@link TradeOffer}.
	 * <p>
	 * The given item stacks are assumed to be immutable and therefore not copied before they are
	 * stored by the trade offer.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @return the new offer
	 * @see TradeOffer#create(UnmodifiableItemStack, UnmodifiableItemStack, UnmodifiableItemStack)
	 */
	public TradeOffer createTradeOffer(
			UnmodifiableItemStack resultItem,
			UnmodifiableItemStack item1,
			@Nullable UnmodifiableItemStack item2
	);

	/**
	 * Creates a new {@link BookOffer}.
	 * 
	 * @param bookTitle
	 *            the book title, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 * @see BookOffer#create(String, int)
	 */
	public BookOffer createBookOffer(String bookTitle, int price);

	// SHOPKEEPER SNAPSHOTS

	/**
	 * Gets the maximum length of {@link ShopkeeperSnapshot} names.
	 * 
	 * @return the maximum snapshot name length
	 * @see ShopkeeperSnapshot#getMaxNameLength()
	 */
	public int getShopkeeperSnapshotMaxNameLength();

	/**
	 * Checks if the given {@link ShopkeeperSnapshot} name is valid.
	 * 
	 * @param name
	 *            the name
	 * @return <code>true</code> if the name is valid
	 * @see ShopkeeperSnapshot#isNameValid(String)
	 */
	public boolean isShopkeeperSnapshotNameValid(String name);

	// UTILITIES

	/**
	 * Checks if the given {@link UnmodifiableItemStack} is empty.
	 * <p>
	 * The item stack is considered 'empty' if it is <code>null</code>, is of type
	 * {@link Material#AIR}, or its amount is less than or equal to zero.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return <code>true</code> if the item stack is empty
	 */
	public boolean isEmpty(@Nullable UnmodifiableItemStack itemStack);
}
