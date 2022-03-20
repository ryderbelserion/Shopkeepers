package com.nisovin.shopkeepers.util.trading;

import java.time.Instant;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Represents a number of consecutive trades that involved the same player, the same shopkeeper, and
 * the same traded items.
 */
public class MergedTrades {

	private final ShopkeeperTradeEvent initialTrade;
	private final Instant timestamp = Instant.now();
	private int tradeCount = 1;

	/**
	 * Creates a new {@link MergedTrades} for the given {@link ShopkeeperTradeEvent trade} with a
	 * {@link #getTradeCount() trade count} of one.
	 * 
	 * @param initialTrade
	 *            the initial trade, not <code>null</code>
	 */
	public MergedTrades(ShopkeeperTradeEvent initialTrade) {
		Validate.notNull(initialTrade, "initialTrade is null");
		this.initialTrade = initialTrade;
	}

	/**
	 * Gets the {@link ShopkeeperTradeEvent} of the initial trade.
	 * 
	 * @return the initial trade, not <code>null</code>
	 */
	public ShopkeeperTradeEvent getInitialTrade() {
		return initialTrade;
	}

	/**
	 * Gets the timestamp of the {@link #getInitialTrade() initial trade}.
	 * 
	 * @return the timestamp of the initial trade, not <code>null</code>
	 */
	public Instant getTimestamp() {
		return timestamp;
	}

	/**
	 * Gets the result item of the trades. See {@link ShopkeeperTradeEvent#getTradingRecipe()} and
	 * {@link TradingRecipe#getResultItem()}.
	 * 
	 * @return an unmodifiable view on the result item, not <code>null</code> or empty
	 */
	public UnmodifiableItemStack getResultItem() {
		return initialTrade.getTradingRecipe().getResultItem();
	}

	/**
	 * Gets the first offered item of the trades. See
	 * {@link ShopkeeperTradeEvent#getOfferedItem1()}.
	 * 
	 * @return an unmodifiable view on the first offered item, not <code>null</code> or empty
	 */
	public UnmodifiableItemStack getOfferedItem1() {
		return initialTrade.getOfferedItem1();
	}

	/**
	 * Gets the second offered item of the trades. See
	 * {@link ShopkeeperTradeEvent#getOfferedItem2()}.
	 * 
	 * @return an unmodifiable view on the second offered item, can be <code>null</code>
	 */
	public @Nullable UnmodifiableItemStack getOfferedItem2() {
		return initialTrade.getOfferedItem2();
	}

	/**
	 * Gets the number of equivalent trades that are represented by this {@link MergedTrades}.
	 * 
	 * @return the number of trades
	 */
	public int getTradeCount() {
		return tradeCount;
	}

	/**
	 * Adds the specified number of trades to this {@link MergedTrades}.
	 * 
	 * @param tradesToAdd
	 *            the number of trades to add
	 */
	public void addTrades(int tradesToAdd) {
		Validate.isTrue(tradesToAdd > 0, "tradesToAdd has to be positive");
		tradeCount += tradesToAdd;
	}

	/**
	 * Checks if the given {@link MergedTrades} can be merged with this {@link MergedTrades}.
	 * <p>
	 * I.e. this checks if the trades involve the same player, shopkeeper, and items.
	 * 
	 * @param otherTrades
	 *            the other trades, not <code>null</code>
	 * @return <code>true</code> if the trades can be merged
	 */
	public boolean canMerge(MergedTrades otherTrades) {
		Validate.notNull(otherTrades, "otherTrades is null");
		ShopkeeperTradeEvent otherInitialTrade = otherTrades.getInitialTrade();
		if (initialTrade.getClickEvent() != otherInitialTrade.getClickEvent()) {
			if (initialTrade.getPlayer() != otherInitialTrade.getPlayer()) return false;
			if (initialTrade.getShopkeeper() != otherInitialTrade.getShopkeeper()) return false;

			// Note: We do not compare the trading recipes here, because the items offered by the
			// player might be different to those of the trading recipe, and therefore also among
			// trades that use the same trading recipe.
			// Items are compared with equals instead of isSimilar to also take stack sizes into
			// account:
			if (!Objects.equals(this.getResultItem(), otherTrades.getResultItem())) return false;
			if (!Objects.equals(this.getOfferedItem1(), otherTrades.getOfferedItem1())) return false;
			if (!Objects.equals(this.getOfferedItem2(), otherTrades.getOfferedItem2())) return false;
		} else {
			// We assume that the player, shopkeeper, and the involved items (offered items and the
			// result item) remain the same throughout the same click event (this avoids costly item
			// comparisons). However, if the selected trading recipe changed throughout the trades
			// caused by the same click event, the item stack sizes of the involved trading recipes
			// (i.e. of the offered items and the result items) might have changed. We consider
			// these to be a separate kinds of trades then.
			int resultItemAmount = this.getResultItem().getAmount();
			int otherResultItemAmount = otherTrades.getResultItem().getAmount();
			if (resultItemAmount != otherResultItemAmount) return false;

			int offeredItem1Amount = this.getOfferedItem1().getAmount();
			int otherOfferedItem1Amount = otherTrades.getOfferedItem1().getAmount();
			if (offeredItem1Amount != otherOfferedItem1Amount) return false;

			int offeredItem2Amount = ItemUtils.getItemStackAmount(this.getOfferedItem2());
			int otherOfferedItem2Amount = ItemUtils.getItemStackAmount(otherTrades.getOfferedItem2());
			if (offeredItem2Amount != otherOfferedItem2Amount) return false;
		}
		return true;
	}
}
