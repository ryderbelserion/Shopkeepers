package com.nisovin.shopkeepers.trading.commandtrading;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.trading.TradeEffect;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class TradedCommandsTradeEffect implements TradeEffect {

	private final @Nullable String item1Command;
	private final int item1CommandCount;
	private final @Nullable String item2Command;
	private final int item2CommandCount;
	private final @Nullable String resultItemCommand;
	private final int resultItemCommandCount;

	public TradedCommandsTradeEffect(
			@Nullable String item1Command,
			int item1CommandCount,
			@Nullable String item2CommandCount,
			int item2Amount,
			@Nullable String resultItemCommand,
			int resultItemCommandCount
	) {
		this.item1Command = item1Command;
		this.item1CommandCount = item1CommandCount;
		this.item2Command = item2CommandCount;
		this.item2CommandCount = item2Amount;
		this.resultItemCommand = resultItemCommand;
		this.resultItemCommandCount = resultItemCommandCount;
	}

	@Override
	public void onTradeAborted(ShopkeeperTradeEvent tradeEvent) {
	}

	@Override
	public void onTradeApplied(ShopkeeperTradeEvent tradeEvent) {
		// Dispatch the traded commands:
		// This is done during trade application, before the ShopkeeperTradeCompletedEvent is
		// called.
		this.dispatchTradedCommand(tradeEvent, item1Command, item1CommandCount);
		this.dispatchTradedCommand(tradeEvent, item2Command, item2CommandCount);
		this.dispatchTradedCommand(tradeEvent, resultItemCommand, resultItemCommandCount);
	}

	private void dispatchTradedCommand(ShopkeeperTradeEvent tradeEvent, @Nullable String command, int count) {
		if (command == null || count <= 0) return;

		// Replace placeholders:
		Player tradingPlayer = tradeEvent.getPlayer();
		Shopkeeper shopkeeper = tradeEvent.getShopkeeper();
		String preparedCommand = StringUtils.replaceArguments(command,
				"player_name", Unsafe.assertNonNull(tradingPlayer.getName()),
				"player_uuid", tradingPlayer.getUniqueId(),
				"player_displayname", tradingPlayer.getDisplayName(),
				"shop_uuid", shopkeeper.getUniqueId()
		);

		// Dispatch the command:
		Log.debug("Dispatching " + count + "x traded command \"" + preparedCommand + "\"");
		for (int i = 0; i < count; i++) {
			try {
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), preparedCommand);
			} catch (Exception e) {
				Log.warning("Error during the execution of the traded command \"" + preparedCommand + "\"", e);
				// Since we don't know why the command failed, we don't abort the execution here,
				// but continue to dispatch the command the intended number of times.
			}
		}
	}
}
