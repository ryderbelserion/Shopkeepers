package com.nisovin.shopkeepers.playershops;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A permission node associated with a maximum player shops limit.
 * <p>
 * {@link MaxShopsPermission} is {@link Comparable} and ordered based on its {@link #getMaxShops()
 * maximum player shops limit}.
 */
public class MaxShopsPermission implements Comparable<MaxShopsPermission> {

	public static final MaxShopsPermission UNLIMITED = new MaxShopsPermission(
			Integer.MAX_VALUE,
			ShopkeepersPlugin.MAXSHOPS_UNLIMITED_PERMISSION
	);
	private static final String PERMISSION_PREFIX = "shopkeeper.maxshops.";

	/**
	 * Parses the {@link MaxShopsPermission} from the given input String.
	 * <p>
	 * The input String is expected to be the String representation of the maximum shops limit.
	 * 
	 * @param maxShopsPermissionOption
	 *            the input String
	 * @return the parsed {@link MaxShopsPermission}, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the {@link MaxShopsPermission} cannot be parsed
	 */
	public static MaxShopsPermission parse(String maxShopsPermissionOption) {
		Integer maxShops = ConversionUtils.parseInt(maxShopsPermissionOption);
		if (maxShops == null || maxShops <= 0) {
			throw new IllegalArgumentException("Invalid max shops permission option: "
					+ maxShopsPermissionOption);
		}
		// Using the String representation of the parsed integer instead of the original String
		// ensures that the result is always the expected integer representation and not affected by
		// any lenient parsing rules (such as for example the optionally allowed '+' sign character
		// for positive integers):
		String permission = PERMISSION_PREFIX + maxShops;
		return new MaxShopsPermission(maxShops, permission);
	}

	/**
	 * Parses a list of {@link MaxShopsPermission} from the given input String.
	 * <p>
	 * The input String is expected to be the String representation of the maximum shops limits
	 * separated by commas.
	 * 
	 * @param maxShopsPermissionOptionsList
	 *            the input String, not <code>null</code>
	 * @param invalidPermissionOptionCallback
	 *            this callback is invoked for invalid maximum shops permission options, not
	 *            <code>null</code>
	 * @return the parsed list of {@link MaxShopsPermission}
	 */
	public static List<MaxShopsPermission> parseList(
			String maxShopsPermissionOptionsList,
			Consumer<? super String> invalidPermissionOptionCallback
	) {
		Validate.notNull(maxShopsPermissionOptionsList, "maxShopsPermissionOptionsList is null");
		Validate.notNull(invalidPermissionOptionCallback, "invalidPermissionOptionCallback is null");
		@NonNull String[] permissionOptions = StringUtils.removeWhitespace(maxShopsPermissionOptionsList).split(",");
		List<MaxShopsPermission> maxShopsPermissions = new ArrayList<>(permissionOptions.length);
		for (String permissionOption : permissionOptions) {
			MaxShopsPermission maxShopsPermission;
			try {
				maxShopsPermission = parse(permissionOption);
			} catch (IllegalArgumentException e) {
				invalidPermissionOptionCallback.accept(permissionOption);
				continue;
			}
			assert maxShopsPermission != null;
			maxShopsPermissions.add(maxShopsPermission);
		}
		return maxShopsPermissions;
	}

	// ----

	// Integer.MAX_VALUE indicates no limit.
	private final int maxShops;
	private final String permission;

	/**
	 * Creates a new {@link MaxShopsPermission}.
	 * 
	 * @param maxShops
	 *            the maximum shops limit, has to be positive
	 * @param permission
	 *            the permission node, not <code>null</code> or empty
	 */
	public MaxShopsPermission(int maxShops, String permission) {
		Validate.isTrue(maxShops > 0, "maxShops has to be positive");
		Validate.notEmpty(permission, "permission is null or empty");
		this.maxShops = maxShops;
		this.permission = permission;
	}

	/**
	 * Gets the maximum player shops limit associated with this permission.
	 * 
	 * @return the maximum player shops limit, a positive number, can be {@link Integer#MAX_VALUE}
	 *         if there is no limit
	 */
	public int getMaxShops() {
		return maxShops;
	}

	/**
	 * Checks if the {@link #getMaxShops() maximum shops limit} of this {@link MaxShopsPermission}
	 * is unlimited (i.e. is {@link Integer#MAX_VALUE}).
	 * 
	 * @return <code>true</code> if the maximum shops limit is unlimited
	 */
	public boolean isUnlimited() {
		return (maxShops == Integer.MAX_VALUE);
	}

	/**
	 * Gets the permission node.
	 * 
	 * @return the permission node, not <code>null</code> or empty
	 */
	public String getPermission() {
		return permission;
	}

	/**
	 * {@link PluginManager#addPermission(Permission) Registers} the permission node of this maximum
	 * shops permission, if it is not already registered.
	 */
	public void registerPermission() {
		PermissionUtils.registerPermission(permission, node -> createPermission());
	}

	private Permission createPermission() {
		String description;
		if (this.isUnlimited()) {
			description = "Allows ownership of an unlimited number of shopkeepers";
		} else {
			description = "Allows ownership of up to " + maxShops + " shopkeeper(s)";
		}
		return new Permission(permission, description, PermissionDefault.FALSE);
	}

	/**
	 * Checks if the given {@link Permissible} has this permission.
	 * 
	 * @param permissible
	 *            the permissible, not <code>null</code>
	 * @return <code>true</code> if the permissible has the permission, <code>false</code> otherwise
	 * @see PermissionUtils#hasPermission(Permissible, String)
	 */
	public boolean hasPermission(Permissible permissible) {
		return PermissionUtils.hasPermission(permissible, permission);
	}

	@Override
	public int compareTo(MaxShopsPermission other) {
		return Integer.compare(maxShops, other.maxShops);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MaxShopsPermission [maxShops=");
		builder.append(maxShops);
		builder.append(", permission=");
		builder.append(permission);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + maxShops;
		result = prime * result + permission.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof MaxShopsPermission)) return false;
		MaxShopsPermission other = (MaxShopsPermission) obj;
		if (maxShops != other.maxShops) return false;
		if (!permission.equals(other.permission)) return false;
		return true;
	}
}
