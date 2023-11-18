package com.nisovin.shopkeepers.tradelog.data;

import java.time.Instant;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.trading.MergedTrades;

/**
 * An immutable snapshot of the information about one or multiple equal trades that took place.
 * <p>
 * In order to represent trades more compactly, a single {@link TradeRecord} may represent a number
 * of consecutive trades that involve the same player, the same shopkeeper, and the same traded
 * items.
 */
public class TradeRecord {

	/**
	 * Creates a {@link TradeRecord} for the given {@link MergedTrades}.
	 * 
	 * @param trades
	 *            the merged trades
	 * @return the trade record
	 */
	public static TradeRecord create(MergedTrades trades) {
		Validate.notNull(trades, "trades is null");
		Instant timestamp = trades.getTimestamp();
		ShopkeeperTradeEvent tradeEvent = trades.getInitialTrade();
		PlayerRecord playerRecord = PlayerRecord.of(tradeEvent.getPlayer());
		ShopRecord shopRecord = ShopRecord.of(tradeEvent.getShopkeeper());
		// We reuse the immutable items of the given MergedTrades:
		UnmodifiableItemStack resultItem = trades.getResultItem();
		UnmodifiableItemStack item1 = trades.getOfferedItem1();
		UnmodifiableItemStack item2 = trades.getOfferedItem2();
		int tradeCount = trades.getTradeCount();
		return new TradeRecord(
				timestamp,
				playerRecord,
				shopRecord,
				resultItem,
				item1,
				item2,
				tradeCount
		);
	}

	/**
	 * Creates a {@link TradeRecord} for the given {@link ShopkeeperTradeEvent}.
	 * 
	 * @param tradeEvent
	 *            the trade event
	 * @return the trade record
	 */
	public static TradeRecord create(ShopkeeperTradeEvent tradeEvent) {
		Validate.notNull(tradeEvent, "tradeEvent is null");
		Instant timestamp = Instant.now();
		PlayerRecord playerRecord = PlayerRecord.of(tradeEvent.getPlayer());
		ShopRecord shopRecord = ShopRecord.of(tradeEvent.getShopkeeper());
		// These items are immutable:
		UnmodifiableItemStack resultItem = tradeEvent.getTradingRecipe().getResultItem();
		UnmodifiableItemStack item1 = tradeEvent.getOfferedItem1();
		UnmodifiableItemStack item2 = tradeEvent.getOfferedItem2();
		return new TradeRecord(timestamp, playerRecord, shopRecord, resultItem, item1, item2, 1);
	}

	private final Instant timestamp; // Not null
	private final PlayerRecord player; // Not null
	private final ShopRecord shop; // Not null
	// The items provided by the player that match the first and second items required by the trade.
	// These items might not necessarily be equal the items required by the trade (they only have to
	// 'match' / be accepted). Their amounts match those of the trading recipe.
	// The order in which the player provided the items in the trading interface is not recorded.
	// Note: We only record the trade's original trading recipe items currently. If any plugins
	// modified the received or result items, those changes are not captured by the trade log.
	private final UnmodifiableItemStack resultItem; // Not null
	private final UnmodifiableItemStack item1; // Not null
	private final @Nullable UnmodifiableItemStack item2; // Can be null
	private final int tradeCount; // > 0

	private TradeRecord(
			Instant timestamp,
			PlayerRecord player,
			ShopRecord shop,
			UnmodifiableItemStack resultItem,
			UnmodifiableItemStack item1,
			@Nullable UnmodifiableItemStack item2,
			int tradeCount
	) {
		Validate.notNull(timestamp, "timestamp is null");
		Validate.notNull(player, "player is null");
		Validate.notNull(shop, "shop is null");
		Validate.notNull(resultItem, "resultItem is null");
		Validate.notNull(item1, "item1 is null");
		Validate.isTrue(tradeCount > 0, "tradeCount has to be positive");
		this.timestamp = timestamp;
		this.player = player;
		this.shop = shop;
		this.resultItem = resultItem;
		this.item1 = item1;
		this.item2 = item2;
		this.tradeCount = tradeCount;
	}

	/**
	 * Gets the timestamp of the trade.
	 * 
	 * @return the timestamp
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Gets the trading player.
	 * 
	 * @return the trading player
	 */
	public PlayerRecord getPlayer() {
		return player;
	}

	/**
	 * Gets the involved shop.
	 * 
	 * @return the shop
	 */
	public ShopRecord getShop() {
		return shop;
	}

	/**
	 * Gets the result item.
	 * <p>
	 * In order to avoid excessive item copying, this returns the item stored by this record without
	 * copying it first. However, it is only meant for read-only purposes. Do not modify it!
	 * 
	 * @return an unmodifiable view on the result item, not <code>null</code>
	 */
	public UnmodifiableItemStack getResultItem() {
		return resultItem;
	}

	/**
	 * Gets the first item provided by the player.
	 * <p>
	 * Due to Minecraft's fuzzy item matching rules, this may not necessarily perfectly match the
	 * corresponding item of the used trading recipe.
	 * 
	 * @return an unmodifiable view on the first item involved in the trade, not <code>null</code>
	 */
	public UnmodifiableItemStack getItem1() {
		return item1;
	}

	/**
	 * Gets the second item provided by the player.
	 * <p>
	 * Due to Minecraft's fuzzy item matching rules, this may not necessarily perfectly match the
	 * corresponding item of the used trading recipe.
	 * 
	 * @return an unmodifiable view on the second item involved in the trade, can be
	 *         <code>null</code>
	 */
	public @Nullable UnmodifiableItemStack getItem2() {
		return item2;
	}

	/**
	 * Gets the number of equivalent trades that are represented by this {@link TradeRecord}.
	 * 
	 * @return the number of trades
	 */
	public int getTradeCount() {
		return tradeCount;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TradeRecord [timestamp=");
		builder.append(timestamp);
		builder.append(", player=");
		builder.append(player);
		builder.append(", shop=");
		builder.append(shop);
		builder.append(", resultItem=");
		builder.append(resultItem);
		builder.append(", item1=");
		builder.append(item1);
		builder.append(", item2=");
		builder.append(item2);
		builder.append(", tradeCount=");
		builder.append(tradeCount);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + timestamp.hashCode();
		result = prime * result + player.hashCode();
		result = prime * result + shop.hashCode();
		result = prime * result + resultItem.hashCode();
		result = prime * result + item1.hashCode();
		result = prime * result + Objects.hashCode(item2);
		result = prime * result + tradeCount;
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof TradeRecord)) return false;
		TradeRecord other = (TradeRecord) obj;
		if (!timestamp.equals(other.timestamp)) return false;
		if (tradeCount != other.tradeCount) return false;
		if (!player.equals(other.player)) return false;
		if (!shop.equals(other.shop)) return false;
		if (!resultItem.equals(other.resultItem)) return false;
		if (!item1.equals(other.item1)) return false;
		if (!Objects.equals(item2, other.item2)) return false;
		return true;
	}
}
