package com.nisovin.shopkeepers.shopobjects.citizens;

import java.util.UUID;

import org.bukkit.Bukkit;
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
import com.nisovin.shopkeepers.shopobjects.SKDefaultShopObjectTypes;
import com.nisovin.shopkeepers.util.Log;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

public class CitizensShopkeeperTrait extends Trait {

	public static final String TRAIT_NAME = "shopkeeper";

	// citizens shop object id
	private String shopObjectId = null;

	public CitizensShopkeeperTrait() {
		super("shopkeeper");
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
		// this was also called when citizens reloads or disables in the past..
		// we detect trait removal by listening to specific citizens events
	}

	void onShopkeeperRemove() {
		Log.debug(() -> "Removing citizens trait due to shopkeeper removal for NPC " + CitizensShops.getNPCIdString(npc));
		shopObjectId = null;
		this.getNPC().removeTrait(CitizensShopkeeperTrait.class);
	}

	public void onTraitDeletion() {
		Shopkeeper shopkeeper = this.getShopkeeper();
		if (shopkeeper != null) {
			Log.debug(() -> "Removing shopkeeper " + shopkeeper.getId() + " due to citizens trait removal for NPC " + CitizensShops.getNPCIdString(npc));
			assert shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN();
			SKCitizensShopObject shopObject = (SKCitizensShopObject) shopkeeper.getShopObject();
			shopObject.setKeepNPCOnDeletion();
			// this should keep the citizens npc and only remove the shopkeeper data:
			shopkeeper.delete();
			// save:
			shopkeeper.save();
		} else {
			// TODO what if the trait gets removed and Shopkeepers is disabled?
			// -> does a new npc get created when Shopkeepers enables again?
		}
	}

	// if a player added the trait via command, this gets called shortly after the trait got attached
	public void onTraitAddedByPlayer(Player player) {
		// create a new shopkeeper (if there isn't one already for this npc), using the given player as creator:
		this.createShopkeeper(player);
	}

	@Override
	public void onAttach() {
		// note: this is also called whenever citizens gets reloaded
		// Log.debug("Shopkeeper trait attached to NPC " + npc.getId());

		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) {
			// shopkeepers is not running:
			return;
		}

		// giving citizens some time to properly initialize the trait and npc:
		// also: shopkeeper creation by a player is handled after trait attachment
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// create a new shopkeeper if there isn't one already for this npc (with out creator):
			this.createShopkeeper(null);
		}, 5L);
	}

	private boolean isMissingShopkeeper() {
		NPC npc = this.getNPC();
		if (npc == null || !npc.hasTrait(CitizensShopkeeperTrait.class)) {
			// citizens not running or trait got already removed again?
			return false;
		}
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) {
			// shopkeepers not running:
			return false;
		}

		UUID npcUniqueId = npc.getUniqueId();
		String shopObjectId = SKDefaultShopObjectTypes.CITIZEN().createObjectId(npcUniqueId);
		if (plugin.getShopkeeperRegistry().getActiveShopkeeper(shopObjectId) != null) {
			// there is already a shopkeeper for this npc:
			// the trait was probably re-attached after a reload of citizens:
			return false;
		}

		return true;
	}

	// creator can be null
	private void createShopkeeper(Player creator) {
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) {
			// shopkeepers is not running:
			return;
		}

		// create a new shopkeeper (if there isn't one already for this npc):
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

		if (location != null) {
			ShopCreationData creationData = AdminShopCreationData.create(creator, DefaultShopTypes.ADMIN(), DefaultShopObjectTypes.CITIZEN(), location, null);
			creationData.setValue(SKCitizensShopObject.CREATION_DATA_NPC_UUID_KEY, npc.getUniqueId());

			Shopkeeper shopkeeper = null;
			if (creator != null) {
				// handle shopkeeper creation by player:
				shopkeeper = plugin.handleShopkeeperCreation(creationData);
			} else {
				// create shopkeeper directly (without available creator):
				ShopkeeperRegistry shopkeeperRegistry = plugin.getShopkeeperRegistry();
				try {
					shopkeeper = shopkeeperRegistry.createShopkeeper(creationData);
					assert shopkeeper != null;

					// save:
					shopkeeper.save();
				} catch (ShopkeeperCreateException e) {
					// some issue identified during shopkeeper creation (possibly hinting to a bug):
					Log.warning("Shopkeeper creation failed!", e);
				}
			}

			if (shopkeeper != null) {
				shopObjectId = shopkeeper.getShopObject().getId();
			} else {
				Log.warning("Shopkeeper creation via trait failed. Removing trait again.");
				shopObjectId = null;
				Bukkit.getScheduler().runTask(plugin, () -> npc.removeTrait(CitizensShopkeeperTrait.class));
			}
		} else {
			// well.. no idea what to do in that case.. we cannot create a shopkeeper without a location, right?
			Log.debug("Shopkeeper NPC Trait: Failed to create shopkeeper due to missing NPC location.");
		}
	}
}
