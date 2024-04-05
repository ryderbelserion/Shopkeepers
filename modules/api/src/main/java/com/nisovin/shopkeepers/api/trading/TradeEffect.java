package com.nisovin.shopkeepers.api.trading;

import com.nisovin.shopkeepers.api.events.ShopkeeperTradeCompletedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;

/**
 * A trade effect that can be added to the {@link ShopkeeperTradeEvent#getTradeEffects()} in order
 * to be invoked once the trade is either {@link TradeEffect#onTradeAborted(ShopkeeperTradeEvent)
 * aborted} or {@link TradeEffect#onTradeApplied(ShopkeeperTradeEvent) applied}.
 * <p>
 * This can be useful when implementing custom trade effects that require some processing to happen
 * both for the {@link ShopkeeperTradeEvent} but also once the trade was actually applied, and that
 * can benefit from sharing some state between those two phases. For example, when implementing a
 * custom trade effect that executes traded command items, we need to check if any of the traded
 * items contain commands to execute, retrieve the commands, clear those items so that they are not
 * received by the trading player or shopkeeper, and only once the trade is actually applied, we
 * want to dispatch the previously retrieved commands. This can be implemented quite easily with a
 * {@link TradeEffect} that captures the retrieved commands and dispatches them once the trade is
 * applied, without having to analyze the involved items more than once, or taking care of cleaning
 * up any previously captured state.
 */
public interface TradeEffect {

	/**
	 * This is called when the trade was aborted.
	 * <p>
	 * Do not modify the given {@link ShopkeeperTradeEvent} at this point!
	 * 
	 * @param tradeEvent
	 *            the aborted trade
	 */
	public void onTradeAborted(ShopkeeperTradeEvent tradeEvent);

	/**
	 * This is called when the trade is being applied, after most of the default trade application,
	 * but before the {@link ShopkeeperTradeCompletedEvent}.
	 * <p>
	 * This can be used to apply additional trade effects.
	 * <p>
	 * Do not modify the given {@link ShopkeeperTradeEvent} at this point!
	 * 
	 * @param tradeEvent
	 *            the trade
	 */
	public void onTradeApplied(ShopkeeperTradeEvent tradeEvent);
}
