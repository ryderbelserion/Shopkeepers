package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;

public class LivingShops {

	private final SKShopkeepersPlugin plugin;
	private final SKLivingShopObjectTypes livingEntityObjectTypes = new SKLivingShopObjectTypes(this);
	private final LivingEntityAI livingEntityAI;
	private final LivingEntityShopListener livingEntityShopListener;
	private final CreatureForceSpawnListener creatureForceSpawnListener = new CreatureForceSpawnListener();

	public LivingShops(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		livingEntityAI = new LivingEntityAI(plugin);
		livingEntityShopListener = new LivingEntityShopListener(plugin.getShopkeeperRegistry());
	}

	public void onEnable() {
		livingEntityAI.onEnable();
		Bukkit.getPluginManager().registerEvents(livingEntityShopListener, plugin);
		// Register force-creature-spawn event handler:
		if (Settings.bypassSpawnBlocking) {
			Bukkit.getPluginManager().registerEvents(creatureForceSpawnListener, plugin);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(livingEntityShopListener);
		HandlerList.unregisterAll(creatureForceSpawnListener);
		// Reset force spawning:
		creatureForceSpawnListener.forceCreatureSpawn(null, null);

		// Stop living entity AI:
		livingEntityAI.onDisable();
	}

	public SKLivingShopObjectTypes getLivingEntityObjectTypes() {
		return livingEntityObjectTypes;
	}

	public LivingEntityAI getLivingEntityAI() {
		return livingEntityAI;
	}

	// Bypassing creature spawn blocking plugins ('region protection' plugins):
	void forceCreatureSpawn(Location location, EntityType entityType) {
		if (Settings.bypassSpawnBlocking) {
			creatureForceSpawnListener.forceCreatureSpawn(location, entityType);
		}
	}
}
