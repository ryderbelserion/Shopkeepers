package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shoptypes.PlayerShopkeeper;

/**
 * This event is called whenever a player is about to hire a player shopkeeper.
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
public class PlayerShopkeeperHiredEvent extends Event implements Cancellable {

	private final Player player;
	private final PlayerShopkeeper shopkeeper;
	private final ItemStack[] newPlayerInventoryContents;
	private int maxShops;

	private boolean cancelled;

	public PlayerShopkeeperHiredEvent(Player player, PlayerShopkeeper shopkeeper, ItemStack[] newPlayerInventoryContents, int maxShops) {
		this.player = player;
		this.shopkeeper = shopkeeper;
		this.newPlayerInventoryContents = newPlayerInventoryContents;
		this.maxShops = maxShops;
	}

	/**
	 * Gets the player trying to hire the shop.
	 * 
	 * @return the player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the shokeeper the player is about to hire.
	 * 
	 * @return the shopkeeper
	 */
	public PlayerShopkeeper getShopkeeper() {
		return shopkeeper;
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
	 * Gets the maximum number of shops the player can have.
	 * 
	 * @return player max shops
	 */
	public int getMaxShopsForPlayer() {
		return maxShops;
	}

	/**
	 * Sets the maximum number of shops the creating player can have.
	 * <p>
	 * If the player has more than this number, the shop will not be hired.
	 * 
	 * @param maxShops
	 *            the player's max shops
	 */
	public void setMaxShopsForPlayer(int maxShops) {
		this.maxShops = maxShops;
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
