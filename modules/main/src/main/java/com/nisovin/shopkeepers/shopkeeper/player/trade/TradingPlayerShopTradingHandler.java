package com.nisovin.shopkeepers.shopkeeper.player.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;

public class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected TradingPlayerShopTradingHandler(SKTradingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKTradingPlayerShopkeeper getShopkeeper() {
		return (SKTradingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;

		SKTradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		// Find offer:
		TradeOffer offer = shopkeeper.getOffer(tradingRecipe);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(
					tradingPlayer,
					"Could not find the offer corresponding to the trading recipe!"
			);
			return false;
		}

		return true;
	}

	@Override
	protected boolean finalTradePreparation(Trade trade) {
		if (!super.finalTradePreparation(trade)) return false;

		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();
		@Nullable ItemStack[] newContainerContents = Unsafe.assertNonNull(this.newContainerContents);

		// Remove the result items from the container contents:
		// Note: We always use the configured result item here, ignoring any modifications to the
		// "result" item during the trade event. The trading player will still receive the modified
		// result item.
		UnmodifiableItemStack resultItem = tradingRecipe.getResultItem();
		assert resultItem != null;
		if (InventoryUtils.removeItems(newContainerContents, resultItem) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStock);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container does not contain the required items."
			);
			return false;
		}

		// Add the received items to the container contents, taking modifications during the trade
		// event into account:
		// Note: Even if the received items were not altered by any plugins, depending on the used
		// item comparison logic and settings, the items that the trading player offered might
		// slightly differ the required items, but still be accepted.
		ShopkeeperTradeEvent tradeEvent = Unsafe.assertNonNull(trade.getTradeEvent());
		UnmodifiableItemStack receivedItem1 = tradeEvent.getReceivedItem1();
		UnmodifiableItemStack receivedItem2 = tradeEvent.getReceivedItem2();

		if (this.addReceivedItem(newContainerContents, receivedItem1) != 0
				|| this.addReceivedItem(newContainerContents, receivedItem2) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container cannot hold the received items."
			);
			return false;
		}

		return true;
	}
}
