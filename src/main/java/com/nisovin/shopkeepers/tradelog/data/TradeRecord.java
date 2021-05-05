package com.nisovin.shopkeepers.tradelog.data;

import java.time.Instant;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.util.Validate;

/**
 * An immutable snapshot of the information about a trade that happened.
 */
public class TradeRecord {

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
		// These ItemStacks are clones:
		ItemStack resultItem = tradeEvent.getTradingRecipe().getResultItem();
		ItemStack item1 = tradeEvent.getOfferedItem1();
		ItemStack item2 = tradeEvent.getOfferedItem2();
		return new TradeRecord(timestamp, playerRecord, shopRecord, resultItem, item1, item2);
	}

	private final Instant timestamp; // Not null
	private final PlayerRecord player; // Not null
	private final ShopRecord shop; // Not null
	private final ItemStack resultItem; // Not null
	// The items provided by the player that match the first and second items required by the trade. These items might
	// not necessarily be equal the items required by the trade (they only have to 'match' / be accepted). Their amounts
	// match those of the trading recipe.
	// The order in which the player provided the items in the trading interface is not recorded.
	private final ItemStack item1; // Not null
	private final ItemStack item2; // Can be null

	private TradeRecord(Instant timestamp, PlayerRecord player, ShopRecord shop, ItemStack resultItem, ItemStack item1, ItemStack item2) {
		Validate.notNull(timestamp, "timestamp is null");
		Validate.notNull(player, "player is null");
		Validate.notNull(shop, "shop is null");
		Validate.notNull(resultItem, "resultItem is null");
		Validate.notNull(item1, "item1 is null");
		this.timestamp = timestamp;
		this.player = player;
		this.shop = shop;
		this.resultItem = resultItem;
		this.item1 = item1;
		this.item2 = item2;
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
	 * In order to avoid excessive item copying, this returns the item stored by this record without copying it first.
	 * However, it is only meant for read-only purposes. Do not modify it!
	 * 
	 * @return the result item, not <code>null</code>
	 */
	public ItemStack getResultItem() {
		return resultItem;
	}

	/**
	 * Gets the first item provided by the player.
	 * <p>
	 * Due to Minecraft's fuzzy item matching rules, this may not necessarily perfectly match the corresponding item of
	 * the used trading recipe.
	 * <p>
	 * In order to avoid excessive item copying, this returns the item stored by this record without copying it first.
	 * However, it is only meant for read-only purposes. Do not modify it!
	 * 
	 * @return the first item involved in the trade, not <code>null</code>
	 */
	public ItemStack getItem1() {
		return item1;
	}

	/**
	 * Gets the second item provided by the player.
	 * <p>
	 * Due to Minecraft's fuzzy item matching rules, this may not necessarily perfectly match the corresponding item of
	 * the used trading recipe.
	 * <p>
	 * In order to avoid excessive item copying, this returns the item stored by this record without copying it first.
	 * However, it is only meant for read-only purposes. Do not modify it!
	 * 
	 * @return the second item involved in the trade, can be <code>null</code>
	 */
	public ItemStack getItem2() {
		return item2;
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
		result = prime * result + ((item2 == null) ? 0 : item2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof TradeRecord)) return false;
		TradeRecord other = (TradeRecord) obj;
		if (!timestamp.equals(other.timestamp)) return false;
		if (!player.equals(other.player)) return false;
		if (!shop.equals(other.shop)) return false;
		if (!resultItem.equals(other.resultItem)) return false;
		if (!item1.equals(other.item1)) return false;
		if (item2 == null) {
			if (other.item2 != null) return false;
		} else if (!item2.equals(other.item2)) return false;
		return true;
	}
}
