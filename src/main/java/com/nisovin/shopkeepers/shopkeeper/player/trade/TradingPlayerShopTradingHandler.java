package com.nisovin.shopkeepers.shopkeeper.player.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected TradingPlayerShopTradingHandler(SKTradingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKTradingPlayerShopkeeper getShopkeeper() {
		return (SKTradingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		SKTradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		// Find offer:
		TradeOffer offer = shopkeeper.getOffer(tradingRecipe);
		if (offer == null) {
			// This might happen if the trades got modified while the player was trading:
			this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
			return false;
		}

		assert containerInventory != null & newContainerContents != null;

		// Remove result items from container contents:
		ItemStack resultItem = tradingRecipe.getResultItem();
		assert resultItem != null;
		if (ItemUtils.removeItems(newContainerContents, resultItem) != 0) {
			this.debugPreventedTrade(tradingPlayer, "The shop's container does not contain the required items.");
			return false;
		}

		// Add traded items to container contents:
		if (!this.addItems(newContainerContents, tradingRecipe.getItem1(), tradeData.offeredItem1)
				|| !this.addItems(newContainerContents, tradingRecipe.getItem2(), tradeData.offeredItem2)) {
			this.debugPreventedTrade(tradingPlayer, "The shop's container cannot hold the traded items.");
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
			ItemStack receivedItem = offeredItem.clone(); // Create a copy, just in case
			receivedItem.setAmount(amountAfterTaxes);
			if (ItemUtils.addItems(contents, receivedItem) != 0) {
				// Couldn't add all items to the contents:
				return false;
			}
		}
		return true;
	}
}
