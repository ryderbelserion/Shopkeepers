package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;

/**
 * This event is called whenever a player is about to hire a {@link PlayerShopkeeper}.
 * <p>
 * This event is called after the inventory of the hiring player has been checked to contain the required hire cost
 * items and the new inventory contents resulting from hiring have been calculated.<br>
 * The new inventory contents only get applied to the player, if the hiring is successful. It will then replace the
 * player's inventory at that point. So be aware that any inventory changes made to the player's inventory during
 * handling of this event might get overwritten at that point.
 * <p>
 * This event is called before the max shops limit is checked for the player.<br>
 * If this event is cancelled or the player has reached the max shops limit, the shop will not be hired.
 */
public class PlayerShopkeeperHireEvent extends ShopkeeperEvent implements Cancellable {

	private final Player player;
	private final ItemStack[] newPlayerInventoryContents;
	private int maxShopsLimit;
	private boolean cancelled = false;

	public PlayerShopkeeperHireEvent(PlayerShopkeeper shopkeeper, Player player, ItemStack[] newPlayerInventoryContents, int maxShopsLimit) {
		super(shopkeeper);
		this.player = player;
		this.newPlayerInventoryContents = newPlayerInventoryContents;
		this.maxShopsLimit = maxShopsLimit;
	}

	@Override
	public PlayerShopkeeper getShopkeeper() {
		return (PlayerShopkeeper) super.getShopkeeper();
	}

	/**
	 * Gets the player who is trying to hire the shopkeeper.
	 * 
	 * @return the hiring player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the player's new inventory contents that get applied to the player if the hiring is successful.
	 * <p>
	 * Note that changes to some items contained in this array <i>might</i> directly affect the corresponding items in
	 * the player's current inventory. So when changing items in this array, consider replacing them with cloned items
	 * first.
	 * 
	 * @return the player's new inventory contents
	 */
	public ItemStack[] getNewPlayerInventoryContents() {
		return newPlayerInventoryContents;
	}

	/**
	 * Gets the maximum number of shops the hiring player can have.
	 * <p>
	 * {@link Integer#MAX_VALUE} indicates no limit.
	 * 
	 * @return the hiring player's max shops limit
	 */
	public int getMaxShopsLimit() {
		return maxShopsLimit;
	}

	/**
	 * Sets the maximum number of shops the hiring player can have.
	 * <p>
	 * The new max shops limit only affects this specific shopkeeper hire. If the player already has more shops than
	 * this, the shop will not be hired.
	 * <p>
	 * {@link Integer#MAX_VALUE} indicates no limit.
	 * 
	 * @param maxShopsLimit
	 *            the hiring player's max shops limit to use for this hire
	 */
	public void setMaxShopsLimit(int maxShopsLimit) {
		if (maxShopsLimit < 0) {
			throw new IllegalArgumentException("maxShopsLimit cannot be negative!");
		}
		this.maxShopsLimit = maxShopsLimit;
	}

	/**
	 * If cancelled the hiring will not take place.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the hiring will not take place.
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

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
