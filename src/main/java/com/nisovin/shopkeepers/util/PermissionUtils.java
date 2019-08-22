package com.nisovin.shopkeepers.util;

import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;

public class PermissionUtils {

	private PermissionUtils() {
	}

	/**
	 * Performs a permissions check and logs debug information about it.
	 * 
	 * @param permissible
	 * @param permission
	 * @return
	 */
	public static boolean hasPermission(Permissible permissible, String permission) {
		assert permissible != null;
		boolean hasPerm = permissible.hasPermission(permission);
		if (!hasPerm && (permissible instanceof Player)) {
			Log.debug("Player '" + ((Player) permissible).getName() + "' does not have permission '" + permission + "'.");
		}
		return hasPerm;
	}
}
