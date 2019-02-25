package com.nisovin.shopkeepers.shopkeeper.player.buy;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class BuyingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected BuyingPlayerShopTradingHandler(BuyingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public BuyingPlayerShopkeeper getShopkeeper() {
		return (BuyingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		BuyingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;
		TradingRecipe tradingRecipe = tradeData.tradingRecipe;

		// get offer for the bought item:
		ItemStack boughtItem = tradingRecipe.getItem1();
		PriceOffer offer = shopkeeper.getOffer(boughtItem);
		if (offer == null) {
			// this should not happen.. because the recipes were created based on the shopkeeper's offers
			this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
			return false;
		}

		// validate the found offer:
		int expectedBoughtItemAmount = offer.getItem().getAmount();
		if (expectedBoughtItemAmount > boughtItem.getAmount()) {
			// this shouldn't happen .. because the recipe was created based on this offer
			this.debugPreventedTrade(tradingPlayer, "The offer doesn't match the trading recipe!");
			return false;
		}

		assert chestInventory != null & newChestContents != null;

		// remove currency items from chest contents:
		int remaining = this.removeCurrency(newChestContents, offer.getPrice());
		if (remaining > 0) {
			this.debugPreventedTrade(tradingPlayer, "The shop's chest doesn't contain enough currency.");
			return false;
		} else if (remaining < 0) {
			this.debugPreventedTrade(tradingPlayer, "The shop's chest does not have enough space to split large currency items.");
			return false;
		}

		// add bought items to chest contents:
		int amountAfterTaxes = this.getAmountAfterTaxes(expectedBoughtItemAmount);
		if (amountAfterTaxes > 0) {
			// the item the trading player gave might slightly differ from the required item,
			// but is still accepted, depending on the used item comparison logic and settings:
			ItemStack receivedItem = tradeData.offeredItem1.clone(); // create a copy, just in case
			receivedItem.setAmount(amountAfterTaxes);
			if (ItemUtils.addItems(newChestContents, receivedItem) != 0) {
				this.debugPreventedTrade(tradingPlayer, "The shop's chest cannot hold the traded items.");
				return false;
			}
		}
		return true;
	}

	// TODO simplify this? Maybe by separating into different, general utility functions
	// TODO support iterating in reverse order, for nicer looking chest contents?
	// returns the amount of currency that couldn't be removed, 0 on full success, negative if too much was removed
	protected int removeCurrency(ItemStack[] contents, int amount) {
		Validate.notNull(contents);
		Validate.isTrue(amount >= 0, "Amount cannot be negative!");
		if (amount == 0) return 0;
		int remaining = amount;

		// first pass: remove as much low currency as available from partial stacks
		// second pass: remove as much low currency as available from full stacks
		for (int k = 0; k < 2; k++) {
			for (int slot = 0; slot < contents.length; slot++) {
				ItemStack itemStack = contents[slot];
				if (!Settings.isCurrencyItem(itemStack)) continue;

				// second pass, or the itemstack is a partial one:
				int itemAmount = itemStack.getAmount();
				if (k == 1 || itemAmount < itemStack.getMaxStackSize()) {
					int newAmount = (itemAmount - remaining);
					if (newAmount > 0) {
						// copy the item before modifying it:
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

		if (!Settings.isHighCurrencyEnabled()) {
			// we couldn't remove all currency:
			return remaining;
		}

		int remainingHigh = (int) Math.ceil((double) remaining / Settings.highCurrencyValue);
		// we rounded the high currency up, so if this is negative now, it represents the remaining change which
		// needs to be added back:
		remaining -= (remainingHigh * Settings.highCurrencyValue);
		assert remaining <= 0;

		// first pass: remove high currency from partial stacks
		// second pass: remove high currency from full stacks
		for (int k = 0; k < 2; k++) {
			for (int slot = 0; slot < contents.length; slot++) {
				ItemStack itemStack = contents[slot];
				if (!Settings.isHighCurrencyItem(itemStack)) continue;

				// second pass, or the itemstack is a partial one:
				int itemAmount = itemStack.getAmount();
				if (k == 1 || itemAmount < itemStack.getMaxStackSize()) {
					int newAmount = (itemAmount - remainingHigh);
					if (newAmount > 0) {
						// copy the item before modifying it:
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

		remaining += (remainingHigh * Settings.highCurrencyValue);
		if (remaining >= 0) {
			return remaining;
		}
		assert remaining < 0; // we have some change left
		remaining = -remaining; // the change is now represented as positive value

		// add the remaining change into empty slots (all partial slots have already been cleared above):
		// TODO this could probably be replaced with Utils.addItems
		int maxStackSize = Settings.currencyItem.getMaxStackSize();
		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack itemStack = contents[slot];
			if (!ItemUtils.isEmpty(itemStack)) continue;

			int stackSize = Math.min(remaining, maxStackSize);
			contents[slot] = Settings.createCurrencyItem(stackSize);
			remaining -= stackSize;
			if (remaining == 0) break;
		}
		// we removed too much, represent as negative value:
		remaining = -remaining;
		return remaining;
	}
}
