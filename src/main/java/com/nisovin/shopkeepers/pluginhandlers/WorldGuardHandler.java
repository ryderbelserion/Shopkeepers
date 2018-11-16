package com.nisovin.shopkeepers.pluginhandlers;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.Settings;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.flags.BooleanFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionQuery;

public class WorldGuardHandler {

	public static final String PLUGIN_NAME = "WorldGuard";

	public static Plugin getPlugin() {
		return Bukkit.getPluginManager().getPlugin(PLUGIN_NAME);
	}

	public static boolean isPluginEnabled() {
		return Bukkit.getPluginManager().isPluginEnabled(PLUGIN_NAME);
	}

	public static boolean isShopAllowed(Player player, Location loc) {
		// note: This works even if WorldGuard is not present.
		// The class is only going to get resolved, when it is required (ex. when accessed).
		WorldGuardPlugin wgPlugin = (WorldGuardPlugin) getPlugin();
		if (wgPlugin == null || !wgPlugin.isEnabled()) return true;

		RegionQuery query = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery();
	
		// check if shop flag is set:
		boolean allowShopFlag = false; // false if unset or disallowed

		// get shop flag (might not exist if removed from WorldGuard by now, and not re-implemented by another plugin):
		Flag<?> shopFlag = Flags.get("allow-shop");
		if (shopFlag == null) {
			// try alternative name:
			shopFlag = Flags.get("enable-shop");
		}
		if (shopFlag != null) {
			// check if shop flag is set:
			if (shopFlag instanceof StateFlag) {
				allowShopFlag = query.testState(BukkitAdapter.adapt(loc), null, (StateFlag) shopFlag);
			} else if (shopFlag instanceof BooleanFlag) {
				// value might be null:
				Boolean shopFlagValue = query.queryValue(BukkitAdapter.adapt(loc), null, (BooleanFlag) shopFlag);
				allowShopFlag = (Boolean.TRUE.equals(shopFlagValue));
			} else {
				// unknown flag type, assume unset
			}
		} else {
			// shop flag doesn't exist, assume unset
		}

		if (Settings.requireWorldGuardAllowShopFlag) {
			// allow shops ONLY in regions with the shop flag set:
			return allowShopFlag;
		} else {
			// allow shops in regions where the shop flag is set OR the player can build:
			return (allowShopFlag || query.testState(BukkitAdapter.adapt(loc), wgPlugin.wrapPlayer(player), Flags.BUILD));
		}
	}
}
