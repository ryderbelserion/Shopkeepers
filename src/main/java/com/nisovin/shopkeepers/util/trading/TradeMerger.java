package com.nisovin.shopkeepers.util.trading;

import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Lazy;
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

	/**
	 * Represents merged equivalent trades.
	 * <p>
	 * Equivalent trades occur sequentially and involve the same player, shopkeeper, and traded items.
	 */
	public static class MergedTrades {

		private final ShopkeeperTradeEvent initialTrade;
		private final Lazy<ItemStack> resultItem;
		private final Lazy<ItemStack> offeredItem1;
		private final Lazy<ItemStack> offeredItem2;
		private int tradeCount = 1;

		private MergedTrades(ShopkeeperTradeEvent initialTrade) {
			assert initialTrade != null;
			this.initialTrade = initialTrade;
			resultItem = new Lazy<>(() -> initialTrade.getTradingRecipe().getResultItem());
			offeredItem1 = new Lazy<>(() -> initialTrade.getOfferedItem1());
			offeredItem2 = new Lazy<>(() -> initialTrade.getOfferedItem2());
		}

		/**
		 * Gets the initial trade.
		 * 
		 * @return the initial trade
		 */
		public ShopkeeperTradeEvent getInitialTrade() {
			return initialTrade;
		}

		/**
		 * Gets the result item of the trades. See {@link ShopkeeperTradeEvent#getTradingRecipe()} and
		 * {@link TradingRecipe#getResultItem()}.
		 * <p>
		 * The returned {@link ItemStack} is cached and reused. It is not meant to be modified!
		 * 
		 * @return the result item
		 */
		public ItemStack getResultItem() {
			return resultItem.get();
		}

		/**
		 * Gets the first offered item of the trades. See {@link ShopkeeperTradeEvent#getOfferedItem1()}.
		 * <p>
		 * The returned {@link ItemStack} is cached and reused. It is not meant to be modified!
		 * 
		 * @return the first offered item
		 */
		public ItemStack getOfferedItem1() {
			return offeredItem1.get();
		}

		/**
		 * Gets the second offered item of the trades. See {@link ShopkeeperTradeEvent#getOfferedItem2()}.
		 * <p>
		 * The returned {@link ItemStack} is cached and reused. It is not meant to be modified!
		 * 
		 * @return the second offered item, or <code>null</code>
		 */
		public ItemStack getOfferedItem2() {
			return offeredItem2.get();
		}

		/**
		 * Gets the number of merged equivalent trades.
		 * 
		 * @return the number of merged trades
		 */
		public int getTradeCount() {
			return tradeCount;
		}

		private boolean tryMergeWith(MergedTrades otherTrades, MergeMode mergeMode) {
			assert otherTrades != null && mergeMode != null;
			ShopkeeperTradeEvent otherInitialTrade = otherTrades.getInitialTrade();
			if (initialTrade.getClickEvent() != otherInitialTrade.getClickEvent()) {
				if (mergeMode == MergeMode.SAME_CLICK_EVENT) return false;
				if (initialTrade.getPlayer() != otherInitialTrade.getPlayer()) return false;
				if (initialTrade.getShopkeeper() != otherInitialTrade.getShopkeeper()) return false;

				// Note: We do not compare the trading recipes here, because the items offered by the player might be
				// different to those of the trading recipe, and therefore also among trades that use the same trading
				// recipe.
				// Items are compared with equals instead of isSimilar to also take stack sizes into account:
				if (!Objects.equals(this.getResultItem(), otherTrades.getResultItem())) return false;
				if (!Objects.equals(this.getOfferedItem1(), otherTrades.getOfferedItem1())) return false;
				if (!Objects.equals(this.getOfferedItem2(), otherTrades.getOfferedItem2())) return false;
			} else {
				// We assume that the player, shopkeeper, and the involved items (offered items and the result item)
				// remain the same throughout the same click event (this avoids costly item comparisons). However, if
				// the selected trading recipe changed throughout the trades caused by the same click event, the item
				// stack sizes of the involved trading recipes (i.e. of the offered items and the result items) might
				// have changed. We consider these to be a separate kinds of trades then.
				int resultItemAmount = ItemUtils.getItemStackAmount(this.getResultItem());
				int otherResultItemAmount = ItemUtils.getItemStackAmount(otherTrades.getResultItem());
				if (resultItemAmount != otherResultItemAmount) return false;

				int offeredItem1Amount = ItemUtils.getItemStackAmount(this.getOfferedItem1());
				int otherOfferedItem1Amount = ItemUtils.getItemStackAmount(otherTrades.getOfferedItem1());
				if (offeredItem1Amount != otherOfferedItem1Amount) return false;

				int offeredItem2Amount = ItemUtils.getItemStackAmount(this.getOfferedItem2());
				int otherOfferedItem2Amount = ItemUtils.getItemStackAmount(otherTrades.getOfferedItem2());
				if (offeredItem2Amount != otherOfferedItem2Amount) return false;
			}

			tradeCount += otherTrades.getTradeCount();
			return true;
		}
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
		this.processMergedTrades();
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
		} else if (!previousTrades.tryMergeWith(newMergedTrades, mergeMode)) {
			this.processMergedTrades();
			assert previousTrades == null;
			previousTrades = newMergedTrades;
			this.startDelayedTask();
		} // Else: The trade was merged.
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
			this.processMergedTrades();
		}, mergeDurationTicks);
	}

	private void processMergedTrades() {
		if (previousTrades == null) return;
		mergedTradesConsumer.accept(previousTrades);
		previousTrades = null;
	}
}
