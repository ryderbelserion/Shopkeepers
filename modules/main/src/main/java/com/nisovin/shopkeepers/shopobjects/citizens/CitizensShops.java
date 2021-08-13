package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.pluginhandlers.CitizensHandler;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.TimeUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.MobType;
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

	// This mapping is updated whenever shopkeepers with Citizens shop objects are added or removed from the shopkeeper
	// registry.
	// Shopkeepers for which the corresponding NPC could not be created are not contained in this mapping.
	// If multiple shopkeepers are associated with the same NPC, this mapping keeps track of all of these shopkeepers.
	private final Map<UUID, List<AbstractShopkeeper>> shopkeepersByNpcId = new HashMap<>();

	public CitizensShops(SKShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "plugin is null");
		this.plugin = plugin;
		this.citizensListener = new CitizensListener(plugin, this);
	}

	// This is called on plugin enable.
	public void onEnable() {
		this.enable();
		Bukkit.getPluginManager().registerEvents(pluginListener, plugin);
	}

	// This is called on plugin disable.
	public void onDisable() {
		this.disable();
		HandlerList.unregisterAll(pluginListener);
		shopkeepersByNpcId.clear();
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

		// Delayed to run after shopkeepers and NPCs were loaded:
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Check for invalid Citizens shopkeepers:
			this.validateCitizenShopkeepers(Settings.deleteInvalidCitizenShopkeepers, false);
		}, 3L);

		// Enabled:
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

		// Note: We intentionally don't clear the shopkeepersByNpcId mapping here, so that we do not freshly have to
		// fill it again on reloads of the Citizens plugin. It is cleaned up when the corresponding shopkeepers are
		// unloaded, and on disable of the Shopkeepers plugin.

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

	void registerCitizensShopkeeper(SKCitizensShopObject citizensShop) {
		assert citizensShop != null;
		UUID npcId = citizensShop.getNPCUniqueId();
		if (npcId == null) return;

		AbstractShopkeeper shopkeeper = citizensShop.getShopkeeper();
		// If there is no entry for this NPC id yet, we create a new List with an initial capacity of 1, because we
		// usually expect there to only be one shopkeeper associated with the NPC:
		shopkeepersByNpcId.computeIfAbsent(npcId, key -> new ArrayList<>(1)).add(shopkeeper);
	}

	void unregisterCitizensShopkeeper(SKCitizensShopObject citizensShop) {
		assert citizensShop != null;
		UUID npcId = citizensShop.getNPCUniqueId();
		if (npcId == null) return;

		AbstractShopkeeper shopkeeper = citizensShop.getShopkeeper();
		shopkeepersByNpcId.computeIfPresent(npcId, (key, shopkeepers) -> {
			shopkeepers.remove(shopkeeper);
			if (shopkeepers.isEmpty()) {
				return null; // Removes the mapping
			} else {
				return shopkeepers;
			}
		});
	}

	// If there are multiple shopkeepers associated with the given NPC, this only returns one of them.
	public AbstractShopkeeper getShopkeeper(NPC npc) {
		List<? extends AbstractShopkeeper> shopkeepers = this.getShopkeepers(npc);
		return shopkeepers.isEmpty() ? null : shopkeepers.get(0);
	}

	// Returns an empty list if there are no shopkeepers associated with the given NPC.
	public List<? extends AbstractShopkeeper> getShopkeepers(NPC npc) {
		if (npc == null) return Collections.emptyList();
		UUID npcId = npc.getUniqueId();
		assert npcId != null;
		return this.getShopkeepers(npcId);
	}

	// Returns an empty list if there are no shopkeepers associated with the given NPC id.
	private List<? extends AbstractShopkeeper> getShopkeepers(UUID npcId) {
		assert npcId != null;
		List<AbstractShopkeeper> shopkeepers = shopkeepersByNpcId.get(npcId);
		return (shopkeepers != null) ? shopkeepers : Collections.emptyList();
	}

	public static String getNPCIdString(NPC npc) {
		return npc.getId() + " (" + npc.getUniqueId() + ")";
	}

	// Returns null if this entity is no citizens NPC (or citizens or citizens shops are disabled).
	public UUID getNPCUniqueId(Entity entity) {
		if (this.isEnabled()) {
			NPC npc = CitizensAPI.getNPCRegistry().getNPC(entity);
			return (npc != null) ? npc.getUniqueId() : null;
		} else {
			return null;
		}
	}

	// Returns the created NPC, or null.
	public NPC createNPC(Location location, EntityType entityType, String name) {
		if (!this.isEnabled()) return null;

		NPC npc = CitizensAPI.getNPCRegistry().createNPC(entityType, name);
		if (npc == null) return null;

		// Ensure that Citizens remembers the mob type:
		npc.getOrAddTrait(MobType.class).setType(entityType);

		// Look towards nearby players:
		npc.getOrAddTrait(LookClose.class).lookClose(true);

		if (location != null) {
			// This will log a debug message from Citizens if it cannot spawn the NPC currently, but will then later
			// attempt to spawn it when the chunk gets loaded:
			npc.spawn(location);
		} else {
			// The Citizens shop object will periodically check if the NPC has already been spawned (i.e. if it has no
			// location assigned yet) and will otherwise attempt to spawn it.
		}
		return npc;
	}

	void onNPCEdited(NPC npc) {
		if (Settings.saveCitizenNpcsInstantly) {
			this.saveNPCs();
		} // Else: Saving is controlled only by Citizens itself: Periodically, manually, and on shutdown.
	}

	public void saveNPCs() {
		if (!this.isEnabled()) return;

		long startNanos = System.nanoTime();
		// TODO Saving is quite a heavy operation, but there is no API yet to trigger an asynchronous save. We therefore
		// execute this command to trigger an asynchronous save. However, as a side effect, this will log messages to
		// the server console.
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "citizens save -a");
		// CitizensAPI.getNPCRegistry().saveToStore();
		double durationMillis = TimeUtils.convert(System.nanoTime() - startNanos, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
		Log.debug(() -> "Saved Citizens NPCs (" + TextUtils.DECIMAL_FORMAT.format(durationMillis) + "ms)");
	}

	/**
	 * Checks for and optionally warns about or deletes invalid Citizen shopkeepers.
	 * <p>
	 * This does nothing if the Citizens integration is disabled, or if the Citizens plugin is not running currently.
	 * 
	 * @param deleteInvalidShopkeepers
	 *            <code>true</code> to also delete any found invalid Citizen shopkeepers
	 * @param silent
	 *            <code>true</code> to not log warnings about any found invalid Citizen shopkeepers
	 * @return the number of found invalid Citizen shopkeepers
	 */
	public int validateCitizenShopkeepers(boolean deleteInvalidShopkeepers, boolean silent) {
		if (!this.isEnabled()) {
			// Cannot determine which shopkeepers have a backing NPC if Citizens isn't running:
			return 0;
		}

		SKShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
		List<Shopkeeper> invalidShopkeepers = new ArrayList<>();
		shopkeeperRegistry.getAllShopkeepers().forEach(shopkeeper -> {
			if (!(shopkeeper.getShopObject() instanceof SKCitizensShopObject)) {
				return;
			}

			SKCitizensShopObject citizensShop = (SKCitizensShopObject) shopkeeper.getShopObject();
			UUID npcUniqueId = citizensShop.getNPCUniqueId();
			if (npcUniqueId == null) {
				// NPC wasn't created yet, which is only the case if a shopkeeper got somehow created without Citizens
				// being enabled:
				invalidShopkeepers.add(shopkeeper);
				if (!silent) {
					Log.warning("Invalid Citizen shopkeeper " + shopkeeper.getId() + ": The NPC has not yet been created.");
				}
				return;
			}

			if (CitizensAPI.getNPCRegistry().getByUniqueId(npcUniqueId) == null) {
				// There is no NPC with the stored NPC id:
				invalidShopkeepers.add(shopkeeper);
				if (!silent) {
					Log.warning("Invalid Citizen shopkeeper " + shopkeeper.getId() + ": There is no NPC with unique id "
							+ npcUniqueId);
				}
				return;
			}

			List<? extends AbstractShopkeeper> shopkeepers = this.getShopkeepers(npcUniqueId);
			if (shopkeepers.size() > 1) {
				// There are multiple Citizen shopkeepers using the same NPC.
				// We consider the first registered shopkeeper to be the legitimate one and don't add it to the invalid
				// shopkeepers:
				Shopkeeper mainShopkeeper = shopkeepers.get(0);
				if (mainShopkeeper != shopkeeper) {
					citizensShop.setKeepNPCOnDeletion();
					invalidShopkeepers.add(shopkeeper);
					if (!silent) {
						Log.warning("Invalid Citizen shopkeeper " + shopkeeper.getId() + ": Shopkeeper "
								+ mainShopkeeper.getId() + " is using the same NPC with unique id " + npcUniqueId);
					}
					return;
				}
			}
		});

		if (!invalidShopkeepers.isEmpty()) {
			if (deleteInvalidShopkeepers) {
				// Delete those shopkeepers:
				for (Shopkeeper shopkeeper : invalidShopkeepers) {
					shopkeeper.delete();
				}

				// Save:
				plugin.getShopkeeperStorage().save();

				if (!silent) {
					Log.warning("Deleted " + invalidShopkeepers.size() + " invalid Citizen shopkeepers!");
				}
			} else {
				// Only log a warning:
				if (!silent) {
					Log.warning("Found " + invalidShopkeepers.size() + " invalid Citizen shopkeepers!");
					Log.warning("Either enable the setting 'delete-invalid-citizen-shopkeepers' inside the config, or use the "
							+ "command '/shopkeepers cleanupCitizenShopkeepers' to automatically delete these shopkeepers and "
							+ "get rid of these warnings.");
				}
			}
		}
		return invalidShopkeepers.size();
	}
}
