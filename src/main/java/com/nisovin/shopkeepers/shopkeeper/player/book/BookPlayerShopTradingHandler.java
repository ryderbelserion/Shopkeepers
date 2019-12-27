package com.nisovin.shopkeepers.shopkeeper.player.book;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
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
		if (!SKBookPlayerShopkeeper.isValidBookCopy(bookItem)) {
			this.debugPreventedTrade(tradingPlayer, "The traded item is no valid book copy!");
			return false;
		}

		String bookTitle = SKBookPlayerShopkeeper.getBookTitle(bookItem);
		if (bookTitle == null) {
			// this should not happen.. because the recipes were created based on the shopkeeper's offers
			this.debugPreventedTrade(tradingPlayer, "Couldn't determine the book title of the traded item!");
			return false;
		}

		// get offer for this type of item:
		BookOffer offer = shopkeeper.getOffer(bookTitle);
		if (offer == null) {
			// this might happen if the trades got modified while the player was trading:
			this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
			return false;
		}

		assert chestInventory != null & newChestContents != null;

		// remove blank book from chest contents:
		boolean removed = false;
		for (int slot = 0; slot < newChestContents.length; slot++) {
			ItemStack itemStack = newChestContents[slot];
			if (ItemUtils.isEmpty(itemStack)) continue;
			if (itemStack.getType() != Material.WRITABLE_BOOK) continue;

			int newAmount = itemStack.getAmount() - 1;
			assert newAmount >= 0;
			if (newAmount == 0) {
				newChestContents[slot] = null;
			} else {
				// copy the item before modifying it:
				itemStack = itemStack.clone();
				newChestContents[slot] = itemStack;
				itemStack.setAmount(newAmount);
			}
			removed = true;
			break;
		}
		if (!removed) {
			this.debugPreventedTrade(tradingPlayer, "The shop's chest doesn't contain any book-and-quill items.");
			return false;
		}

		// add earnings to chest contents:
		int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
		if (amountAfterTaxes > 0) {
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
