package com.nisovin.shopkeepers.shopkeeper.player.trade;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopTradingHandler;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

	protected TradingPlayerShopTradingHandler(SKTradingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKTradingPlayerShopkeeper getShopkeeper() {
		return (SKTradingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;
		SKTradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = trade.getTradingPlayer();
		TradingRecipe tradingRecipe = trade.getTradingRecipe();

		// Find offer:
		TradeOffer offer = shopkeeper.getOffer(tradingRecipe);
		if (offer == null) {
			// Unexpected, because the recipes were created based on the shopkeeper's offers.
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
			this.debugPreventedTrade(
					tradingPlayer,
					"Could not find the offer corresponding to the trading recipe!"
			);
			return false;
		}

		@Nullable ItemStack[] newContainerContents = Unsafe.assertNonNull(this.newContainerContents);

		// Remove result items from container contents:
		UnmodifiableItemStack resultItem = tradingRecipe.getResultItem();
		assert resultItem != null;
		if (InventoryUtils.removeItems(newContainerContents, resultItem) != 0) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStock);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container does not contain the required items."
			);
			return false;
		}

		// Add the traded items to the container contents:
		UnmodifiableItemStack recipeItem1 = tradingRecipe.getItem1();
		UnmodifiableItemStack recipeItem2 = tradingRecipe.getItem2();
		ItemStack offeredItem1 = trade.getOfferedItem1();
		ItemStack offeredItem2 = trade.getOfferedItem2();
		if (!this.addItems(newContainerContents, recipeItem1, offeredItem1)
				|| !this.addItems(newContainerContents, recipeItem2, offeredItem2)) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeInsufficientStorageSpace);
			this.debugPreventedTrade(
					tradingPlayer,
					"The shop's container cannot hold the traded items."
			);
			return false;
		}
		return true;
	}

	// We differentiate between the required and the offered items here, because the items that the
	// trading player uses in the trade might be slightly different to the items that are required
	// according to the trading recipe. But depending on Minecraft's item comparison rules and our
	// settings, the items of the trading player might still be accepted for the trade.
	// Returns false, if not all items could be added to the contents.
	private boolean addItems(
			@ReadOnly @Nullable ItemStack @ReadWrite [] contents,
			@Nullable UnmodifiableItemStack requiredItem,
			@Nullable ItemStack offeredItem
	) {
		if (ItemUtils.isEmpty(requiredItem)) return true;
		Unsafe.assertNonNull(requiredItem);
		Unsafe.assertNonNull(offeredItem);
		assert requiredItem != null && offeredItem != null;
		int amountAfterTaxes = this.getAmountAfterTaxes(requiredItem.getAmount());
		if (amountAfterTaxes > 0) {
			int remaining = InventoryUtils.addItems(contents, offeredItem, amountAfterTaxes);
			if (remaining != 0) {
				// Could not add all items to the contents:
				return false;
			}
		}
		return true;
	}
}
