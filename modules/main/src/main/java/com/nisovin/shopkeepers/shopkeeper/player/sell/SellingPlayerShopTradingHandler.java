package com.nisovin.shopkeepers.shopkeeper.player.sell;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.ui.trading.TradingContext;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;

public class SellingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	/**
	 * The offer corresponding to the currently processed trade.
	 */
	private @Nullable PriceOffer currentOffer = null;

	protected SellingPlayerShopTradingHandler(SKSellingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKSellingPlayerShopkeeper getShopkeeper() {
		return (SKSellingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;

		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		// Get offer for this type of item:
		UnmodifiableItemStack soldItem = tradingRecipe.getResultItem();
		PriceOffer offer = shopkeeper.getOffer(soldItem);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(
					tradingPlayer,
					"Could not find the offer corresponding to the trading recipe!"
			);
			return false;
		}

		// Validate the found offer:
		int expectedSoldItemAmount = offer.getItem().getAmount();
		if (expectedSoldItemAmount != soldItem.getAmount()) {
			// Unexpected, because the recipe was created based on this offer.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(
					tradingPlayer,
					"The offer does not match the trading recipe!"
			);
			return false;
		}

		this.currentOffer = offer;

		return true;
	}

	@Override
	protected boolean finalTradePreparation(Trade trade) {
		if (!super.finalTradePreparation(trade)) return false;

		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();
		PriceOffer offer = Unsafe.assertNonNull(this.currentOffer);
		@Nullable ItemStack[] newContainerContents = Unsafe.assertNonNull(this.newContainerContents);

		// Remove the result items from the container contents:
		// Note: We always use the configured result item here, ignoring any modifications to the
		// "result" item during the trade event. The trading player will still receive the modified
		// result item.
		UnmodifiableItemStack soldItem = tradingRecipe.getResultItem();
		if (InventoryUtils.removeItems(newContainerContents, soldItem) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStock);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container does not contain the required items."
			);
			return false;
		}

		// Add the earnings to the container contents:
		// Note: We always use the configured currency items here, ignoring any modifications to the
		// "received" items during the subsequent trade event.
		int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
		if (this.addCurrencyItems(newContainerContents, amountAfterTaxes) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container cannot hold the traded items."
			);
			return false;
		}

		return true;
	}

	@Override
	protected void onTradeOver(TradingContext tradingContext) {
		super.onTradeOver(tradingContext);

		// Reset trade related state:
		this.currentOffer = null;
	}
}
