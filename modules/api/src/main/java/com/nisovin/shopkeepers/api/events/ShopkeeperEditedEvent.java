package com.nisovin.shopkeepers.api.events;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * This event is called whenever a player has finished editing a {@link Shopkeeper} in some way.
 */
public class ShopkeeperEditedEvent extends ShopkeeperEvent {

	private final Player player;

	/**
	 * Creates a new {@link ShopkeeperEditedEvent}.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param player
	 *            the player, not <code>null</code>
	 */
	public ShopkeeperEditedEvent(Shopkeeper shopkeeper, Player player) {
		super(shopkeeper);
		Preconditions.checkNotNull(player, "player is null");
		this.player = player;
	}

	/**
	 * The editing player.
	 * 
	 * @return the editing player
	 */
	public Player getPlayer() {
		return player;
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
