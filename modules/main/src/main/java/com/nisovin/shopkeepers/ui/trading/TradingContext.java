package com.nisovin.shopkeepers.ui.trading;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.util.java.KeyValueStore;
import com.nisovin.shopkeepers.util.java.MapBasedKeyValueStore;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Information about an inventory interaction and its processing that might trigger zero, one, or
 * multiple trades.
 */
public final class TradingContext {

	private final Shopkeeper shopkeeper;
	private final InventoryClickEvent inventoryClickEvent;
	private final MerchantInventory merchantInventory;
	private final Player tradingPlayer;
	private final PlayerInventory playerInventory;
	private final KeyValueStore metadata = new MapBasedKeyValueStore();

	private int tradeCount = 0;
	private @Nullable Trade currentTrade = null;

	/**
	 * Creates a new {@link TradingContext}.
	 * 
	 * @param shopkeeper
	 *            the involved {@link Shopkeeper}, not <code>null</code>
	 * @param inventoryClickEvent
	 *            the involved {@link InventoryClickEvent}, not <code>null</code>
	 */
	TradingContext(Shopkeeper shopkeeper, InventoryClickEvent inventoryClickEvent) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.notNull(inventoryClickEvent, "inventoryClickEvent is null");
		assert inventoryClickEvent.getView().getTopInventory() instanceof MerchantInventory;
		assert inventoryClickEvent.getWhoClicked() instanceof Player;
		this.shopkeeper = shopkeeper;
		this.inventoryClickEvent = inventoryClickEvent;
		this.merchantInventory = (MerchantInventory) inventoryClickEvent.getView().getTopInventory();
		this.tradingPlayer = (Player) inventoryClickEvent.getWhoClicked();
		this.playerInventory = tradingPlayer.getInventory();
	}

	/**
	 * Gets the involved {@link Shopkeeper}.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	public Shopkeeper getShopkeeper() {
		return shopkeeper;
	}

	/**
	 * Gets the involved {@link InventoryClickEvent}.
	 * <p>
	 * Do not modify this event or any of the involved items! The event has to be kept cancelled!
	 * 
	 * @return the inventory click event, not <code>null</code>
	 */
	public InventoryClickEvent getInventoryClickEvent() {
		return inventoryClickEvent;
	}

	/**
	 * Gets the involved {@link MerchantInventory}.
	 * 
	 * @return the merchant inventory, not <code>null</code>
	 */
	public MerchantInventory getMerchantInventory() {
		return merchantInventory;
	}

	/**
	 * Gets the trading {@link Player}.
	 * 
	 * @return the trading player, not <code>null</code>
	 */
	public Player getTradingPlayer() {
		return tradingPlayer;
	}

	/**
	 * Gets the {@link PlayerInventory} of the trading player.
	 * 
	 * @return the player inventory, not <code>null</code>
	 */
	public PlayerInventory getPlayerInventory() {
		return playerInventory;
	}

	/**
	 * Gets the {@link KeyValueStore} that stores additional arbitrary metadata related to this
	 * {@link TradingContext}.
	 * 
	 * @return the metadata key-value store, not <code>null</code>
	 */
	public KeyValueStore getMetadata() {
		return metadata;
	}

	/**
	 * Gets the number of trades that were triggered in this {@link TradingContext} so far.
	 * <p>
	 * This returns {@code 0} if no trades have been processed yet, and {@code 1} if only one trade
	 * has been or is currently being processed.
	 * <p>
	 * The processing of the last trade attempt might not be complete yet and the trade might still
	 * get aborted. Whether or not the currently processed trade attempt has been aborted is not
	 * reflected by the returned number.
	 * 
	 * @return the number of trades processed so far
	 */
	public int getTradeCount() {
		return tradeCount;
	}

	/**
	 * Gets the last {@link Trade} that was or is currently being processed.
	 * <p>
	 * The current trade is reset to <code>null</code> whenever the processing of a new trade
	 * attempt starts.
	 * 
	 * @return the current trade, or <code>null</code> if no trade has been processed yet or if no
	 *         {@link Trade} instance has been created yet for the current trade attempt
	 */
	public @Nullable Trade getCurrentTrade() {
		return currentTrade;
	}

	/**
	 * Starts the processing of a new trade.
	 * <p>
	 * This increments the {@link #getTradeCount() trade count} and resets the
	 * {@link #getCurrentTrade() current trade} to <code>null</code>.
	 */
	void startNewTrade() {
		tradeCount += 1;
		currentTrade = null;
	}

	/**
	 * Sets the {@link Trade} that is currently being processed.
	 * 
	 * @param trade
	 *            the trade, not <code>null</code>
	 */
	void setCurrentTrade(Trade trade) {
		Validate.notNull(trade, "trade is null");
		this.currentTrade = trade;
	}
}
