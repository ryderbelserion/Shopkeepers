package com.nisovin.shopkeepers.shopkeeper.player.buy;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.ui.trading.TradingContext;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class BuyingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	/**
	 * The offer corresponding to the currently processed trade.
	 */
	private @Nullable PriceOffer currentOffer = null;

	protected BuyingPlayerShopTradingHandler(SKBuyingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKBuyingPlayerShopkeeper getShopkeeper() {
		return (SKBuyingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;

		SKBuyingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		// Get offer for the bought item:
		UnmodifiableItemStack boughtItem = tradingRecipe.getItem1();
		PriceOffer offer = shopkeeper.getOffer(boughtItem);
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
		int expectedBoughtItemAmount = offer.getItem().getAmount();
		if (expectedBoughtItemAmount != boughtItem.getAmount()) {
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
		PriceOffer offer = Unsafe.assertNonNull(this.currentOffer);
		@Nullable ItemStack[] newContainerContents = Unsafe.assertNonNull(this.newContainerContents);

		// Remove the currency items from the container contents:
		// Note: We always use the configured currency items here, ignoring any modifications to the
		// "result" item during the trade event.
		int remaining = this.removeCurrency(newContainerContents, offer.getPrice());
		if (remaining > 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientCurrency);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container does not contain enough currency."
			);
			return false;
		} else if (remaining < 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container does not have enough space to split large currency items."
			);
			return false;
		}

		// Add the bought items to the container contents, taking modifications to the trade event's
		// "received" items into account:
		// Note: Even if the received items were not altered by any plugins, depending on the used
		// item comparison logic and settings, the items that the trading player offered might
		// slightly differ the required items, but still be accepted.
		// Note: Event handlers might set a second "received" item even if the original trade only
		// involved a single item stack.
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

	@Override
	protected void onTradeOver(TradingContext tradingContext) {
		super.onTradeOver(tradingContext);

		// Reset trade related state:
		this.currentOffer = null;
	}

	// TODO Simplify this? Maybe by separating into different, general utility functions.
	// TODO Support iterating in reverse order, for nicer looking container contents?
	// Returns the amount of currency that couldn't be removed, 0 on full success, negative if too
	// much was removed.
	protected int removeCurrency(@ReadOnly @Nullable ItemStack[] contents, int amount) {
		Validate.notNull(contents, "contents is null");
		Validate.isTrue(amount >= 0, "amount cannot be negative");
		if (amount == 0) return 0;
		int remaining = amount;

		// First pass: Remove as much low currency as available from partial stacks.
		// Second pass: Remove as much low currency as available from full stacks.
		Currency baseCurrency = Currencies.getBase();
		for (int k = 0; k < 2; k++) {
			for (int slot = 0; slot < contents.length; slot++) {
				ItemStack itemStack = contents[slot];
				if (!baseCurrency.getItemData().matches(itemStack)) continue;
				itemStack = Unsafe.assertNonNull(itemStack);

				// Second pass, or the ItemStack is a partial one:
				int itemAmount = itemStack.getAmount();
				if (k == 1 || itemAmount < itemStack.getMaxStackSize()) {
					int newAmount = (itemAmount - remaining);
					if (newAmount > 0) {
						// Copy the item before modifying it:
						itemStack = itemStack.clone();
						contents[slot] = itemStack;
						itemStack.setAmount(newAmount);
						remaining = 0;
						break;
					} else {
						contents[slot] = null;
						remaining = -newAmount;
						if (newAmount == 0) {
							break;
						}
					}
				}
			}
			if (remaining == 0) break;
		}
		if (remaining == 0) return 0;

		if (!Currencies.isHighCurrencyEnabled()) {
			// We couldn't remove all currency:
			return remaining;
		}

		Currency highCurrency = Currencies.getHigh();
		int remainingHigh = (int) Math.ceil((double) remaining / highCurrency.getValue());
		// We rounded the high currency up, so if this is negative now, it represents the remaining
		// change which needs to be added back:
		remaining -= (remainingHigh * highCurrency.getValue());
		assert remaining <= 0;

		// First pass: Remove high currency from partial stacks.
		// Second pass: Remove high currency from full stacks.
		for (int k = 0; k < 2; k++) {
			for (int slot = 0; slot < contents.length; slot++) {
				ItemStack itemStack = contents[slot];
				if (!highCurrency.getItemData().matches(itemStack)) continue;
				itemStack = Unsafe.assertNonNull(itemStack);

				// Second pass, or the ItemStack is a partial one:
				int itemAmount = itemStack.getAmount();
				if (k == 1 || itemAmount < itemStack.getMaxStackSize()) {
					int newAmount = (itemAmount - remainingHigh);
					if (newAmount > 0) {
						// Copy the item before modifying it:
						itemStack = itemStack.clone();
						contents[slot] = itemStack;
						itemStack.setAmount(newAmount);
						remainingHigh = 0;
						break;
					} else {
						contents[slot] = null;
						remainingHigh = -newAmount;
						if (newAmount == 0) {
							break;
						}
					}
				}
			}
			if (remainingHigh == 0) break;
		}

		remaining += (remainingHigh * highCurrency.getValue());
		if (remaining >= 0) {
			return remaining;
		}
		assert remaining < 0; // We have some change left
		remaining = -remaining; // The change is now represented as positive value

		// Add the remaining change into empty slots (all partial slots have already been cleared
		// above):
		// TODO This could probably be replaced with Utils.addItems
		int maxStackSize = baseCurrency.getMaxStackSize();
		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack itemStack = contents[slot];
			if (!ItemUtils.isEmpty(itemStack)) continue;

			int stackSize = Math.min(remaining, maxStackSize);
			contents[slot] = baseCurrency.getItemData().createItemStack(stackSize);
			remaining -= stackSize;
			if (remaining == 0) break;
		}
		// We removed too much, represent as negative value:
		remaining = -remaining;
		return remaining;
	}
}
