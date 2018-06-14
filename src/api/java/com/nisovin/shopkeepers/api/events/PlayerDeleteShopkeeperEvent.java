package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * This event is called whenever a player is about to delete a {@link Shopkeeper}.
 * <p>
 * If you want to react to all shopkeeper deletions, take a look at {@link ShopkeeperRemoveEvent}.
 */
public class PlayerDeleteShopkeeperEvent extends ShopkeeperEvent implements Cancellable {

	private final Player player;
	private boolean cancelled = false;

	public PlayerDeleteShopkeeperEvent(Shopkeeper shopkeeper, Player player) {
		super(shopkeeper);
		this.player = player;
	}

	/**
	 * The player who is deleting the {@link Shopkeeper}.
	 * 
	 * @return the deleting player
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * If cancelled the shopkeeper will not be deleted.
	 */
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	/**
	 * If cancelled the shopkeeper will not be deleted.
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
