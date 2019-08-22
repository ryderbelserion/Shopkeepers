package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public abstract class PlayerShopTradingHandler extends TradingHandler {

	// state related to the currently handled trade:
	protected Inventory chestInventory = null;
	protected ItemStack[] newChestContents = null;

	protected PlayerShopTradingHandler(AbstractPlayerShopkeeper shopkeeper) {
		super(SKDefaultUITypes.TRADING(), shopkeeper);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean canOpen(Player player) {
		if (!super.canOpen(player)) return false;
		PlayerShopkeeper shopkeeper = this.getShopkeeper();

		// stop opening if trading shall be prevented while the owner is offline:
		if (Settings.preventTradingWhileOwnerIsOnline && !PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			Player ownerPlayer = shopkeeper.getOwner();
			if (ownerPlayer != null) {
				Log.debug("Blocked trade window opening from " + player.getName() + " because the owner is online");
				TextUtils.sendMessage(player, Settings.msgCantTradeWhileOwnerOnline, "{owner}", ownerPlayer.getName());
				return false;
			}
		}
		return true;
	}

	@Override
	protected boolean prepareTrade(TradeData tradeData) {
		if (!super.prepareTrade(tradeData)) return false;
		PlayerShopkeeper shopkeeper = this.getShopkeeper();
		Player tradingPlayer = tradeData.tradingPlayer;

		// no trading with own shop:
		if (Settings.preventTradingWithOwnShop && shopkeeper.isOwner(tradingPlayer) && !tradingPlayer.isOp()) {
			this.debugPreventedTrade(tradingPlayer, "Trading with the own shop is not allowed.");
			return false;
		}

		// no trading while shop owner is online:
		if (Settings.preventTradingWhileOwnerIsOnline && !PermissionUtils.hasPermission(tradingPlayer, ShopkeepersPlugin.BYPASS_PERMISSION)) {
			Player ownerPlayer = shopkeeper.getOwner();
			if (ownerPlayer != null && !shopkeeper.isOwner(tradingPlayer)) {
				TextUtils.sendMessage(tradingPlayer, Settings.msgCantTradeWhileOwnerOnline, "{owner}", ownerPlayer.getName());
				this.debugPreventedTrade(tradingPlayer, "Trading is not allowed while the shop owner is online.");
				return false;
			}
		}

		// check for the shop's chest:
		Block chest = shopkeeper.getChest();
		if (!ItemUtils.isChest(chest.getType())) {
			this.debugPreventedTrade(tradingPlayer, "Couldn't find the shop's chest.");
			return false;
		}

		// setup common state information for handling this trade:
		this.chestInventory = ((Chest) chest.getState()).getInventory();
		this.newChestContents = chestInventory.getContents();

		return true;
	}

	@Override
	protected void onTradeApplied(TradeData tradeData) {
		super.onTradeApplied(tradeData);

		// apply chest content changes:
		if (chestInventory != null && newChestContents != null) {
			chestInventory.setContents(newChestContents);
		}

		// reset trade related state information:
		this.resetTradeState();
	}

	@Override
	protected void onTradeAborted(TradeData tradeData) {
		super.onTradeAborted(tradeData);
		this.resetTradeState();
	}

	protected void resetTradeState() {
		chestInventory = null;
		newChestContents = null;
	}
}
