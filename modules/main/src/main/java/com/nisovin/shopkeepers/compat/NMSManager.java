package com.nisovin.shopkeepers.compat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.compat.api.NMSCallProvider;
import com.nisovin.shopkeepers.util.bukkit.ServerUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public final class NMSManager {

	private static final Map<String, CompatVersion> SUPPORTED_MAPPINGS_VERSIONS = new LinkedHashMap<>();

	private static void register(CompatVersion version) {
		SUPPORTED_MAPPINGS_VERSIONS.put(version.getMappingsVersion(), version);
	}

	// Note: Although they look similar, our compat versions do not necessarily match CraftBukkit's 'Minecraft Version'.
	// Our revision number (behind the 'R') is incremented with every mappings version change, whereas CraftBukkit may
	// increment it less frequently.
	// TODO Since 1.17, we have to update and rebuild our compat code whenever the mappings are changed. However, Spigot
	// does not necessarily align mappings changes with CraftBukkit version bumps. The BuildTools also does not allow to
	// build specific commits. We therefore cannot build our compat modules against specific mappings versions of
	// CraftBukkit yet and can consequently only support the latest mappings version for every CraftBukkit version.
	// Versions before 1.17 are not affected by this, because they don't depend on the NMS code remapping.
	static {
		// Registered in the order from latest to oldest.
		register(new CompatVersion("1_18_R2", "1.18.1", "20b026e774dbf715e40a0b2afe114792")); // Different CB revision
		register(new CompatVersion("1_18_R1", "1.18", "9e9fe6961a80f3e586c25601590b51ec"));
		register(new CompatVersion("1_17_R2", "1.17.1", "f0e3dfc7390de285a4693518dd5bd126")); // Different CB revision
		register(new CompatVersion("1_17_R1", "1.17", "acd6e6c27e5a0a9440afba70a96c27c9"));
		register(new CompatVersion("1_16_R3", "1.16.5", "d4b392244df170796f8779ef0fc1f2e9"));
		register(new CompatVersion("1_16_R3", "1.16.5", "54e89c47309b53737f894f1bf1b0edbe"));
		register(new CompatVersion("1_16_R3", "1.16.4", "da85101b34b252659e3ddf10c0c57cc9"));
		register(new CompatVersion("1_16_R2", "1.16.3", "09f04031f41cb54f1077c6ac348cc220"));
		register(new CompatVersion("1_16_R2", "1.16.2", "c2d5d7871edcc4fb0f81d18959c647af"));
		register(new CompatVersion("1_16_R1", "1.16.1", "25afc67716a170ea965092c1067ff439"));
		register(new CompatVersion("1_15_R1", "1.15.2", "5684afcc1835d966e1b6eb0ed3f72edb"));
		register(new CompatVersion("1_14_R1", "1.14.4", "11ae498d9cf909730659b6357e7c2afa"));
	}

	public static CompatVersion getCompatVersion(String compatVersion) {
		// If there are multiple entries for the same compat version, we return the latest one.
		for (CompatVersion version : SUPPORTED_MAPPINGS_VERSIONS.values()) {
			if (version.getCompatVersion().equals(compatVersion)) {
				return version;
			}
		}
		return null;
	}

	// ----

	private static NMSCallProvider provider;

	public static NMSCallProvider getProvider() {
		return NMSManager.provider;
	}

	// Returns true if the NMS provider (or fallback handler) has been successfully set up.
	public static boolean load(Plugin plugin) {
		String mappingsVersion = ServerUtils.getMappingsVersion();
		CompatVersion compatVersion = SUPPORTED_MAPPINGS_VERSIONS.get(mappingsVersion);
		if (compatVersion != null) {
			String compatVersionString = compatVersion.getCompatVersion();
			try {
				Class<?> clazz = Class.forName("com.nisovin.shopkeepers.compat.v" + compatVersionString + ".NMSHandler");
				if (NMSCallProvider.class.isAssignableFrom(clazz)) {
					NMSManager.provider = (NMSCallProvider) clazz.getConstructor().newInstance();
					return true; // Success
				} else {
					// Unexpected: NMSHandler does not implement NMSCallProvider. Continue with fallback.
				}
			} catch (Exception e) {
				// Something went wrong. Continue with fallback.
			}
		}

		// Incompatible server version detected:
		String cbVersion = ServerUtils.getCraftBukkitVersion();
		Log.warning("Incompatible server version: " + cbVersion + " (mappings: " + mappingsVersion + ")");
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
