package com.nisovin.shopkeepers.api.trading;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeCompletedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;

/**
 * A trade effect that can be added to the {@link ShopkeeperTradeEvent#getTradeEffects()} in order
 * to be invoked once the trade is either {@link TradeEffect#onTradeAborted(ShopkeeperTradeEvent)
 * aborted} or {@link TradeEffect#onTradeCompleted(ShopkeeperTradeEvent) completed}.
 * <p>
 * This can be useful when implementing custom trade effects that require some processing to happen
 * both for the {@link ShopkeeperTradeEvent} but also once the trade was actually applied, and that
 * can benefit from sharing some state between those two phases. For example, when implementing a
 * custom trade effect that executes traded command items, we need to check if any of the traded
 * items are command items, retrieve the commands to execute, clear those items so that they are not
 * received by the trading player or shopkeeper, and only once the trade is actually applied, we
 * want to execute the previously retrieved commands. This can be implemented quite easily with a
 * {@link TradeEffect} that captures the retrieved commands and executes them once the trade is
 * applied, without having to analyze the involved items more than once, or taking care of cleaning
 * up any previously captured state.
 */
public interface TradeEffect {

	/**
	 * This is invoked when the trade was aborted.
	 * 
	 * @param tradeEvent
	 *            the aborted trade
	 */
	public void onTradeAborted(ShopkeeperTradeEvent tradeEvent);

	/**
	 * This is invoked when the trade was successfully applied, before the
	 * {@link ShopkeeperTradeCompletedEvent}.
	 * 
	 * @param tradeEvent
	 *            the completed trade
	 */
	public void onTradeCompleted(ShopkeeperTradeEvent tradeEvent);
}
