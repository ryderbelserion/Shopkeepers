package com.nisovin.shopkeepers.ui.trading;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.TradeSelectEvent;

import com.nisovin.shopkeepers.api.ui.UISession;

/**
 * {@link TradingListener}s can be registered via {@link TradingHandler#addListener(TradingListener)} and are then
 * informed on various events during the handling of trades.
 */
public abstract class TradingListener {

	/**
	 * This is called very early, prior to the {@link TradingHandler}'s own processing, for every
	 * {@link InventoryClickEvent} forwarded to the {@link TradingHandler}.
	 * 
	 * @param uiSession
	 *            the {@link UISession}, not <code>null</code>
	 * @param event
	 *            the inventory click event, not <code>null</code>
	 */
	public void onInventoryClick(UISession uiSession, InventoryClickEvent event) {
	}

	/**
	 * This is called very early, prior to the {@link TradingHandler}'s own processing, for every
	 * {@link TradeSelectEvent} forwarded to the {@link TradingHandler}.
	 * 
	 * @param uiSession
	 *            the {@link UISession}, not <code>null</code>
	 * @param event
	 *            the trade select event, not <code>null</code>
	 */
	public void onTradeSelect(UISession uiSession, TradeSelectEvent event) {
	}

	/**
	 * This is called after a trade has been successfully completed and applied.
	 * 
	 * @param trade
	 *            the trade, not <code>null</code>
	 * @param silent
	 *            <code>true</code> to skip any actions that might be noticeable by players on the server
	 */
	public void onTradeCompleted(Trade trade, boolean silent) {
	}

	/**
	 * This is called whenever a trade attempt has been cancelled for some reason.
	 * <p>
	 * This is not called for cancelled {@link InventoryClickEvent}s, or inventory actions that are ignored because they
	 * would not result in a trade in vanilla Minecraft either.
	 * <p>
	 * If available, the corresponding {@link Trade} instance can be retrieved via
	 * {@link TradingContext#getCurrentTrade()}. However, trade attempts can also be aborted before a corresponding
	 * valid {@link Trade} instance could be created. {@link TradingContext#getCurrentTrade()} will then return
	 * <code>null</code>. {@link TradingContext#getTradeCount()} will always reflect the aborted trade attempt.
	 * <p>
	 * When a trade has been cancelled, no further trades will be processed for the same {@link TradingContext}.
	 * 
	 * @param tradingContext
	 *            the trading context, not <code>null</code>
	 * @param silent
	 *            <code>true</code> to skip any actions that might be noticeable by players on the server
	 */
	public void onTradeAborted(TradingContext tradingContext, boolean silent) {
	}
}
