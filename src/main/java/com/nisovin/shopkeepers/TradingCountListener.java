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

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.api.util.ItemUtils;
import com.nisovin.shopkeepers.api.util.TradingRecipe;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.ui.defaults.DefaultUITypes;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SKItemUtils;

/**
 * Tries to accurately detect individual trades handled by minecraft by listening to corresponding changes of the
 * TRADED_WITH_VILLAGER statistic for a short period of time right after a player has clicked inside the merchant
 * inventory view.
 * <p>
 * This cannot be used for handling or canceling the trades, because at that point the trading and player inventory was
 * already (at least partly) modified. It can however be useful for debugging purposes.
 * <p>
 * Since shopkeepers handles its trades on its own, only trades with vanilla villagers get detected here. This can
 * currently be used to debug and compare vanilla trading behavior with Shopkeeper's trading behavior.
 */
class TradingCountListener implements Listener {

	private final ShopkeepersPlugin plugin;
	private final Runnable stopListeningAction = () -> {
		stopListeningTask = null;
		stopListeningForTrades();
	};

	private Player tradingPlayer = null;
	private int tradeCounter = 0;
	private BukkitTask stopListeningTask = null;

	TradingCountListener(ShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	private void startListeningForTrades(Player tradingPlayer) {
		this.stopListeningForTrades();
		Log.debug("Listening for non-shopkeeper trades of player " + tradingPlayer.getName() + " ..");
		this.tradingPlayer = tradingPlayer;
		stopListeningTask = Bukkit.getScheduler().runTask(plugin, stopListeningAction);
	}

	private void stopListeningForTrades() {
		if (tradingPlayer == null) return;
		Log.debug(".. Stopped listening for non-shopkeeper trades.");
		tradingPlayer = null;
		tradeCounter = 0;
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
		UIType uiType = plugin.getUIRegistry().getOpenUIType(player);
		if (uiType == DefaultUITypes.TRADING()) return; // trading with a shopkeeper, which handles trades on its own

		MerchantInventory inventory = (MerchantInventory) event.getInventory();
		ItemStack resultItem = inventory.getItem(2);
		if (ItemUtils.isEmpty(resultItem)) {
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
			Log.debug("Non-shopkeeper trade detection: Expected trading statistic change of 1, but got " + delta);
			return;
		}
		Inventory inventory = player.getOpenInventory().getTopInventory();
		if (!(inventory instanceof MerchantInventory)) {
			Log.debug("Non-shopkeeper trade detection: Expected open merchant inventory, but got " + inventory.getType());
			return;
		}

		MerchantInventory merchantInventory = (MerchantInventory) inventory;
		// find the recipe minecraft is using for the trade (the active recipe gets updated after the statistic change):
		TradingRecipe usedRecipe = NMSManager.getProvider().getUsedTradingRecipe(merchantInventory);
		if (usedRecipe == null) {
			Log.debug("Non-shopkeeper trade detection: Couldn't find the used trading recipe.");
			return;
		}

		tradeCounter++;
		Log.debug("Detected non-shopkeeper trade (#" + tradeCounter + "): " + SKItemUtils.getSimpleRecipeInfo(usedRecipe));
	}
}
