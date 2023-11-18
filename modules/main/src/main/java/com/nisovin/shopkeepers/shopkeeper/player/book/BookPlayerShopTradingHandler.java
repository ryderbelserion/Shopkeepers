package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.function.Predicate;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.ui.trading.TradingContext;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.BookItems;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class BookPlayerShopTradingHandler extends PlayerShopTradingHandler {

	private static final Predicate<@ReadOnly @Nullable ItemStack> WRITABLE_BOOK_MATCHER
			= ItemUtils.itemsOfType(Material.WRITABLE_BOOK);

	/**
	 * The offer corresponding to the currently processed trade.
	 */
	private @Nullable BookOffer currentOffer = null;

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
			this.debugPreventedTrade(
					tradingPlayer,
					"The traded item is no valid book copy!"
			);
			return false;
		}

		String bookTitle = BookItems.getTitle(bookMeta);
		if (bookTitle == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(
					tradingPlayer,
					"Could not determine the book title of the traded item!"
			);
			return false;
		}

		// Get the offer for this type of item:
		BookOffer offer = shopkeeper.getOffer(bookTitle);
		if (offer == null) {
			// Unexpected, but this might happen if the trades got modified while the player was
			// trading:
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(
					tradingPlayer,
					"Could not find the offer corresponding to the trading recipe!"
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
		BookOffer offer = Unsafe.assertNonNull(this.currentOffer);
		@Nullable ItemStack[] newContainerContents = Unsafe.assertNonNull(this.newContainerContents);

		// Remove a blank book from the container contents:
		if (InventoryUtils.removeItems(newContainerContents, WRITABLE_BOOK_MATCHER, 1) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientWritableBooks);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container does not contain any writable (book-and-quill) items."
			);
			return false;
		}

		// Add the earnings to the container contents:
		// Note: We always use the configured currency items here, ignoring any modifications to the
		// "received" items during the trade event.
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
