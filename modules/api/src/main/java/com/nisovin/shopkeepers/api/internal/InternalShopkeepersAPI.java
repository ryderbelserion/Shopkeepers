package com.nisovin.shopkeepers.api.internal;

import org.apache.commons.lang.Validate;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;

/**
 * Internal access point for the Shopkeepers plugin to {@link #enable(InternalShopkeepersPlugin) enable} and
 * {@link #disable() disable} the API.
 * <p>
 * Only for internal use by the Shopkeepers API and the Shopkeepers plugin implementing the API!
 */
public class InternalShopkeepersAPI {

	private static InternalShopkeepersPlugin plugin = null;

	/**
	 * Enables the Shopkeepers API.
	 * 
	 * @param plugin
	 *            the plugin instance, not <code>null</code>
	 */
	public static void enable(InternalShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		if (InternalShopkeepersAPI.plugin != null) {
			throw new IllegalStateException("API is already enabled!");
		}
		InternalShopkeepersAPI.plugin = plugin;
	}

	/**
	 * Disables the Shopkeepers API.
	 */
	public static void disable() {
		if (plugin == null) {
			throw new IllegalStateException("API is already disabled!");
		}
		plugin = null;
	}

	/**
	 * Checks whether the API has already been enabled.
	 * 
	 * @return <code>true</code> if enabled
	 */
	public static boolean isEnabled() {
		return (plugin != null);
	}

	/**
	 * Gets the {@link ShopkeepersPlugin} instance.
	 * 
	 * @return the plugin instance, not <code>null</code>
	 * @throws IllegalStateException
	 *             if the API is not enabled currently, eg. because the plugin is not enabled currently
	 */
	public static InternalShopkeepersPlugin getPlugin() {
		if (plugin == null) {
			throw new IllegalStateException("API is not enabled!");
		}
		return plugin;
	}

	private InternalShopkeepersAPI() {
	}
}
