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
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.util.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.trait.LookClose;

/**
 * Citizens shopkeepers can be created in two ways:
 * <ol>
 * <li>By creating a shopkeeper of type 'npc' (alias 'citizen'), either via command, the shop creation item or by
 * another plugin through the Shopkeepers API. This will create both the shopkeeper and the corresponding Citizens NPC.
 * <li>By attaching the 'shopkeeper' trait to an already existing Citizens NPC. The trait can also be attached to a NPC
 * for which there already exists a corresponding shopkeeper. No new shopkeeper will be created then.
 * </ol>
 * 
 * Citizen shopkeepers can be removed again in the following ways:
 * <ol>
 * <li>The shopkeeper gets deleted, either due to a player removing it via command or via the editor option, or due the
 * Shopkeepers plugin removing it due to reasons such owner inactivity or when the shopkeeper's container is broken. If
 * the corresponding Citizens NPC has the 'shopkeeper' trait attached, only this trait gets removed and the NPC remains
 * existing. Otherwise, the Citizens NPC is removed.
 * TODO Removing the Citizens NPC only works if the Citizens plugin is currently running.
 * <li>The Citizens NPC gets deleted. This deletes the corresponding shopkeeper. If the Shopkeepers plugin is not
 * running currently, the shopkeeper gets deleted during the next startup of the Shopkeepers plugin.
 * <li>The 'shopkeeper' trait gets removed from the Citizens NPC. This deletes the corresponding shopkeeper but keeps
 * the Citizens NPC alive.
 * TODO Removing the shopkeeper only works if the Citizens plugin is currently running. Otherwise the shopkeeper will
 * remain existing and attached to the NPC until it is removed directly (eg. via the editor or via command).
 * <li>On plugin startup, if the Citizens plugin is running as well, we check for and remove invalid shopkeepers such as
 * shopkeepers for which there is no corresponding Citizens NPC (anymore) or shopkeepers which have the same Citizens
 * NPC assigned as another shopkeeper.
 * </ol>
 */
public class CitizensShops {

	private final SKShopkeepersPlugin plugin;
	private final SKCitizensShopObjectType citizensShopObjectType = new SKCitizensShopObjectType(this);
	private final PluginListener pluginListener = new PluginListener(this);

	private final CitizensListener citizensListener;
	private boolean citizensShopsEnabled = false;
	private TraitInfo shopkeeperTrait = null;

	public CitizensShops(SKShopkeepersPlugin plugin) {
		assert plugin != null;
		this.plugin = plugin;
		this.citizensListener = new CitizensListener(plugin);
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
			// Disable first, to perform cleanup if needed:
			this.disable();
		}

		if (!Settings.enableCitizenShops) return; // Feature disabled
		if (!CitizensHandler.isPluginEnabled()) {
			Log.debug("Citizen shops enabled, but Citizens plugin not found or disabled.");
			return;
		}
		Log.info("Citizens found, enabling NPC shopkeepers.");

		// Register shopkeeper trait:
		this.registerShopkeeperTrait();

		// Register citizens listener:
		Bukkit.getPluginManager().registerEvents(citizensListener, plugin);
		citizensListener.onEnable();

		// Delayed to run after shopkeepers were loaded:
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Run legacy id conversion: // TODO Remove again at some point.
			this.convertLegacyNPCIds();
			// Remove invalid citizens shopkeepers:
			this.removeInvalidCitizensShopkeepers();
		}, 3L);

		// enabled:
		citizensShopsEnabled = true;
	}

	void disable() {
		if (!this.isEnabled()) {
			// Already disabled
			return;
		}

		Plugin citizensPlugin = CitizensHandler.getPlugin();
		if (citizensPlugin != null) {
			// Unregister shopkeeper trait:
			this.unregisterShopkeeperTrait();
		}

		// Unregister citizens listener:
		citizensListener.onDisable();
		HandlerList.unregisterAll(citizensListener);

		// Disabled:
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

	// Returns null if this entity is no citizens NPC (or citizens or citizens shops are disabled).
	public UUID getNPCUniqueId(Entity entity) {
		if (this.isEnabled()) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
			return (npc != null ? npc.getUniqueId() : null);
		} else {
			return null;
		}
	}

	// Returns the uuid of the created NPC, or null.
	public UUID createNPC(Location location, EntityType entityType, String name) {
		if (!this.isEnabled()) return null;
		NPC npc = CitizensAPI.getNPCRegistry().createNPC(entityType, name);
		if (npc == null) return null;
		// Look towards near players:
		npc.getTrait(LookClose.class).lookClose(true);
		if (location != null) {
			// This will log a debug message from Citizens if it cannot spawn the NPC currently, but will then later
			// attempt to spawn it when the chunk gets loaded:
			npc.spawn(location);
		} else {
			// Our teleport task will periodically call SKCitizensShopObject#check(), which spawns the NPC if this has
			// not yet happened (if it has no location assigned yet).
		}
		return npc.getUniqueId();
	}

	private void convertLegacyNPCIds() {
		if (!this.isEnabled()) {
			// Cannot determine backing NPCs if citizens isn't running:
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
			// Save:
			plugin.getShopkeeperStorage().save();
		}
	}

	public void removeInvalidCitizensShopkeepers() {
		if (!this.isEnabled()) {
			// Cannot determine which shopkeepers have a backing NPC if Citizens isn't running:
			return;
		}
		ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		List<Shopkeeper> forRemoval = new ArrayList<>();
		for (Shopkeeper shopkeeper : shopkeeperRegistry.getAllShopkeepers()) {
			if (shopkeeper.getShopObject() instanceof SKCitizensShopObject) {
				SKCitizensShopObject citizensShop = (SKCitizensShopObject) shopkeeper.getShopObject();
				UUID npcUniqueId = citizensShop.getNPCUniqueId();
				if (npcUniqueId == null) {
					// NPC wasn't created yet, which is only the case if a shopkeeper got somehow created without
					// citizens being enabled:
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": NPC has not been created.");
				} else if (CitizensAPI.getNPCRegistry().getByUniqueId(npcUniqueId) == null) {
					// There is no NPC with the stored unique id:
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": No NPC existing with unique id '" + npcUniqueId + "'.");
				} else if (shopkeeperRegistry.getActiveShopkeeper(shopkeeper.getShopObject().getId()) != shopkeeper) {
					// There is already another citizens shopkeeper using this NPC id:
					citizensShop.setKeepNPCOnDeletion();
					forRemoval.add(shopkeeper);
					Log.warning("Removing citizens shopkeeper at " + shopkeeper.getPositionString()
							+ ": There exists another shopkeeper using the same NPC with unique id '" + npcUniqueId + "'.");
				}
			}
		}

		// Remove those shopkeepers:
		if (!forRemoval.isEmpty()) {
			for (Shopkeeper shopkeeper : forRemoval) {
				shopkeeper.delete();
			}

			// Save:
			plugin.getShopkeeperStorage().save();
		}
	}

	// Unused
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

	public static Shopkeeper getShopkeeper(NPC npc) {
		if (npc == null) return null;
		assert ShopkeepersAPI.isEnabled();
		UUID npcUniqueId = npc.getUniqueId();
		String shopObjectId = SKDefaultShopObjectTypes.CITIZEN().createObjectId(npcUniqueId);
		return ShopkeepersAPI.getShopkeeperRegistry().getActiveShopkeeper(shopObjectId);
	}
}
