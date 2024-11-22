package com.nisovin.shopkeepers.util.bukkit;

import java.util.function.Function;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.PluginManager;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public final class PermissionUtils {

	private static boolean LOG_PERMISSION_CHECKS = true;

	public static <T> T runWithoutPermissionCheckLogging(Supplier<T> action) {
		if (!LOG_PERMISSION_CHECKS) {
			// Already in a scope with disabled logging:
			return action.get();
		}

		try {
			LOG_PERMISSION_CHECKS = false;
			return action.get();
		} finally {
			LOG_PERMISSION_CHECKS = true;
		}
	}

	/**
	 * Performs a permissions check and logs debug information about it.
	 * 
	 * @param permissible
	 *            the permissible, not <code>null</code>
	 * @param permission
	 *            the permission, not <code>null</code> or empty
	 * @return <code>true</code> if the permissible has the specified permission, or
	 *         <code>false</code> otherwise
	 */
	public static boolean hasPermission(Permissible permissible, String permission) {
		Validate.notNull(permissible, "permissible is null");
		Validate.notEmpty(permission, "permission is null or empty");
		boolean hasPermission = permissible.hasPermission(permission);
		if (!hasPermission && (permissible instanceof Player)) {
			if (LOG_PERMISSION_CHECKS) {
				Log.debug(() -> "Player '" + ((Player) permissible).getName()
						+ "' does not have permission '" + permission + "'.");
			}
		}
		return hasPermission;
	}

	/**
	 * {@link PluginManager#addPermission(Permission) Registers} the given permission node, if it is
	 * not already registered.
	 * 
	 * @param permissionNode
	 *            the permission node, not <code>null</code> or empty
	 * @param permissionProvider
	 *            function that is lazily invoked if the permission is not yet registered and then
	 *            provides the new permission to register
	 * @return <code>true</code> if the permission got newly registered, <code>false</code> if it
	 *         was already registered
	 */
	public static boolean registerPermission(
			String permissionNode,
			Function<String, Permission> permissionProvider
	) {
		Validate.notEmpty(permissionNode, "permissionNode is null or empty");
		Validate.notNull(permissionProvider, "permissionProvider is null");

		PluginManager pluginManager = Bukkit.getPluginManager();
		if (pluginManager.getPermission(permissionNode) != null) {
			// The permission is already registered:
			return false;
		}

		Permission permission = Unsafe.assertNonNull(permissionProvider.apply(permissionNode));
		Validate.notNull(permission, "permissionProvider returned a null permission");
		pluginManager.addPermission(permission);
		return true;
	}

	/**
	 * {@link PluginManager#addPermission(Permission) Registers} the given permission node, if it is
	 * not already registered.
	 * <p>
	 * The permission will use {@link PermissionDefault#FALSE} as its default.
	 * 
	 * @param permissionNode
	 *            the permission node, not <code>null</code> or empty
	 * @return <code>true</code> if the permission got newly registered, <code>false</code> if it
	 *         was already registered
	 * @see PermissionUtils#registerPermission(String, Function)
	 */
	public static boolean registerPermission(String permissionNode) {
		return registerPermission(
				permissionNode,
				node -> new Permission(node, PermissionDefault.FALSE)
		);
	}

	private PermissionUtils() {
	}
}
