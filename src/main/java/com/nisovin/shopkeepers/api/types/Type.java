package com.nisovin.shopkeepers.api.types;

import org.bukkit.entity.Player;

public interface Type {

	/**
	 * Gets the unique identifier for this type.
	 * 
	 * @return the unique identifier, not <code>null</code> or empty
	 */
	public String getIdentifier();

	/**
	 * Gets the permission that is required for players to access or use this type in some way.
	 * 
	 * @return the permission, or <code>null</code> to indicate that no permission is required
	 */
	public String getPermission();

	/**
	 * Checks if the given player has the required permission to access or use this type in some way.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the player has the required permission
	 */
	public boolean hasPermission(Player player);

	/**
	 * Checks whether this type is enabled.
	 * 
	 * @return <code>true</code> if enabled
	 */
	public boolean isEnabled();

	/**
	 * Checks if the given (possibly inaccurate) identifier matches to this type.
	 * 
	 * @param identifier
	 *            an (possible inaccurate) identifier
	 * @return <code>true</code> if the given identifier is considered to represent this type
	 */
	public boolean matches(String identifier);
}
