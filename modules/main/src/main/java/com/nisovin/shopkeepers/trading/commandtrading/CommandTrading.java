package com.nisovin.shopkeepers.trading.commandtrading;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class CommandTrading implements Listener {

	private final ShopkeepersPlugin plugin;

	public CommandTrading(ShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public void onDisable() {
		HandlerList.unregisterAll(this);
	}

	@EventHandler(ignoreCancelled = true)
	void onShopkeeperTrade(ShopkeeperTradeEvent event) {
		UnmodifiableItemStack item1 = event.getReceivedItem1();
		UnmodifiableItemStack item2 = event.getReceivedItem2();
		UnmodifiableItemStack resultItem = event.getResultItem();

		@Nullable String item1Command = CommandTradingUtils.getTradedCommand(item1);
		@Nullable String item2Command = CommandTradingUtils.getTradedCommand(item2);
		@Nullable String resultItemCommand = CommandTradingUtils.getTradedCommand(resultItem);

		if (item1Command == null && item2Command == null && resultItemCommand == null) return;

		if (item1Command != null) {
			Log.debug("First item contains traded command \"" + item1Command + "\"");
			event.setReceivedItem1(null);
		}
		if (item2Command != null) {
			Log.debug("Second item contains traded command \"" + item2Command + "\"");
			event.setReceivedItem2(null);
		}
		if (resultItemCommand != null) {
			Log.debug("Result item contains traded command \"" + resultItemCommand + "\"");
			event.setResultItem(null);
		}

		TradedCommandsTradeEffect tradeEffect = new TradedCommandsTradeEffect(
				item1Command,
				item1Command != null ? Unsafe.assertNonNull(item1).getAmount() : 0,
				item2Command,
				item2Command != null ? Unsafe.assertNonNull(item2).getAmount() : 0,
				resultItemCommand,
				resultItem != null ? Unsafe.assertNonNull(resultItem).getAmount() : 0
		);
		event.getTradeEffects().add(tradeEffect);
	}
}
