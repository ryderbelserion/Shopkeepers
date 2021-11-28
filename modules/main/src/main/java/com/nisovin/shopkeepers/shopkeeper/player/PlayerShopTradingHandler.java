package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.trading.Trade;
import com.nisovin.shopkeepers.ui.trading.TradingContext;
import com.nisovin.shopkeepers.ui.trading.TradingHandler;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

public abstract class PlayerShopTradingHandler extends TradingHandler {

	// State related to the currently handled trade:
	protected Inventory containerInventory = null;
	protected ItemStack[] newContainerContents = null;

	protected PlayerShopTradingHandler(AbstractPlayerShopkeeper shopkeeper) {
		super(SKDefaultUITypes.TRADING(), shopkeeper);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		if (!super.canOpen(player, silent)) return false;
		PlayerShopkeeper shopkeeper = this.getShopkeeper();

		// Stop opening if trading shall be prevented while the owner is offline:
		if (Settings.preventTradingWhileOwnerIsOnline && !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			Player ownerPlayer = shopkeeper.getOwner();
			if (ownerPlayer != null) {
				if (!silent) {
					this.debugNotOpeningUI(player, "Shop owner is online.");
					TextUtils.sendMessage(player, Messages.cannotTradeWhileOwnerOnline, "owner", ownerPlayer.getName());
				}
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean prepareTrade(Trade trade) {
		if (!super.prepareTrade(trade)) return false;
		AbstractPlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = trade.getTradingPlayer();

		// No trading with own shop:
		if (Settings.preventTradingWithOwnShop && shopkeeper.isOwner(tradingPlayer)
				&& !PermissionUtils.hasPermission(tradingPlayer, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWithOwnShop);
			this.debugPreventedTrade(tradingPlayer, "Trading with the own shop is not allowed.");
			return false;
		}

		// No trading while shop owner is online:
		if (Settings.preventTradingWhileOwnerIsOnline) {
			Player ownerPlayer = shopkeeper.getOwner();
			if (ownerPlayer != null && !shopkeeper.isOwner(tradingPlayer)
					&& !PermissionUtils.hasPermission(tradingPlayer, ShopkeepersPlugin.BYPASS_PERMISSION)) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWhileOwnerOnline, "owner", ownerPlayer.getName());
				this.debugPreventedTrade(tradingPlayer, "Trading is not allowed while the shop owner is online.");
				return false;
			}
		}

		// Check for the shop's container:
		Inventory containerInventory = shopkeeper.getContainerInventory();
		if (containerInventory == null) {
			TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeWithShopMissingContainer, "owner", shopkeeper.getOwnerName());
			this.debugPreventedTrade(tradingPlayer, "The shop's container is missing.");
			return false;
		}

		// Setup common state information for handling this trade:
		this.containerInventory = containerInventory;
		this.newContainerContents = containerInventory.getContents();

		return true;
	}

	@Override
	protected void onTradeApplied(Trade trade) {
		super.onTradeApplied(trade);

		// Apply container content changes:
		if (containerInventory != null && newContainerContents != null) {
			containerInventory.setContents(newContainerContents);
		}

		// Reset trade related state information:
		this.resetTradeState();
	}

	@Override
	protected void onTradeAborted(TradingContext tradingContext, boolean silent) {
		super.onTradeAborted(tradingContext, silent);
		this.resetTradeState();
	}

	protected void resetTradeState() {
		containerInventory = null;
		newContainerContents = null;
	}
}
