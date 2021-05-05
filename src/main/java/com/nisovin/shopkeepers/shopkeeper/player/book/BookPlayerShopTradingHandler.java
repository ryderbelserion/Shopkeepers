package com.nisovin.shopkeepers.shopkeeper.player.book;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.BookItems;
import com.nisovin.shopkeepers.util.ItemUtils;

public class BookPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected BookPlayerShopTradingHandler(SKBookPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKBookPlayerShopkeeper getShopkeeper() {
		return (SKBookPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		SKBookPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		ItemStack bookItem = tradingRecipe.getResultItem();
		BookMeta bookMeta = BookItems.getBookMeta(bookItem);
		if (bookMeta == null || !BookItems.isCopy(bookMeta)) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			this.debugPreventedTrade(tradingPlayer, "The traded item is no valid book copy!");
			return false;
		}

		String bookTitle = BookItems.getTitle(bookMeta);
		if (bookTitle == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			this.debugPreventedTrade(tradingPlayer, "Could not determine the book title of the traded item!");
			return false;
		}

		// Get the offer for this type of item:
		BookOffer offer = shopkeeper.getOffer(bookTitle);
		if (offer == null) {
			// Unexpected, but this might happen if the trades got modified while the player was trading:
			this.debugPreventedTrade(tradingPlayer, "Could not find the offer corresponding to the trading recipe!");
			return false;
		}

		assert containerInventory != null & newContainerContents != null;

		// Remove blank book from container contents:
		boolean removed = false;
		for (int slot = 0; slot < newContainerContents.length; slot++) {
			ItemStack itemStack = newContainerContents[slot];
			if (ItemUtils.isEmpty(itemStack)) continue;
			if (itemStack.getType() != Material.WRITABLE_BOOK) continue;

			int newAmount = itemStack.getAmount() - 1;
			assert newAmount >= 0;
			if (newAmount == 0) {
				newContainerContents[slot] = null;
			} else {
				// Copy the item before modifying it:
				itemStack = itemStack.clone();
				newContainerContents[slot] = itemStack;
				itemStack.setAmount(newAmount);
			}
			removed = true;
			break;
		}
		if (!removed) {
			this.debugPreventedTrade(tradingPlayer, "The shop's container does not contain any writable (book-and-quill) items.");
			return false;
		}

		// Add earnings to container contents:
		int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
		if (amountAfterTaxes > 0) {
			int remaining = amountAfterTaxes;
			if (Settings.isHighCurrencyEnabled() && remaining > Settings.highCurrencyMinCost) {
				int highCurrencyAmount = (remaining / Settings.highCurrencyValue);
				if (highCurrencyAmount > 0) {
					int remainingHighCurrency = ItemUtils.addItems(newContainerContents, Settings.createHighCurrencyItem(highCurrencyAmount));
					remaining -= ((highCurrencyAmount - remainingHighCurrency) * Settings.highCurrencyValue);
				}
			}
			if (remaining > 0) {
				if (ItemUtils.addItems(newContainerContents, Settings.createCurrencyItem(remaining)) != 0) {
					this.debugPreventedTrade(tradingPlayer, "The shop's container cannot hold the traded items.");
					return false;
				}
			}
		}
		return true;
	}
}
