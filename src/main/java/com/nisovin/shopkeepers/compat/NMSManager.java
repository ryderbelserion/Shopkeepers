package com.nisovin.shopkeepers.compat;

import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public final class NMSManager {

	private static NMSCallProvider provider;

	public static NMSCallProvider getProvider() {
		return NMSManager.provider;
	}

	public static void load(Plugin plugin) {
		String cbVersion = ServerUtils.getCraftBukkitVersion();
		try {
			Class<?> clazz = Class.forName("com.nisovin.shopkeepers.compat." + cbVersion + ".NMSHandler");
			if (NMSCallProvider.class.isAssignableFrom(clazz)) {
				NMSManager.provider = (NMSCallProvider) clazz.getConstructor().newInstance();
			} else {
				throw new Exception("Nope");
			}
		} catch (Exception e) {
			Log.warning("Potentially incompatible server version: " + cbVersion);
			Log.warning("Shopkeepers is trying to run in 'compatibility mode'.");
			Log.info("Check for updates at: " + plugin.getDescription().getWebsite());

			try {
				NMSManager.provider = new FailedHandler();
			} catch (Exception e_u) {
				// Uncomment for debugging:
				// e_u.printStackTrace();
			}
		}
	}
}
