package com.nisovin.shopkeepers.api.shopkeeper;

import java.time.Instant;

import org.bukkit.ChatColor;

import com.nisovin.shopkeepers.api.internal.ApiInternals;

/**
 * A snapshot of a {@link Shopkeeper}'s dynamic state at a certain point in time.
 * <p>
 * {@link Object#equals(Object)} compares snapshots based on their object identity, not based on
 * their data.
 */
public interface ShopkeeperSnapshot {

	/**
	 * Gets the maximum length of {@link ShopkeeperSnapshot} names.
	 * 
	 * @return the maximum snapshot name length
	 */
	public static int getMaxNameLength() {
		return ApiInternals.getInstance().getShopkeeperSnapshotMaxNameLength();
	}

	/**
	 * Checks if the given {@link ShopkeeperSnapshot} name is valid.
	 * <p>
	 * The tested constraints are not fixed and might change or be extended in the future.
	 * Currently, this performs at least the following checks:
	 * <ul>
	 * <li>The name is not <code>null</code> or empty.
	 * <li>The name's length does not exceed the {@link #getMaxNameLength() name length limit}.
	 * <li>The name does not contain the color code character {@link ChatColor#COLOR_CHAR}
	 * (character '&amp;' is allowed).
	 * </ul>
	 * 
	 * @param name
	 *            the name, can be <code>null</code> or empty
	 * @return <code>true</code> if the name is valid
	 */
	public static boolean isNameValid(String name) {
		return ApiInternals.getInstance().isShopkeeperSnapshotNameValid(name);
	}

	/**
	 * The name of this snapshot.
	 * <p>
	 * The name allows players to more easily identify this snapshot again later.
	 * <p>
	 * The name conforms to the constraints checked by {@link #isNameValid(String)}.
	 * 
	 * @return the name, not <code>null</code> or empty
	 */
	public String getName();

	/**
	 * The timestamp of when this snapshot was taken.
	 * 
	 * @return the timestamp, not <code>null</code>
	 */
	public Instant getTimestamp();
}
