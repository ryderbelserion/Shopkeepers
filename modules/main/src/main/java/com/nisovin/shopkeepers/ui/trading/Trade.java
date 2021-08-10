package com.nisovin.shopkeepers.ui.trading;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Information about a trade that is currently being processed.
 */
public final class Trade {
	/**
	 * The inventory click event which originally triggered the trade.
	 * <p>
	 * Do not modify this event or any of the involved items! This has to be kept cancelled!
	 */
	public final InventoryClickEvent clickEvent;
	/**
	 * The involved merchant inventory.
	 */
	public final MerchantInventory merchantInventory;
	/**
	 * The trading player.
	 */
	public final Player tradingPlayer;
	/**
	 * The involved player inventory.
	 */
	public final PlayerInventory playerInventory;
	/**
	 * The used trading recipe.
	 */
	public final TradingRecipe tradingRecipe;
	/**
	 * The item offered by the player matching the first required item of the used trading recipe (not necessarily the
	 * item in the first slot), not <code>null</code> or empty.
	 * <p>
	 * The type equals that of the required item from the trading recipe. The metadata however can differ, but still be
	 * accepted for the trade depending on the item matching rules of the used Minecraft version and the shopkeeper
	 * settings (ex. strict item comparison disabled).
	 * <p>
	 * This item stack is not a copy and might be modified once the trade is applied! The stack size matches the
	 * original stack size of the item used by the player.
	 */
	public final ItemStack offeredItem1;
	/**
	 * The item offered by the player matching the second required item of the used trading recipe (not necessarily the
	 * item in the second slot), can be <code>null</code>.
	 * <p>
	 * The type equals that of the required item from the trading recipe. The metadata however can differ, but still be
	 * accepted for the trade depending on the item matching rules of the used Minecraft version and the shopkeeper
	 * settings (ex. strict item comparison disabled).
	 * <p>
	 * This item stack is not a copy and might be modified once the trade is applied! The stack size matches the
	 * original stack size of the item used by the player.
	 */
	public final ItemStack offeredItem2;
	/**
	 * Whether the <code>offeredItem1</code> and <code>offeredItem2</code> are placed in reverse or regular order inside
	 * the trading slots of the merchant inventory.
	 */
	public final boolean swappedItemOrder;
	/**
	 * Arbitrary additional information related to this trade.
	 */
	private Map<String, Object> customData = null;

	Trade(	InventoryClickEvent clickEvent, MerchantInventory merchantInventory, Player tradingPlayer,
			TradingRecipe tradingRecipe, ItemStack offeredItem1, ItemStack offeredItem2, boolean swappedItemOrder) {
		assert clickEvent != null && merchantInventory != null && tradingPlayer != null && tradingRecipe != null && offeredItem1 != null;
		this.clickEvent = clickEvent;
		this.merchantInventory = merchantInventory;
		this.tradingPlayer = tradingPlayer;
		this.playerInventory = tradingPlayer.getInventory();
		this.tradingRecipe = tradingRecipe;
		this.offeredItem1 = offeredItem1;
		this.offeredItem2 = offeredItem2;
		this.swappedItemOrder = swappedItemOrder;
	}

	/**
	 * Gets the value for the specified key.
	 * <p>
	 * In combination with {@link #set(String, Object)} this allows the {@link Trade} to store additional arbitrary
	 * data.
	 * 
	 * @param <T>
	 *            the expected type of the value
	 * @param key
	 *            the key, not <code>null</code>
	 * @return the value, or <code>null</code> if there is no value for the specified key
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		Validate.notNull(key, "key");
		if (customData == null) return null;
		return (T) customData.get(key);
	}

	/**
	 * Stores the given value under the specified key.
	 * <p>
	 * In combination with {@link #get(String)} this allows the {@link Trade} to store additional arbitrary data.
	 * 
	 * @param <T>
	 *            the type of the value
	 * @param key
	 *            the key, not <code>null</code>
	 * @param value
	 *            the value, or <code>null</code> to clear any previous value for the given key
	 */
	public <T> void set(String key, T value) {
		Validate.notNull(key, "key");
		if (value == null) {
			// Clear the value for the given key:
			if (customData != null) {
				customData.remove(key);
			}
		} else {
			if (customData == null) {
				customData = new HashMap<>();
			}
			customData.put(key, value);
		}
	}
}
