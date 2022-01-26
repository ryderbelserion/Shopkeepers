package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.BookItems;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class BookPlayerShopTradingHandler extends PlayerShopTradingHandler {

	private static final Predicate<ItemStack> WRITABLE_BOOK_MATCHER = ItemUtils.itemsOfType(Material.WRITABLE_BOOK);

	protected BookPlayerShopTradingHandler(SKBookPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKBookPlayerShopkeeper getShopkeeper() {
		return (SKBookPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;
		SKBookPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		UnmodifiableItemStack bookItem = tradingRecipe.getResultItem();
		BookMeta bookMeta = BookItems.getBookMeta(bookItem);
		if (bookMeta == null || !BookItems.isCopy(bookMeta)) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "The traded item is no valid book copy!");
			return false;
		}

		String bookTitle = BookItems.getTitle(bookMeta);
		if (bookTitle == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "Could not determine the book title of the traded item!");
			return false;
		}

		// Get the offer for this type of item:
		BookOffer offer = shopkeeper.getOffer(bookTitle);
		if (offer == null) {
			// Unexpected, but this might happen if the trades got modified while the player was trading:
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(tradingPlayer, "Could not find the offer corresponding to the trading recipe!");
			return false;
		}

		assert containerInventory != null && newContainerContents != null;

		// Remove a blank book from the container contents:
		if (InventoryUtils.removeItems(newContainerContents, WRITABLE_BOOK_MATCHER, 1) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientWritableBooks);
			this.debugPreventedTrade(tradingPlayer, "The shop's container does not contain any writable (book-and-quill) items.");
			return false;
		}

		// Add earnings to container contents:
		int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
		if (amountAfterTaxes > 0) {
			int remaining = amountAfterTaxes;
			if (Currencies.isHighCurrencyEnabled() && remaining > Settings.highCurrencyMinCost) {
				Currency highCurrency = Currencies.getHigh();
				int highCurrencyAmount = (remaining / highCurrency.getValue());
				if (highCurrencyAmount > 0) {
					ItemStack currencyItems = Currencies.getHigh().getItemData().createItemStack(highCurrencyAmount);
					int remainingHighCurrency = InventoryUtils.addItems(newContainerContents, currencyItems);
					remaining -= ((highCurrencyAmount - remainingHighCurrency) * highCurrency.getValue());
				}
			}
			if (remaining > 0) {
				ItemStack currencyItems = Currencies.getBase().getItemData().createItemStack(remaining);
				if (InventoryUtils.addItems(newContainerContents, currencyItems) != 0) {
					TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
					this.debugPreventedTrade(tradingPlayer, "The shop's container cannot hold the traded items.");
					return false;
				}
			}
		}
		return true;
	}
}
