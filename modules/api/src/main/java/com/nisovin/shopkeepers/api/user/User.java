package com.nisovin.shopkeepers.api.user;

import java.util.UUID;

import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

/**
 * Represents a player that the Shopkeepers plugin knows about.
 */
public interface User {

	/**
	 * Gets the player's unique id.
	 * 
	 * @return the player's unique id, not <code>null</code>
	 */
	public UUID getUniqueId();

	/**
	 * Gets the player's last known name.
	 * <p>
	 * If the player is currently online, this matches the player's current name.
	 * 
	 * @return the player's last known name, not <code>null</code> or empty
	 */
	public String getName();

	/**
	 * Gets the player's display name.
	 * <p>
	 * If the player is currently offline, the display name may not be available. In this case, this returns the
	 * player's {@link #getName() last known name} as fallback.
	 * 
	 * @return the display name or last known name, not <code>null</code> or empty
	 */
	public String getDisplayName();

	/**
	 * Checks whether the player is currently online.
	 * 
	 * @return <code>true</code> if the player is online
	 */
	public boolean isOnline();

	/**
	 * Gets the {@link Player} if they are currently online.
	 * 
	 * @return the player, or <code>null</code> if the player is not online
	 */
	public Player getPlayer();

	/**
	 * Gets the {@link OfflinePlayer}.
	 * 
	 * @return the offline player, never <code>null</code> (even if the player does not exist or has never played on the
	 *         server before)
	 */
	public OfflinePlayer getOfflinePlayer();
}
