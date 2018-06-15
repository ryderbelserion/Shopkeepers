package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;

public class LivingEntityShops {

	private final SKShopkeepersPlugin plugin;
	private final SKLivingEntityObjectTypes livingEntityObjectTypes = new SKLivingEntityObjectTypes(this);
	private final LivingEntityAI livingEntityAI;
	private final LivingEntityShopListener livingEntityShopListener;
	private final CreatureForceSpawnListener creatureForceSpawnListener = new CreatureForceSpawnListener();

	public LivingEntityShops(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		livingEntityAI = new LivingEntityAI(plugin);
		livingEntityShopListener = new LivingEntityShopListener(plugin.getShopkeeperRegistry());
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(livingEntityShopListener, plugin);
		// register force-creature-spawn event handler:
		if (Settings.bypassSpawnBlocking) {
			Bukkit.getPluginManager().registerEvents(creatureForceSpawnListener, plugin);
		}
	}

	public void onDisable() {
		HandlerList.unregisterAll(livingEntityShopListener);
		HandlerList.unregisterAll(creatureForceSpawnListener);
		// reset force spawning:
		creatureForceSpawnListener.forceCreatureSpawn(null, null);

		// stop living entity AI:
		livingEntityAI.stop();
		livingEntityAI.reset(); // cleanup, reset timings, etc.
	}

	public SKLivingEntityObjectTypes getLivingEntityObjectTypes() {
		return livingEntityObjectTypes;
	}

	public LivingEntityAI getLivingEntityAI() {
		return livingEntityAI;
	}

	// bypassing creature spawn blocking plugins ('region protection' plugins):
	void forceCreatureSpawn(Location location, EntityType entityType) {
		if (Settings.bypassSpawnBlocking) {
			creatureForceSpawnListener.forceCreatureSpawn(location, entityType);
		}
	}
}
