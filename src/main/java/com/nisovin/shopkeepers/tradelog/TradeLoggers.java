package com.nisovin.shopkeepers.tradelog;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.tradelog.csv.CsvTradeLogger;
import com.nisovin.shopkeepers.tradelog.data.TradeRecord;
import com.nisovin.shopkeepers.util.Validate;

public class TradeLoggers implements Listener {

	private final Plugin plugin;
	private final List<TradeLogger> loggers = new ArrayList<>();

	public TradeLoggers(Plugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		if (Settings.logTradesToCsv) {
			loggers.add(new CsvTradeLogger(plugin));
		}
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public void onDisable() {
		// Don't react to new trades:
		HandlerList.unregisterAll(this);

		// Wait for any pending writes to complete:
		loggers.forEach(TradeLogger::flush);
		loggers.clear();
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onTradeCompleted(ShopkeeperTradeEvent event) {
		if (loggers.isEmpty()) return; // Nothing to log

		TradeRecord trade = TradeRecord.create(event);
		loggers.forEach(logger -> logger.logTrade(trade));
	}
}
