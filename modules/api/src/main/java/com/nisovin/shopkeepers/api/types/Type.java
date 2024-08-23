package com.nisovin.shopkeepers.api.types;

import java.util.Collection;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Represents a type of something.
 */
public interface Type {

	/**
	 * Gets the unique identifier of this type.
	 * 
	 * @return the unique identifier, not <code>null</code> or empty
	 */
	public String getIdentifier();

	/**
	 * Gets the aliases of this type.
	 * <p>
	 * The aliases are for example used by {@link #matches(String)}.
	 *
	 * @return the aliases, not <code>null</code>, can be empty
	 */
	public Collection<? extends String> getAliases();

	/**
	 * Gets the display name of this type.
	 * 
	 * @return the display name, not <code>null</code>
	 */
	public default String getDisplayName() {
		return this.getIdentifier();
	}

	/**
	 * Gets the permission that is required for players to access or use this type in some way.
	 * 
	 * @return the permission, or <code>null</code> to indicate that no permission is required
	 */
	public @Nullable String getPermission();

	/**
	 * Checks if the given player has the required permission to access or use this type in some
	 * way.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return <code>true</code> if the player has the required permission
	 * @see #getPermission()
	 */
	public boolean hasPermission(Player player);

	/**
	 * Checks if this type is enabled.
	 * 
	 * @return <code>true</code> if enabled
	 */
	public boolean isEnabled();

	/**
	 * Checks if the given identifier matches this type.
	 * <p>
	 * Typically, this normalizes and compares the given identifier with the identifier of this
	 * type, its aliases, and its display name.
	 * 
	 * @param identifier
	 *            the identifier, not <code>null</code>
	 * @return <code>true</code> if the given identifier matches this type
	 */
	public boolean matches(String identifier);
}
