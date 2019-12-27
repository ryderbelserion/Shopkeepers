package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.util.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.LookClose;

public class CitizensShops {

	private final SKShopkeepersPlugin plugin;
	private final SKCitizensShopObjectType citizensShopObjectType = new SKCitizensShopObjectType(this);
	private final PluginListener pluginListener = new PluginListener(this);

	private final CitizensListener citizensListener = new CitizensListener();
	private boolean citizensShopsEnabled = false;
	private TraitInfo shopkeeperTrait = null;

	public CitizensShops(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		this.enable();
		Bukkit.getPluginManager().registerEvents(pluginListener, plugin);
	}

	public void onDisable() {
		this.disable();
		HandlerList.unregisterAll(pluginListener);
	}

	public SKCitizensShopObjectType getCitizensShopObjectType() {
		return citizensShopObjectType;
	}

	/**
	 * Returns whether citizens shopkeepers are currently enabled.
	 * 
	 * @return <code>true</code> if currently enabled
	 */
	public boolean isEnabled() {
		return citizensShopsEnabled;
	}

	void enable() {
		if (this.isEnabled()) {
			// disable first, to perform cleanup if needed:
			this.disable();
		}

		if (!Settings.enableCitizenShops) return; // feature disabled
		if (!CitizensHandler.isPluginEnabled()) {
			Log.warning("Citizens Shops enabled, but Citizens plugin not found or disabled.");
			return;
		}
		Log.info("Citizens found, enabling NPC shopkeepers.");

		// register shopkeeper trait:
		this.registerShopkeeperTrait();

		// register citizens listener:
		Bukkit.getPluginManager().registerEvents(citizensListener, plugin);

		// delayed to run after shopkeepers were loaded:
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// run legacy id conversion: // TODO remove again at some point
			this.convertLegacyNPCIds();
			// remove invalid citizens shopkeepers:
			this.removeInvalidCitizensShopkeepers();
		}, 3L);

		// enabled:
		citizensShopsEnabled = true;
	}

	void disable() {
		if (!this.isEnabled()) {
			// already disabled
			return;
		}

		Plugin citizensPlugin = CitizensHandler.getPlugin();
		if (citizensPlugin != null) {
			// unregister shopkeeper trait:
			this.unregisterShopkeeperTrait();
		}

		// unregister citizens listener:
		HandlerList.unregisterAll(citizensListener);

		// disabled:
		citizensShopsEnabled = false;
	}

	private void registerShopkeeperTrait() {
		assert shopkeeperTrait == null;
		shopkeeperTrait = TraitInfo.create(CitizensShopkeeperTrait.class).withName(CitizensShopkeeperTrait.TRAIT_NAME);
		try {
			CitizensAPI.getTraitFactory().registerTrait(shopkeeperTrait);
		} catch (Throwable ex) {
			Log.debug(() -> "Shopkeeper trait registration error: " + ex.getMessage());
			if (Settings.debug) {
				ex.printStackTrace();
			}
		}
	}

	private void unregisterShopkeeperTrait() {
		if (shopkeeperTrait != null) {
			try {
				CitizensAPI.getTraitFactory().deregisterTrait(shopkeeperTrait);
			} catch (Throwable ex) {
				Log.debug(() -> "Shopkeeper trait unregistration error: " + ex.getMessage());
				if (Settings.debug) {
					ex.printStackTrace();
				}
			} finally {
				shopkeeperTrait = null;
			}
		}
	}

	public static String getNPCIdString(NPC npc) {
		return npc.getId() + " (" + npc.getUniqueId() + ")";
	}

	// returns null if this entity is no citizens npc (or citizens or citizens shops are disabled)
	public UUID getNPCUniqueId(Entity entity) {
		if (this.isEnabled()) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
			return (npc != null ? npc.getUniqueId() : null);
		} else {
			return null;
		}
	}

	// returns the uuid of the created npc, or null
	public UUID createNPC(Location location, EntityType entityType, String name) {
		if (!this.isEnabled()) return null;
		NPC npc = CitizensAPI.getNPCRegistry().createNPC(entityType, name);
		if (npc == null) return null;
		// look towards near players:
		npc.getTrait(LookClose.class).lookClose(true);
		// this will log a debug message from citizens if it currently cannot spawn this npc,
		// but will then later attempt to spawn it when the chunk is loaded:
		npc.spawn(location);
		// npc.teleport(loc, PlayerTeleportEvent.TeleportCause.PLUGIN);
		return npc.getUniqueId();
	}

	private void convertLegacyNPCIds() {
		if (!this.isEnabled()) {
			// cannot determine backing npcs if citizens isn't running:
			return;
		}
		ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		boolean dirty = false;
		for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
			if (shopkeeper.getShopObject() instanceof SKCitizensShopObject) {
				SKCitizensShopObject citizensShop = (SKCitizensShopObject) shopkeeper.getShopObject();
				citizensShop.convertLegacyId();
				if (shopkeeper.isDirty()) dirty = true;
			}
		}

		if (dirty) {
			// save:
			plugin.getShopkeeperStorage().save();
		}
	}

	public void removeInvalidCitizensShopkeepers() {
		if (!this.isEnabled()) {
			// cannot determine which shopkeepers have a backing npc if citizens isn't running:
			return;
		}
		ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		List<Shopkeeper> forRemoval = new ArrayList<>();
		for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
			if (shopkeeper.getShopObject() instanceof SKCitizensShopObject) {
				SKCitizensShopObject citizensShop = (SKCitizensShopObject) shopkeeper.getShopObject();
				UUID npcUniqueId = citizensShop.getNPCUniqueId();
				if (npcUniqueId == null) {
					// npc wasn't created yet, which is only the case if a shopkeeper got somehow created without
					// citizens being enabled:
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": NPC has not been created.");
				} else if (CitizensAPI.getNPCRegistry().getByUniqueId(npcUniqueId) == null) {
					// there is no npc with the stored unique id:
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": No NPC existing with unique id '" + npcUniqueId + "'.");
				} else if (shopkeeperRegistry.getActiveShopkeeper(shopkeeper.getShopObject().getId()) != shopkeeper) {
					// there is already another citizens shopkeeper using this npc id:
					citizensShop.setKeepNPCOnDeletion();
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": There exists another shopkeeper using the same NPC with unique id '" + npcUniqueId + "'.");
				}
			}
		}

		// remove those shopkeepers:
		if (!forRemoval.isEmpty()) {
			for (Shopkeeper shopkeeper : forRemoval) {
				shopkeeper.delete();
			}

			// save:
			plugin.getShopkeeperStorage().save();
		}
	}

	// unused
	public void removeShopkeeperTraits() {
		if (!this.isEnabled()) return;
		Iterator<NPC> npcs = CitizensAPI.getNPCRegistry().iterator();
		while (npcs.hasNext()) {
			NPC npc = npcs.next();
			if (npc.hasTrait(CitizensShopkeeperTrait.class)) {
				npc.removeTrait(CitizensShopkeeperTrait.class);
			}
		}
	}
}
