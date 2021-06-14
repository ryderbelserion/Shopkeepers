package com.nisovin.shopkeepers.api;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopTypesRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectTypesRegistry;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

public final class ShopkeepersAPI {

	private ShopkeepersAPI() {
	}

	private static ShopkeepersPlugin plugin = null;

	public static void enable(ShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "Plugin is null!");
		if (ShopkeepersAPI.plugin != null) {
			throw new IllegalStateException("API is already enabled!");
		}
		ShopkeepersAPI.plugin = plugin;
	}

	public static void disable() {
		if (ShopkeepersAPI.plugin == null) {
			throw new IllegalStateException("API is already disabled!");
		}
		ShopkeepersAPI.plugin = null;
	}

	/**
	 * Checks whether the API has already been enabled.
	 * <p>
	 * If this is called early during plugin startup (eg. during the {@link Plugin#onLoad() loading phase} of plugins,
	 * or while the Shopkeepers plugin itself is still getting enabled), the API may not yet be safe for use even if
	 * this returns <code>true</code>.
	 * 
	 * @return <code>true</code> if enabled
	 */
	public static boolean isEnabled() {
		return (plugin != null);
	}

	public static ShopkeepersPlugin getPlugin() {
		if (plugin == null) {
			throw new IllegalStateException("API is not enabled!");
		}
		return plugin;
	}

	// PERMISSIONS

	/**
	 * Checks if the given player has the permission to create any shopkeeper.
	 * 
	 * @param player
	 *            the player
	 * @return <code>false</code> if he cannot create shops at all, <code>true</code> otherwise
	 */
	public static boolean hasCreatePermission(Player player) {
		return getPlugin().hasCreatePermission(player);
	}

	// SHOP TYPES

	public static ShopTypesRegistry<?> getShopTypeRegistry() {
		return getPlugin().getShopTypeRegistry();
	}

	public static DefaultShopTypes getDefaultShopTypes() {
		return getPlugin().getDefaultShopTypes();
	}

	// SHOP OBJECT TYPES

	public static ShopObjectTypesRegistry<?> getShopObjectTypeRegistry() {
		return getPlugin().getShopObjectTypeRegistry();
	}

	public static DefaultShopObjectTypes getDefaultShopObjectTypes() {
		return getPlugin().getDefaultShopObjectTypes();
	}
	// UI

	public static UIRegistry<?> getUIRegistry() {
		return getPlugin().getUIRegistry();
	}

	public static DefaultUITypes getDefaultUITypes() {
		return getPlugin().getDefaultUITypes();
	}

	// SHOPKEEPER REGISTRY

	public static ShopkeeperRegistry getShopkeeperRegistry() {
		return getPlugin().getShopkeeperRegistry();
	}

	// STORAGE

	/**
	 * Gets the {@link ShopkeeperStorage}.
	 * 
	 * @return the shopkeeper storage
	 */
	public static ShopkeeperStorage getShopkeeperStorage() {
		return getPlugin().getShopkeeperStorage();
	}

	//

	/**
	 * Creates and spawns a new shopkeeper in the same way a player would create it.
	 * <p>
	 * This takes any limitations into account that might affect the creator or owner of the shopkeeper, and this sends
	 * the creator messages if the shopkeeper creation fails for some reason.
	 * 
	 * @param shopCreationData
	 *            the shop creation data containing the necessary arguments (spawn location, object type, etc.) for
	 *            creating this shopkeeper
	 * @return the new shopkeeper, or <code>null</code> if the creation wasn't successful for some reason
	 */
	public static Shopkeeper handleShopkeeperCreation(ShopCreationData shopCreationData) {
		return getPlugin().handleShopkeeperCreation(shopCreationData);
	}

	// FACTORIES

	/**
	 * Creates an {@link UnmodifiableItemStack} for the given {@link ItemStack}.
	 * <p>
	 * If the given item stack is already an {@link UnmodifiableItemStack}, this returns the given item stack itself.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the unmodifiable item stack, or <code>null</code> if the given item stack is <code>null</code>
	 */
	public static UnmodifiableItemStack createUnmodifiableItemStack(ItemStack itemStack) {
		return getPlugin().createUnmodifiableItemStack(itemStack);
	}

	// OFFER FACTORIES

	/**
	 * Creates a new {@link PriceOffer}.
	 * <p>
	 * If the given item stack is an {@link UnmodifiableItemStack}, it is assumed to be immutable and therefore not
	 * copied before it is stored by the price offer. Otherwise, it is first copied.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 */
	public static PriceOffer createPriceOffer(ItemStack item, int price) {
		return getPlugin().createPriceOffer(item, price);
	}

	/**
	 * Creates a new {@link PriceOffer}.
	 * <p>
	 * The given item stack is assumed to be immutable and therefore not copied before it is stored by the price offer.
	 * 
	 * @param item
	 *            the item being traded, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 */
	public static PriceOffer createPriceOffer(UnmodifiableItemStack item, int price) {
		return getPlugin().createPriceOffer(item, price);
	}

	/**
	 * Creates a new {@link TradeOffer}.
	 * <p>
	 * If the given item stacks are {@link UnmodifiableItemStack}s, they are assumed to be immutable and therefore not
	 * copied before they are stored by the trade offer. Otherwise, they are first copied.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @return the new offer
	 */
	public static TradeOffer createTradeOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		return getPlugin().createTradeOffer(resultItem, item1, item2);
	}

	/**
	 * Creates a new {@link TradeOffer}.
	 * <p>
	 * The given item stacks are assumed to be immutable and therefore not copied before they are stored by the trade
	 * offer.
	 * 
	 * @param resultItem
	 *            the result item, not empty
	 * @param item1
	 *            the first buy item, not empty
	 * @param item2
	 *            the second buy item, can be empty
	 * @return the new offer
	 */
	public static TradeOffer createTradeOffer(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2) {
		return getPlugin().createTradeOffer(resultItem, item1, item2);
	}

	/**
	 * Creates a new {@link BookOffer}.
	 * 
	 * @param bookTitle
	 *            the book title, not <code>null</code> or empty
	 * @param price
	 *            the price, has to be positive
	 * @return the new offer
	 */
	public static BookOffer createBookOffer(String bookTitle, int price) {
		return getPlugin().createBookOffer(bookTitle, price);
	}
}
