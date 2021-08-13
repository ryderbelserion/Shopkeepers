package com.nisovin.shopkeepers.villagers;

import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Features related to regular villagers.
 */
public class RegularVillagers {

	private final SKShopkeepersPlugin plugin;
	private final VillagerInteractionListener villagerInteractionListener;
	private final BlockVillagerSpawnListener blockVillagerSpawnListener;
	private final BlockZombieVillagerCuringListener blockZombieVillagerCuringListener;

	public RegularVillagers(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.villagerInteractionListener = new VillagerInteractionListener(plugin);
		this.blockVillagerSpawnListener = new BlockVillagerSpawnListener();
		this.blockZombieVillagerCuringListener = new BlockZombieVillagerCuringListener();
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(villagerInteractionListener, plugin);
		if (Settings.blockVillagerSpawns || Settings.blockWanderingTraderSpawns) {
			Bukkit.getPluginManager().registerEvents(blockVillagerSpawnListener, plugin);
		}
		if (Settings.disableZombieVillagerCuring) {
			Bukkit.getPluginManager().registerEvents(blockZombieVillagerCuringListener, plugin);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(villagerInteractionListener);
		HandlerList.unregisterAll(blockVillagerSpawnListener);
		HandlerList.unregisterAll(blockZombieVillagerCuringListener);
	}
}
