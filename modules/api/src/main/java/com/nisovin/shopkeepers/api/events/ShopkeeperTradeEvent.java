package com.nisovin.shopkeepers.api.events;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.trading.TradeEffect;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * This event is called whenever a player is about to trade with a shopkeeper.
 * <p>
 * This event can be used to cancel the trade, or alter the items that the player or the shopkeeper
 * will receive once the trade is actually applied. When the result item or any of the "received"
 * items are modified, applying the trade will still remove the trading recipe's original items from
 * the shop's container and reduce the original items offered by the player, but the shopkeeper and
 * player will instead receive the modified items specified by this event.
 * <p>
 * Setting the result or any of the "received" items to <code>null</code> or an empty item stack
 * will cause the trade to be applied like normal, but the trading player or shopkeeper will not
 * receive the cleared items. This can for example be used to implement items that have a different
 * effect when being traded, such as a command being executed, or the player or shop owner receiving
 * some other kind of reward.
 * <p>
 * Depending on the inventory action, a single inventory click of a player can trigger several
 * successive trades. These trades might even use different trading recipes. This event is called
 * for each of these trades. Canceling a trade will also cancel all successive trades that might
 * otherwise have been triggered by the same click event.
 * <p>
 * This event cannot be used to determine whether the trade will actually take place. For example,
 * the shopkeeper might abort the trade if the player does not have the necessary inventory space
 * available to receive the result items. Use {@link #getTradeEffects()} to register effects that
 * are invoked when the trade is either aborted or applied. Use the
 * {@link ShopkeeperTradeCompletedEvent} for anything that must only happen once the trade has been
 * successfully applied, such as logging.
 * <p>
 * DO NOT modify the corresponding {@link InventoryClickEvent}, any affected inventories (player,
 * merchant, shop container, etc.), or any other state that might be affected by the trade during
 * the handling of this event!
 */
public class ShopkeeperTradeEvent extends ShopkeeperEvent implements Cancellable {

	private final Player player;
	private final InventoryClickEvent clickEvent;
	private final TradingRecipe tradingRecipe;
	private final UnmodifiableItemStack offeredItem1;
	private final @Nullable UnmodifiableItemStack offeredItem2;
	private final boolean swappedItemOrder;

	private @Nullable UnmodifiableItemStack receivedItem1;
	private @Nullable UnmodifiableItemStack receivedItem2;
	private @Nullable UnmodifiableItemStack resultItem;
	private boolean receivedItem1Altered = false;
	private boolean receivedItem2Altered = false;
	private boolean resultItemAltered = false;

	private List<TradeEffect> tradeEffects = new ArrayList<>();
	private boolean cancelled = false;

	/**
	 * Creates a new {@link ShopkeeperTradeEvent}.
	 * <p>
	 * The offered items are expected to be immutable and their stack sizes match the trading recipe
	 * items.
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
	 *            the offered item that matches the first required item of the trading recipe, not
	 *            <code>null</code> or empty
	 * @param offeredItem2
	 *            the offered item that matches the second required item of the trading recipe, can
	 *            be <code>null</code>
	 * @param swappedItemOrder
	 *            <code>true</code> if the player provided the offered items in reverse order
	 */
	public ShopkeeperTradeEvent(
			Shopkeeper shopkeeper,
			Player player,
			InventoryClickEvent clickEvent,
			TradingRecipe tradingRecipe,
			UnmodifiableItemStack offeredItem1,
			@Nullable UnmodifiableItemStack offeredItem2,
			boolean swappedItemOrder
	) {
		super(shopkeeper);
		Preconditions.checkNotNull(player, "player is null");
		Preconditions.checkNotNull(clickEvent, "clickEvent is null");
		Preconditions.checkNotNull(tradingRecipe, "tradingRecipe is null");
		Preconditions.checkNotNull(offeredItem1, "offeredItem1 is null");
		this.player = player;
		this.clickEvent = clickEvent;
		this.tradingRecipe = tradingRecipe;
		this.offeredItem1 = offeredItem1;
		this.offeredItem2 = offeredItem2; // Can be null
		this.swappedItemOrder = swappedItemOrder;

		this.receivedItem1 = offeredItem1;
		this.receivedItem2 = offeredItem2;
		this.resultItem = tradingRecipe.getResultItem();
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
	 * Gets an unmodifiable view on the item offered by the player that matches the first required
	 * item of the used trading recipe. This is not necessarily the item in the first slot.
	 * <p>
	 * The type and stack size equal those of the required item of the trading recipe. The metadata
	 * however can differ, but still be accepted for the trade depending on the item matching rules
	 * of the used Minecraft version and the shopkeeper settings (i.e. with strict item comparisons
	 * being disabled).
	 * <p>
	 * By default, this also matches the first item that the shopkeeper will receive. But the
	 * received item can be altered via {@link #setReceivedItem1(UnmodifiableItemStack)}.
	 * 
	 * @return an unmodifiable view on the offered item that matches the first required item, not
	 *         <code>null</code> or empty
	 */
	public UnmodifiableItemStack getOfferedItem1() {
		return offeredItem1;
	}

	/**
	 * Gets an unmodifiable view on the item offered by the player that matches the second required
	 * item of the used trading recipe. This is not necessarily the item in the second slot.
	 * <p>
	 * The type and stack size equal those of the required item of the trading recipe. The metadata
	 * however can differ, but still be accepted for the trade depending on the item matching rules
	 * of the used Minecraft version and the shopkeeper settings (i.e. with strict item comparisons
	 * being disabled).
	 * <p>
	 * By default, this also matches the second item that the shopkeeper will receive. But the
	 * received item can be altered via {@link #setReceivedItem2(UnmodifiableItemStack)}.
	 * 
	 * @return an unmodifiable view on the offered item that matches the second required item, can
	 *         be <code>null</code>
	 */
	public @Nullable UnmodifiableItemStack getOfferedItem2() {
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
	 * Whether the offered items are placed in reverse or normal order inside the trading slots of
	 * the merchant inventory.
	 * <p>
	 * Minecraft checks for matching trading recipes for both combinations.
	 * 
	 * @return <code>true</code> if the offered items are placed in reverse order
	 */
	public boolean isItemOrderSwapped() {
		return swappedItemOrder;
	}

	/**
	 * Gets an unmodifiable view on the first item that the shopkeeper will receive, or
	 * <code>null</code> or empty if the shopkeeper will not receive any item.
	 * <p>
	 * By default, this equals the {@link #getOfferedItem1() first offered item}, but the item can
	 * be altered via {@link #setReceivedItem1(UnmodifiableItemStack)}.
	 * 
	 * @return the first item that the shopkeeper will receive, or <code>null</code> or empty
	 */
	public @Nullable UnmodifiableItemStack getReceivedItem1() {
		return receivedItem1;
	}

	/**
	 * Sets the first item that the shopkeeper will receive.
	 * <p>
	 * Some shopkeepers might ignore modifications to the received items in certain cases. For
	 * example, when adding or removing currency items to or from a shop's container, shopkeepers
	 * will usually prefer using the currency items configured inside the config and the currency
	 * amount specified by the trading offer.
	 * 
	 * @param itemStack
	 *            the item that the shopkeeper will receive, or <code>null</code> or empty for the
	 *            shopkeeper to not receive any item
	 */
	public void setReceivedItem1(@Nullable UnmodifiableItemStack itemStack) {
		this.receivedItem1Altered = true;
		this.receivedItem1 = itemStack;
	}

	/**
	 * Whether the {@link #getReceivedItem1() first received item} was altered.
	 * <p>
	 * For performance reasons, we don't compare the new item with the original item, but only
	 * detect whether {@link #setReceivedItem1(UnmodifiableItemStack)} has been called.
	 * 
	 * @return <code>true</code> if the first received item was altered.
	 */
	public boolean isReceivedItem1Altered() {
		return receivedItem1Altered;
	}

	/**
	 * Gets an unmodifiable view on the second item that the shopkeeper will receive, or
	 * <code>null</code> or empty if the shopkeeper will not receive any item.
	 * <p>
	 * By default, this equals the {@link #getOfferedItem2() second offered item}, but the item can
	 * be altered via {@link #setReceivedItem2(UnmodifiableItemStack)}.
	 * 
	 * @return the second item that the shopkeeper will receive, or <code>null</code> or empty
	 */
	public @Nullable UnmodifiableItemStack getReceivedItem2() {
		return receivedItem2;
	}

	/**
	 * Sets the second item that the shopkeeper will receive.
	 * <p>
	 * Some shopkeepers might ignore modifications to the received items in certain cases. For
	 * example, when adding or removing currency items to or from a shop's container, shopkeepers
	 * will usually prefer using the currency items configured inside the config and the currency
	 * amount specified by the trading offer.
	 * 
	 * @param itemStack
	 *            the item that the shopkeeper will receive, or <code>null</code> or empty for the
	 *            shopkeeper to not receive any item
	 */
	public void setReceivedItem2(@Nullable UnmodifiableItemStack itemStack) {
		this.receivedItem2Altered = true;
		this.receivedItem2 = itemStack;
	}

	/**
	 * Whether the {@link #getReceivedItem2() second received item} was altered.
	 * <p>
	 * For performance reasons, we don't compare the new item with the original item, but only
	 * detect whether {@link #setReceivedItem2(UnmodifiableItemStack)} has been called.
	 * 
	 * @return <code>true</code> if the second received item was altered.
	 */
	public boolean isReceivedItem2Altered() {
		return receivedItem2Altered;
	}

	/**
	 * Gets an unmodifiable view on the result item that the player will receive, or
	 * <code>null</code> or empty if the player will not receive any item.
	 * <p>
	 * By default, this equals the {@link TradingRecipe#getResultItem()}, but the item can be
	 * altered via {@link #setResultItem(UnmodifiableItemStack)}.
	 * 
	 * @return the result item that the player will receive, or <code>null</code> or empty
	 */
	public @Nullable UnmodifiableItemStack getResultItem() {
		return resultItem;
	}

	/**
	 * Sets the result item that the player will receive.
	 * <p>
	 * Some shopkeepers might ignore modifications to the result item in certain cases. For example,
	 * when removing items from a shop's container, shopkeepers will usually prefer checking for and
	 * removing the trading recipe's original result item.
	 * 
	 * @param itemStack
	 *            the result item, or <code>null</code> or empty for the player to not receive any
	 *            item
	 */
	public void setResultItem(@Nullable UnmodifiableItemStack itemStack) {
		this.resultItemAltered = true;
		this.resultItem = itemStack;
	}

	/**
	 * Whether the {@link #getResultItem() result item} was altered.
	 * <p>
	 * For performance reasons, we don't compare the new item with the original item, but only
	 * detect whether {@link #setResultItem(UnmodifiableItemStack)} has been called.
	 * 
	 * @return <code>true</code> if the result item was altered.
	 */
	public boolean isResultItemAltered() {
		return resultItemAltered;
	}

	/**
	 * Gets a modifiable list of {@link TradeEffect}s that will be invoked once the trade is either
	 * {@link TradeEffect#onTradeAborted(ShopkeeperTradeEvent) aborted} or
	 * {@link TradeEffect#onTradeApplied(ShopkeeperTradeEvent) applied}.
	 * <p>
	 * This can for example be used to add custom trade effects, or for the implementation of some
	 * of the built-in default trade effects. However, it is very likely that this list does not
	 * represent all effects of the trade. For example, various components, including the shopkeeper
	 * itself, may apply their own trade effects without those being represented in this list.
	 * Consequently, clearing this list may not necessarily disable all effects of the trade.
	 * 
	 * @return the modifiable list of trade effects, not <code>null</code>
	 */
	public List<TradeEffect> getTradeEffects() {
		return tradeEffects;
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
