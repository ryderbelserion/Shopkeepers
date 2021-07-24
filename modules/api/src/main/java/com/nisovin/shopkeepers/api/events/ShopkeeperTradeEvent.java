package com.nisovin.shopkeepers.api.events;

import org.apache.commons.lang.Validate;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * This event is called whenever a player is about to trade with a shopkeeper. Canceling it will cause the trade to not
 * get applied.
 * <p>
 * Depending on the inventory action, a single inventory click of a player might trigger several successive trades
 * (possibly even using different trading recipes). Canceling a trade will also cancel all successive trades.
 * <p>
 * All other preconditions regarding the trade have already been checked before this event gets called. So if this event
 * does not get cancelled you can assume that the trade is going to get applied.
 * <p>
 * DO NOT modify the corresponding {@link InventoryClickEvent}, any affected inventories (player, merchant, shop
 * container, ..), or any other state which might be affected by the trade during the handling of this event!
 */
public class ShopkeeperTradeEvent extends ShopkeeperEvent implements Cancellable {

	private final Player player;
	private final InventoryClickEvent clickEvent;
	private final TradingRecipe tradingRecipe;
	private final UnmodifiableItemStack offeredItem1;
	private final UnmodifiableItemStack offeredItem2; // Can be null
	private final boolean swappedItemOrder;
	private boolean cancelled = false;

	/**
	 * Creates a new {@link ShopkeeperTradeEvent}.
	 * <p>
	 * The offered items are expected to be immutable and their stack sizes match the trading recipe items.
	 * 
	 * @param shopkeeper
	 *            the involved shopkeeper, not <code>null</code>
	 * @param player
	 *            the trading player, not <code>null</code>
	 * @param clickEvent
	 *            the click event that triggered the trade, not <code>null</code>
	 * @param tradingRecipe
	 *            the trading recipe, not <code>null</code>
	 * @param offeredItem1
	 *            the offered item that matches the first required item of the trading recipe, not <code>null</code> or
	 *            empty
	 * @param offeredItem2
	 *            the offered item that matches the second required item of the trading recipe, can be <code>null</code>
	 * @param swappedItemOrder
	 *            <code>true</code> if the player provided the offered items in reverse order
	 */
	public ShopkeeperTradeEvent(Shopkeeper shopkeeper, Player player, InventoryClickEvent clickEvent, TradingRecipe tradingRecipe,
								UnmodifiableItemStack offeredItem1, UnmodifiableItemStack offeredItem2, boolean swappedItemOrder) {
		super(shopkeeper);
		Validate.notNull(player, "player");
		Validate.notNull(clickEvent, "clickEvent");
		Validate.notNull(tradingRecipe, "tradingRecipe");
		Validate.notNull(offeredItem1, "offeredItem1");
		this.player = player;
		this.clickEvent = clickEvent;
		this.tradingRecipe = tradingRecipe;
		this.offeredItem1 = offeredItem1;
		this.offeredItem2 = offeredItem2; // Can be null
		this.swappedItemOrder = swappedItemOrder;
	}

	/**
	 * Gets the trading player.
	 * 
	 * @return the trading player, not <code>null</code>
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the {@link InventoryClickEvent} that triggered this trade.
	 * <p>
	 * Note that a single inventory click event can trigger multiple consecutive trades.
	 * <p>
	 * Do not modify the click event or any of the involved items! It needs to remain cancelled.
	 * 
	 * @return the inventory click event, not <code>null</code>
	 */
	public InventoryClickEvent getClickEvent() {
		return clickEvent;
	}

	/**
	 * Gets the trading recipe used by this trade.
	 * 
	 * @return the used trading recipe, not <code>null</code>
	 */
	public TradingRecipe getTradingRecipe() {
		return tradingRecipe;
	}

	/**
	 * Gets an unmodifiable view on the item offered by the player that matches the first required item of the used
	 * trading recipe. This is not necessarily the item in the first slot.
	 * <p>
	 * The type and stack size equal those of the required item of the trading recipe. The metadata however can differ,
	 * but still be accepted for the trade depending on the item matching rules of the used Minecraft version and the
	 * shopkeeper settings (i.e. with strict item comparisons being disabled).
	 * 
	 * @return an unmodifiable view on the offered item that matches the first required item, not <code>null</code> or
	 *         empty
	 */
	public UnmodifiableItemStack getOfferedItem1() {
		return offeredItem1;
	}

	/**
	 * Gets an unmodifiable view on the item offered by the player that matches the second required item of the used
	 * trading recipe. This is not necessarily the item in the second slot.
	 * <p>
	 * The type and stack size equal those of the required item of the trading recipe. The metadata however can differ,
	 * but still be accepted for the trade depending on the item matching rules of the used Minecraft version and the
	 * shopkeeper settings (i.e. with strict item comparisons being disabled).
	 * 
	 * @return an unmodifiable view on the offered item that matches the second required item, can be <code>null</code>
	 */
	public UnmodifiableItemStack getOfferedItem2() {
		return offeredItem2;
	}

	/**
	 * Checks whether this is a trade with two input items.
	 * <p>
	 * This is a shortcut for checking if {@link #getOfferedItem2()} is not <code>null</code>.
	 * 
	 * @return <code>true</code> if this is a trade with two input items
	 */
	public boolean hasOfferedItem2() {
		return offeredItem2 != null;
	}

	/**
	 * Whether the offered items are placed in reverse or normal order inside the trading slots of the merchant
	 * inventory.
	 * <p>
	 * Minecraft checks for matching trading recipes for both combinations.
	 * 
	 * @return <code>true</code> if the offered items are placed in reverse order
	 */
	public boolean isItemOrderSwapped() {
		return swappedItemOrder;
	}

	/**
	 * If cancelled the trade will not take place.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the trade will not take place.
	 */
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
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
