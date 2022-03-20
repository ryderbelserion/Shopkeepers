package com.nisovin.shopkeepers.tradelog.data;

import java.util.UUID;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * An immutable snapshot of the general information about a player.
 * <p>
 * This information is not necessarily up-to-date, but represents the player's information at a
 * certain point in time (e.g. the name might not match the player's current name).
 */
public class PlayerRecord {

	/**
	 * Creates a {@link PlayerRecord} for the given player.
	 * 
	 * @param player
	 *            the player
	 * @return the player record
	 */
	public static PlayerRecord of(Player player) {
		Validate.notNull(player, "player is null");
		return of(player.getUniqueId(), Unsafe.assertNonNull(player.getName()));
	}

	/**
	 * Creates a {@link PlayerRecord} with the specified data.
	 * 
	 * @param playerUniqueId
	 *            the player's unique id, not <code>null</code>
	 * @param playerName
	 *            the player's name, not <code>null</code> or empty
	 * @return the player record
	 */
	public static PlayerRecord of(UUID playerUniqueId, String playerName) {
		return new PlayerRecord(playerUniqueId, playerName);
	}

	private final UUID uniqueId; // Not null
	private final String name; // Not null or empty

	/**
	 * Creates a new {@link PlayerRecord}.
	 * 
	 * @param playerUniqueId
	 *            the player's unique id, not <code>null</code>
	 * @param playerName
	 *            the player's name, not <code>null</code> or empty
	 */
	private PlayerRecord(UUID playerUniqueId, String playerName) {
		Validate.notNull(playerUniqueId, "playerUniqueId is null");
		Validate.notEmpty(playerName, "playerName is null or empty");
		this.uniqueId = playerUniqueId;
		this.name = playerName;

	}

	/**
	 * Gets the player's unique id.
	 * 
	 * @return the player's unique id
	 */
	public UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * Gets the player's name.
	 * 
	 * @return the player's name
	 */
	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PlayerRecord [uniqueId=");
		builder.append(uniqueId);
		builder.append(", name=");
		builder.append(name);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + name.hashCode();
		result = prime * result + uniqueId.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof PlayerRecord)) return false;
		PlayerRecord other = (PlayerRecord) obj;
		if (!name.equals(other.name)) return false;
		if (!uniqueId.equals(other.uniqueId)) return false;
		return true;
	}
}
