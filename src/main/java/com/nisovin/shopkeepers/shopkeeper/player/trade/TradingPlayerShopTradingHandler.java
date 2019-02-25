package com.nisovin.shopkeepers.shopkeeper.player.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected TradingPlayerShopTradingHandler(TradingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public TradingPlayerShopkeeper getShopkeeper() {
		return (TradingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		TradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		// find offer:
		TradingOffer offer = shopkeeper.getOffer(tradingRecipe);
		if (offer == null) {
			// this should not happen.. because the recipes were created based on the shopkeeper's offers
			this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
			return false;
		}

		assert chestInventory != null & newChestContents != null;

		// remove result items from chest contents:
		ItemStack resultItem = tradingRecipe.getResultItem();
		assert resultItem != null;
		if (ItemUtils.removeItems(newChestContents, resultItem) != 0) {
			this.debugPreventedTrade(tradingPlayer, "The shop's chest doesn't contain the required items.");
			return false;
		}

		// add traded items to chest contents:
		if (!this.addItems(newChestContents, tradingRecipe.getItem1(), tradeData.offeredItem1)
				|| !this.addItems(newChestContents, tradingRecipe.getItem2(), tradeData.offeredItem2)) {
			this.debugPreventedTrade(tradingPlayer, "The shop's chest cannot hold the traded items.");
			return false;
		}
		return true;
	}

	// The items the trading player gave might slightly differ from the required items,
	// but are still accepted for the trade, depending on minecraft's item comparison and settings.
	// Therefore we differ between require and offered items here.
	// Returns false, if not all items could be added to the contents:
	private boolean addItems(ItemStack[] contents, ItemStack requiredItem, ItemStack offeredItem) {
		if (ItemUtils.isEmpty(requiredItem)) return true;
		int amountAfterTaxes = this.getAmountAfterTaxes(requiredItem.getAmount());
		if (amountAfterTaxes > 0) {
			ItemStack receivedItem = offeredItem.clone(); // create a copy, just in case
			receivedItem.setAmount(amountAfterTaxes);
			if (ItemUtils.addItems(contents, receivedItem) != 0) {
				// couldn't add all items to the contents:
				return false;
			}
		}
		return true;
	}
}
