package com.nisovin.shopkeepers.shopobjects.citizens;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

public class CitizensShopkeeperTrait extends Trait {

	public static final String TRAIT_NAME = "shopkeeper";

	// Citizens shop object id:
	private String shopObjectId = null;

	public CitizensShopkeeperTrait() {
		super(TRAIT_NAME);
	}

	@Override
	public void load(DataKey key) {
		this.shopObjectId = key.getString("ShopkeeperId", null);
	}

	@Override
	public void save(DataKey key) {
		key.setString("ShopkeeperId", shopObjectId);
	}

	public Shopkeeper getShopkeeper() {
		if (shopObjectId == null) return null;
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) return null;
		return plugin.getShopkeeperRegistry().getActiveShopkeeper(shopObjectId);
	}

	@Override
	public void onRemove() {
		// This was also called when citizens reloads or disables in the past..
		// We detect trait removal by listening to specific citizens events.
	}

	void onShopkeeperDeletion(Shopkeeper shopkeeper) {
		Log.debug(() -> "Removing the 'shopkeeper' trait from Citizens NPC " + CitizensShops.getNPCIdString(npc)
				+ " due to the deletion of shopkeeper " + shopkeeper.getIdString());
		shopObjectId = null;
		this.getNPC().removeTrait(CitizensShopkeeperTrait.class);
	}

	/**
	 * Called whenever this trait got deleted from the NPC.
	 * <p>
	 * Unlike {@link #onTraitAdded(Player)}, which is also called on reloads of the NPC, this is only called if the
	 * trait is permanently deleted.
	 * 
	 * @param player
	 *            the player who deleted the trait, or <code>null</code> if not available
	 */
	public void onTraitDeleted(Player player) {
		Shopkeeper shopkeeper = this.getShopkeeper();
		if (shopkeeper != null) {
			Log.debug(() -> "Removing the shopkeeper " + shopkeeper.getId() + " due to the deletion of the 'shopkeeper' trait"
					+ " from the Citizens NPC " + CitizensShops.getNPCIdString(npc));
			assert shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN();
			SKCitizensShopObject shopObject = (SKCitizensShopObject) shopkeeper.getShopObject();
			shopObject.setKeepNPCOnDeletion();
			// This should keep the citizens npc and only remove the shopkeeper data:
			shopkeeper.delete(player);
			// Save:
			shopkeeper.save();
		} else {
			// TODO If the trait gets removed while the Shopkeepers plugin is not running, the shopkeeper does not get
			// removed and will remain attached to the NPC until it gets removed via the shopkeeper editor or via
			// command. Maybe always attach the 'shopkeeper' trait and check on startup if the trait is still present
			// and otherwise delete the shopkeeper?
		}
	}

	/**
	 * Called whenever this trait got added to a NPC.
	 * <p>
	 * This is also called whenever the NPC gets reloaded. This is called shortly after the trait got attached.
	 * 
	 * @param player
	 *            the player who added the trait, can be <code>null</code> if not available
	 */
	void onTraitAdded(Player player) {
		// Create a new shopkeeper (if there isn't one already for this NPC), using the given player as creator:
		this.createShopkeeper(player);
	}

	@Override
	public void onAttach() {
		// TODO Is this still required? We already handle all trait additions via events now.
		// Note: This is also called whenever citizens gets reloaded.
		// Log.debug("Shopkeeper trait attached to NPC " + npc.getId());

		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) {
			// Shopkeepers is not running:
			return;
		}

		// Giving citizens some time to properly initialize the trait and NPC:
		// Also: Shopkeeper creation by a player is handled after trait attachment.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Create a new shopkeeper if there isn't one already for this NPC (without creator):
			this.createShopkeeper(null);
		}, 5L);
	}

	private boolean isMissingShopkeeper() {
		NPC npc = this.getNPC();
		if (npc == null || !npc.hasTrait(CitizensShopkeeperTrait.class)) {
			// Citizens not running or trait got already removed again?
			return false;
		}
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) {
			// Shopkeepers not running:
			return false;
		}

		Shopkeeper shopkeeper = CitizensShops.getShopkeeper(npc);
		if (shopkeeper != null) {
			// There is already a shopkeeper for this NPC. The trait was probably re-attached after a reload of
			// Citizens.
			return false;
		}
		return true;
	}

	// Creator can be null.
	private void createShopkeeper(Player creator) {
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) {
			// Shopkeepers is not running:
			return;
		}

		// Create a new shopkeeper (if there isn't one already for this NPC):
		if (!this.isMissingShopkeeper()) {
			return;
		}

		NPC npc = this.getNPC();
		Log.debug(() -> "Creating shopkeeper for NPC " + CitizensShops.getNPCIdString(npc)
				+ (creator != null ? " and player '" + creator.getName() + "'" : ""));

		Location location = null;
		Entity entity = npc.getEntity();
		if (entity != null) {
			location = entity.getLocation();
		} else {
			location = npc.getStoredLocation();
		}

		String shopkeeperCreationError = null; // Null indicates success
		if (location != null) {
			ShopCreationData creationData = AdminShopCreationData.create(creator, DefaultShopTypes.ADMIN(), DefaultShopObjectTypes.CITIZEN(), location, null);
			creationData.setValue(SKCitizensShopObject.CREATION_DATA_NPC_UUID_KEY, npc.getUniqueId());

			Shopkeeper shopkeeper = null;
			if (creator != null) {
				// Handle shopkeeper creation by player:
				shopkeeper = plugin.handleShopkeeperCreation(creationData);
			} else {
				// Create shopkeeper directly (without available creator):
				ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
				try {
					shopkeeper = shopkeeperRegistry.createShopkeeper(creationData);
					assert shopkeeper != null;

					// Save:
					shopkeeper.save();
				} catch (ShopkeeperCreateException e) {
					// Some issue identified during shopkeeper creation (possibly hinting to a bug):
					Log.warning("Shopkeeper creation failed!", e);
				}
			}

			if (shopkeeper != null) {
				shopObjectId = shopkeeper.getShopObject().getId();
			} else {
				// Shopkeeper creation failed:
				// TODO Translation?
				shopkeeperCreationError = "Shopkeeper creation via trait failed. Removing the trait again.";
			}
		} else {
			// NPC did not provide any location. We cannot create a shopkeeper without location.
			// TODO Translation?
			shopkeeperCreationError = "Shopkeeper creation via trait failed due to missing NPC location. Removing the trait again.";
		}

		if (shopkeeperCreationError != null) {
			// Shopkeeper creation failed:
			Log.warning(shopkeeperCreationError);
			if (creator != null) {
				TextUtils.sendMessage(creator, ChatColor.RED + shopkeeperCreationError);
			}

			shopObjectId = null;
			Bukkit.getScheduler().runTask(plugin, () -> npc.removeTrait(CitizensShopkeeperTrait.class));
		}
	}
}
