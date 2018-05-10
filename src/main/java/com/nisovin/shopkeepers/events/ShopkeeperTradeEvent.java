package com.nisovin.shopkeepers.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.TradingRecipe;

/**
 * This event is called whenever a player is about to trade with a shopkeeper.
 * <p>
 * It shares its cancelled state with the original {@link InventoryClickEvent}:<br>
 * Canceling the click event will cancel the trade and canceling the trade is implemented by canceling the original
 * click event.<br>
 * It is recommended to not modify the original click event (besides the cancel state).<br>
 * Also note that the shopkeeper has the final say on whether the trade will be cancelled: It is quite possible that
 * this event is not cancelled but the shopkeeper cancels the trade afterwards nevertheless for some reason. So if you
 * are interested in the actual outcome of the trade take a look at the {@link ShopkeeperTradeCompletedEvent}.
 * </p>
 */
public class ShopkeeperTradeEvent extends Event implements Cancellable {

	private final Shopkeeper shopkeeper;
	private final Player player;
	private final InventoryClickEvent clickEvent;
	private final TradingRecipe tradingRecipe;

	public ShopkeeperTradeEvent(Shopkeeper shopkeeper, Player player, InventoryClickEvent clickEvent, TradingRecipe tradingRecipe) {
		this.shopkeeper = shopkeeper;
		this.player = player;
		this.clickEvent = clickEvent;
		this.tradingRecipe = tradingRecipe;
	}

	/**
	 * Gets the trading player.
	 * 
	 * @return the player involved in this trade
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the trading {@link Shopkeeper}.
	 * 
	 * @return the {@link Shopkeeper} involved in this trade
	 */
	public Shopkeeper getShopkeeper() {
		return shopkeeper;
	}

	/**
	 * Gets the {@link InventoryClickEvent} which originally triggered this trade.
	 * 
	 * <p>
	 * Do not modify the returned event!
	 * </p>
	 * 
	 * @return the original {@link InventoryClickEvent}
	 */
	public InventoryClickEvent getClickEvent() {
		return clickEvent;
	}

	/**
	 * Gets the trading recipe used by this trade.
	 * 
	 * @return the used trading recipe
	 */
	public TradingRecipe getTradingRecipe() {
		return tradingRecipe;
	}

	/**
	 * If cancelled the trade will not take place.
	 */
	@Override
	public boolean isCancelled() {
		return clickEvent.isCancelled();
	}

	/**
	 * If cancelled the trade will not take place.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		clickEvent.setCancelled(cancelled);
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
