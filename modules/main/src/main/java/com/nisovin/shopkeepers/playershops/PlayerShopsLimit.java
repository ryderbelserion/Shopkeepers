package com.nisovin.shopkeepers.playershops;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.util.java.Validate;

public class PlayerShopsLimit {

	public PlayerShopsLimit() {
	}

	public void onEnable() {
		registerMaxShopsPermissions();
	}

	public void onDisable() {
	}

	/**
	 * Updates the {@link DerivedSettings#maxShopsPermissions} setting.
	 * <p>
	 * This is called on configuration changes.
	 * 
	 * @param invalidPermissionOptionCallback
	 *            this callback is invoked for invalid maximum shops permission options, not
	 *            <code>null</code>
	 */
	public static void updateMaxShopsPermissions(
			Consumer<? super String> invalidPermissionOptionCallback
	) {
		Validate.notNull(invalidPermissionOptionCallback,
				"invalidPermissionOptionCallback is null");
		String maxShopsPermissionOptions = Settings.maxShopsPermOptions;
		List<MaxShopsPermission> maxShopsPermissions = DerivedSettings.maxShopsPermissions;

		// Clear the list of previous max shops permissions:
		maxShopsPermissions.clear();

		// Add the permission for an unlimited number of shops:
		maxShopsPermissions.add(MaxShopsPermission.UNLIMITED);

		// Add the parsed max shops permissions:
		maxShopsPermissions.addAll(MaxShopsPermission.parseList(
				maxShopsPermissionOptions,
				invalidPermissionOptionCallback
		));

		// Sort the permissions in descending order:
		maxShopsPermissions.sort(Unsafe.assertNonNull(Collections.reverseOrder()));
	}

	/**
	 * Registers the maximum shops permissions, if they are not registered yet.
	 */
	private static void registerMaxShopsPermissions() {
		// Note: These permissions are registered only once, and then never unregistered again until
		// the server is fully reloaded or restarted. This is analogous to how the declared
		// permissions of plugins are registered only once when the plugins are loaded, and not
		// every time a plugin is disabled and enabled again.
		// Also, Bukkit doesn't even properly support dynamically unregistering permissions: For
		// example, there is no way to update the cached default permissions via the API after
		// having unregistered a permission (however, this isn't really required for our permissions
		// anyway). And we would have to manually update all players and other Permissibles that
		// might have these permissions attached to them.
		// However, these permissions are pretty lightweight, so there is no harm caused by keeping
		// them registered until the server restarts, even if the list of max shops permissions has
		// changed after a config reload.
		DerivedSettings.maxShopsPermissions.forEach(MaxShopsPermission::registerPermission);
	}

	/**
	 * Gets the player's maximum shops limit.
	 * <p>
	 * This is calculated based on the configured default maximum shops limit, and the player's
	 * specific maximum shops permissions.
	 * 
	 * @param player
	 *            the player, not <code>null</code>
	 * @return the player's maximum shops limit, or {@link Integer#MAX_VALUE} if there is no limit
	 */
	public static int getMaxShopsLimit(Player player) {
		if (Settings.maxShopsPerPlayer == -1) {
			return Integer.MAX_VALUE; // No limit by default
		}
		int maxShops = Settings.maxShopsPerPlayer; // Default
		for (MaxShopsPermission maxShopsPermission : DerivedSettings.maxShopsPermissions) {
			// Note: The max shops permissions are sorted in descending order.
			int permissionMaxShops = maxShopsPermission.getMaxShops();
			if (permissionMaxShops <= maxShops) {
				break;
			}
			if (maxShopsPermission.hasPermission(player)) {
				maxShops = permissionMaxShops;
				break;
			}
		}
		return maxShops;
	}
}
