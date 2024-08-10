package com.nisovin.shopkeepers.compat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public final class NMSManager {

	private static final Map<String, CompatVersion> SUPPORTED_MAPPINGS_VERSIONS = new LinkedHashMap<>();

	private static void register(CompatVersion version) {
		SUPPORTED_MAPPINGS_VERSIONS.put(version.getMappingsVersion(), version);
	}

	// We have to update and rebuild our compat code whenever the mappings changed.
	// Changes to the mappings version do not necessarily align with a bump of the CraftBukkit
	// version. To reduce the number of CraftBukkit versions to depend on and build, we only support
	// the latest mappings version for every CraftBukkit version.
	// Although they look similar, our compat versions do not necessarily match CraftBukkit's
	// 'Minecraft Version': Our revision number (behind the 'R') is incremented for every new compat
	// module for a specific major Minecraft version, which usually aligns with mappings updates for
	// new minor Minecraft updates, whereas CraftBukkit may increment its 'Minecraft Version' less
	// frequently.
	static {
		// Registered in the order from latest to oldest.
		register(new CompatVersion("1_21_R1", "1.21", "229d7afc75b70a6c388337687ac4da1f"));
		// Note: MC 1.20.6 completely replaced 1.20.5. We only support 1.20.6.
		register(new CompatVersion("1_20_R5", "1.20.6", "ee13f98a43b9c5abffdcc0bb24154460"));
	}

	public static @Nullable CompatVersion getCompatVersion(String compatVersion) {
		// If there are multiple entries for the same compat version, we return the latest one.
		for (CompatVersion version : SUPPORTED_MAPPINGS_VERSIONS.values()) {
			if (version.getCompatVersion().equals(compatVersion)) {
				return version;
			}
		}
		return null;
	}

	// ----

	private static @Nullable NMSCallProvider provider;

	public static boolean hasProvider() {
		return (provider != null);
	}

	public static NMSCallProvider getProvider() {
		return Validate.State.notNull(provider, "NMS provider is not set up!");
	}

	// Returns true if the NMS provider (or fallback handler) has been successfully set up.
	public static boolean load(Plugin plugin) {
		String mappingsVersion = ServerUtils.getMappingsVersion();
		CompatVersion compatVersion = SUPPORTED_MAPPINGS_VERSIONS.get(mappingsVersion);
		if (compatVersion != null) {
			String compatVersionString = compatVersion.getCompatVersion();
			try {
				Class<?> clazz = Class.forName(
						"com.nisovin.shopkeepers.compat.v" + compatVersionString + ".NMSHandler"
				);
				if (NMSCallProvider.class.isAssignableFrom(clazz)) {
					NMSManager.provider = (NMSCallProvider) clazz.getConstructor().newInstance();
					return true; // Success
				} else {
					// Unexpected: NMSHandler does not implement NMSCallProvider. Continue with
					// fallback.
				}
			} catch (Exception e) {
				// Something went wrong. Continue with fallback.
			}
		}

		// Incompatible server version detected:
		Log.warning("Incompatible server version: " + Bukkit.getBukkitVersion() + " (mappings: "
				+ mappingsVersion + ")");
		Log.warning("Shopkeepers is trying to run in 'compatibility mode'.");
		Log.info("Check for updates at: " + plugin.getDescription().getWebsite());

		try {
			NMSManager.provider = new FailedHandler();
			return true; // Success
		} catch (Exception e) {
			Log.severe("Failed to enable 'compatibility mode'!", e);
		}
		return false;
	}
}
