package com.nisovin.shopkeepers.api.events;

import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.google.common.base.Preconditions;

/**
 * This event is called after a player has successfully traded with a shopkeeper.
 * <p>
 * This event can be used for anything that must only happen after a trade has been successfully
 * carried out, such as logging.
 * <p>
 * DO NOT modify the corresponding {@link InventoryClickEvent} or {@link ShopkeeperTradeEvent}.
 */
public class ShopkeeperTradeCompletedEvent extends ShopkeeperEvent {

	private final ShopkeeperTradeEvent completedTrade;

	/**
	 * Creates a new {@link ShopkeeperTradeCompletedEvent}.
	 * 
	 * @param tradeEvent
	 *            the completed trade event, not <code>null</code>
	 */
	public ShopkeeperTradeCompletedEvent(ShopkeeperTradeEvent tradeEvent) {
		super(tradeEvent.getShopkeeper());
		Preconditions.checkNotNull(tradeEvent, "tradeEvent is null");
		this.completedTrade = tradeEvent;
	}

	/**
	 * Gets the completed {@link ShopkeeperTradeEvent}.
	 * <p>
	 * Do not modify the {@link ShopkeeperTradeEvent} in any way!
	 * 
	 * @return the completed trade event, not <code>null</code>
	 */
	public ShopkeeperTradeEvent getCompletedTrade() {
		return completedTrade;
	}

	private static final HandlerList handlers = new HandlerList();

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * Gets the {@link HandlerList} of this event.
	 * 
	 * @return the handler list
	 */
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
