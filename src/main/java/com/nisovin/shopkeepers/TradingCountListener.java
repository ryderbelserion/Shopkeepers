package com.nisovin.shopkeepers;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerStatisticIncrementEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Tries to accurately detect individual trades by listening to corresponding changes of the TRADED_WITH_VILLAGER
 * statistic for a short period of time right after a player has clicked inside the inventories with open merchant
 * inventory view.
 * <p>
 * This cannot be used for handling the trades, because at that point the trading and player inventories were already
 * (at least partly) modified. It can however be useful for debugging purposes.
 */
class TradingCountListener implements Listener {

	private final ShopkeepersPlugin plugin;
	private final Runnable stopListeningAction = new Runnable() {

		@Override
		public void run() {
			stopListeningTask = null;
			stopListeningForTrades();
		}
	};

	private Player tradingPlayer = null;
	private int tradeCount = 0;
	private BukkitTask stopListeningTask = null;

	TradingCountListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	private void startListeningForTrades(Player tradingPlayer) {
		this.stopListeningForTrades();
		Log.debug("Listening for trades of player " + tradingPlayer.getName() + " ..");
		this.tradingPlayer = tradingPlayer;
		stopListeningTask = Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), stopListeningAction);
	}

	private void stopListeningForTrades() {
		if (tradingPlayer == null) return;
		Log.debug(".. Stopped listening for trades.");
		tradingPlayer = null;
		tradeCount = 0;
		if (stopListeningTask != null) {
			stopListeningTask.cancel();
			stopListeningTask = null;
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onInventoryClick(InventoryClickEvent event) {
		if (!Settings.debug) return;
		if (event.getWhoClicked().getType() != EntityType.PLAYER) return;
		if (!(event.getInventory() instanceof MerchantInventory)) return;
		Player player = (Player) event.getWhoClicked();
		UIType uiType = plugin.getUIManager().getOpenInterface(player);
		if (uiType != DefaultUIs.TRADING_WINDOW) return; // trading with a non-shopkeeper merchant

		MerchantInventory inventory = (MerchantInventory) event.getInventory();

		ItemStack resultItem = inventory.getItem(2);
		if (Utils.isEmpty(resultItem)) {
			return; // no trade available, ignoring
		}
		TradingRecipe usedRecipe = NMSManager.getProvider().getUsedTradingRecipe(inventory);
		if (usedRecipe == null) {
			return; // no used recipe found, ignoring
		}

		// start detecting trades:
		this.startListeningForTrades(player);
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerStatisticIncrement(PlayerStatisticIncrementEvent event) {
		if (!Settings.debug) return;
		if (event.getStatistic() != Statistic.TRADED_WITH_VILLAGER) return;
		Player player = event.getPlayer();
		if (!player.equals(tradingPlayer)) return;

		// sanity checks:
		int delta = (event.getNewValue() - event.getPreviousValue());
		if (delta != 1) {
			Log.debug("Trading-statistic-change: Expected statistic change of 1, but got " + delta);
			return;
		}
		Inventory inventory = player.getOpenInventory().getTopInventory();
		if (!(inventory instanceof MerchantInventory)) {
			Log.debug("Trading-statistic-change: Expected open merchant inventory, but got " + inventory.getType());
			return;
		}

		MerchantInventory merchantInventory = (MerchantInventory) inventory;
		// find the recipe minecraft is using for the trade (active recipe gets changed after the statistic change):
		TradingRecipe usedRecipe = NMSManager.getProvider().getUsedTradingRecipe(merchantInventory);
		if (usedRecipe == null) {
			Log.debug("Trading-statistic-change: Couldn't find used recipe.");
			return;
		}

		tradeCount++;
		Log.debug("Detected trade (#" + tradeCount + "): " + Utils.getSimpleRecipeInfo(usedRecipe));
	}
}
