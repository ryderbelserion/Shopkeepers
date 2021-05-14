package com.nisovin.shopkeepers.util.trading;

import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Merges sequentially triggered shopkeeper trades that involve the same player, shopkeeper, and items.
 * <p>
 * Once a trade is encountered that cannot be merged with the previous trades, or once a certain maximum duration has
 * passed, an initially provided {@link Consumer} is informed about the merged trades.
 */
public class TradeMerger {

	/**
	 * Different trade merging behaviors.
	 * <p>
	 * Regardless of the chosen {@link MergeMode}, trades are always only merged if they take place sequentially and
	 * involve the same player, shopkeeper, and items.
	 */
	public enum MergeMode {
		/**
		 * Merges equivalent trades that were triggered by the same {@link InventoryClickEvent} (eg. when multiple
		 * trades are automatically triggered by a single shift click).
		 */
		SAME_CLICK_EVENT,
		/**
		 * Merges equivalent trades over a certain duration (at most 5 seconds by default).
		 */
		DURATION
	}

	private static final long DEFAULT_MERGE_DURATION_TICKS = 100L; // 5 seconds

	private final Plugin plugin;
	private final Consumer<MergedTrades> mergedTradesConsumer;
	private final MergeMode mergeMode;
	private long mergeDurationTicks;

	private MergedTrades previousTrades = null;
	private BukkitTask delayedTask = null;

	public TradeMerger(Plugin plugin, MergeMode mergeMode, Consumer<MergedTrades> mergedTradesConsumer) {
		Validate.notNull(plugin, "plugin");
		Validate.notNull(mergeMode, "mergeMode");
		Validate.notNull(mergedTradesConsumer, "mergedTradesConsumer");
		this.plugin = plugin;
		this.mergedTradesConsumer = mergedTradesConsumer;
		this.mergeMode = mergeMode;
		mergeDurationTicks = (mergeMode == MergeMode.SAME_CLICK_EVENT) ? 1L : DEFAULT_MERGE_DURATION_TICKS;
	}

	public TradeMerger withMergeDuration(long mergeDurationTicks) {
		Validate.State.isTrue(mergeMode == MergeMode.DURATION, "Calling this method is only valid if the MergeMode is DURATION.");
		Validate.isTrue(mergeDurationTicks >= 1, "mergeDurationTicks has to be positive");
		this.mergeDurationTicks = mergeDurationTicks;
		return this;
	}

	public void onEnable() {
	}

	public void onDisable() {
		this.endDelayedTask();
		// Process the pending trades, if there are any:
		this.processPreviousTrades();
	}

	/**
	 * Tries to merge the given trade with the previous trades.
	 * 
	 * @param event
	 *            the trade event
	 */
	public void mergeTrade(ShopkeeperTradeEvent event) {
		Validate.notNull(event, "event");
		// In order to check if the trade can be merged with the previous trades, we most likely need to retrieve item
		// copies from the event. By creating the new MergedTrades right away, instead of only afterwards when it is
		// actually required, we can cache these once retrieved item copies. This is therefore cheaper most of the time.
		MergedTrades newMergedTrades = new MergedTrades(event);
		if (previousTrades == null) {
			previousTrades = newMergedTrades;
			this.startDelayedTask();
		} else if (!this.tryMergeTrades(previousTrades, newMergedTrades, mergeMode)) {
			this.processPreviousTrades();
			assert previousTrades == null;
			previousTrades = newMergedTrades;
			this.startDelayedTask();
		} // Else: The trade was merged.
	}

	private boolean canMergeTrades(MergedTrades target, MergedTrades other, MergeMode mergeMode) {
		if (mergeMode == MergeMode.SAME_CLICK_EVENT) {
			if (target.getInitialTrade().getClickEvent() != other.getInitialTrade().getClickEvent()) {
				return false;
			}
		}
		return target.canMerge(other);
	}

	private boolean tryMergeTrades(MergedTrades target, MergedTrades other, MergeMode mergeMode) {
		if (this.canMergeTrades(target, other, mergeMode)) {
			// Merge trades:
			target.addTrades(other.getTradeCount());
			return true;
		}
		return false;
	}

	private void endDelayedTask() {
		// End the current delayed task, if there is one:
		if (delayedTask != null) {
			delayedTask.cancel();
			delayedTask = null;
		}
	}

	private void startDelayedTask() {
		// End the previous task, if there is one:
		this.endDelayedTask();

		// Start a new delayed task that ends the trade merging after a certain maximum duration:
		delayedTask = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			delayedTask = null;
			this.processPreviousTrades();
		}, mergeDurationTicks);
	}

	/**
	 * Stops merging trades with the previous trades that are still pending to be processed, and processes them.
	 * <p>
	 * Calling this method has no effect if there are no pending trades to process.
	 */
	public void processPreviousTrades() {
		if (previousTrades == null) return;
		this.endDelayedTask();
		mergedTradesConsumer.accept(previousTrades);
		previousTrades = null;
	}
}
