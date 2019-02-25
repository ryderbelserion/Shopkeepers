package com.nisovin.shopkeepers.shopkeeper.player.sell;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class SellingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected SellingPlayerShopTradingHandler(SellingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SellingPlayerShopkeeper getShopkeeper() {
		return (SellingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		SellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		// get offer for this type of item:
		ItemStack soldItem = tradingRecipe.getResultItem();
		PriceOffer offer = shopkeeper.getOffer(soldItem);
		if (offer == null) {
			// this should not happen.. because the recipes were created based on the shopkeeper's offers
			this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
			return false;
		}

		// validate the found offer:
		int expectedSoldItemAmount = offer.getItem().getAmount();
		if (expectedSoldItemAmount != soldItem.getAmount()) {
			// this shouldn't happen .. because the recipe was created based on this offer
			this.debugPreventedTrade(tradingPlayer, "The offer doesn't match the trading recipe!");
			return false;
		}

		assert chestInventory != null & newChestContents != null;

		// remove result items from chest contents:
		if (ItemUtils.removeItems(newChestContents, soldItem) != 0) {
			this.debugPreventedTrade(tradingPlayer, "The shop's chest doesn't contain the required items.");
			return false;
		}

		// add earnings to chest contents:
		// TODO maybe add the actual items the trading player gave, instead of creating new currency items?
		int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
		if (amountAfterTaxes > 0) {
			// TODO always store the currency in the most compressed form possible, regardless of
			// 'highCurrencyMinCost'?
			int remaining = amountAfterTaxes;
			if (Settings.isHighCurrencyEnabled() || remaining > Settings.highCurrencyMinCost) {
				int highCurrencyAmount = (remaining / Settings.highCurrencyValue);
				if (highCurrencyAmount > 0) {
					int remainingHighCurrency = ItemUtils.addItems(newChestContents, Settings.createHighCurrencyItem(highCurrencyAmount));
					remaining -= ((highCurrencyAmount - remainingHighCurrency) * Settings.highCurrencyValue);
				}
			}
			if (remaining > 0) {
				if (ItemUtils.addItems(newChestContents, Settings.createCurrencyItem(remaining)) != 0) {
					this.debugPreventedTrade(tradingPlayer, "The shop's chest cannot hold the traded items.");
					return false;
				}
			}
		}
		return true;
	}
}