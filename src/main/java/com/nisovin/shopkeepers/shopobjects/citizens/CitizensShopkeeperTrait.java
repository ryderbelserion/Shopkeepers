package com.nisovin.shopkeepers.shopobjects.citizens;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.util.Log;

import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;

public class CitizensShopkeeperTrait extends Trait {

	public static final String TRAIT_NAME = "shopkeeper";

	// citizens shop object id
	private String shopkeeperId = null;

	public CitizensShopkeeperTrait() {
		super("shopkeeper");
	}

	@Override
	public void load(DataKey key) {
		this.shopkeeperId = key.getString("ShopkeeperId", null);
	}

	@Override
	public void save(DataKey key) {
		key.setString("ShopkeeperId", shopkeeperId);
	}

	public Shopkeeper getShopkeeper() {
		if (shopkeeperId == null) return null;
		ShopkeepersPlugin plugin = ShopkeepersPlugin.getInstance();
		if (plugin == null) return null;
		return plugin.getShopkeeperRegistry().getActiveShopkeeper(shopkeeperId);
	}

	@Override
	public void onRemove() {
		// this was also called when citizens reloads or disables in the past..
		// we detect trait removal by listening to specific citizens events
	}

	void onShopkeeperRemove() {
		Log.debug("Removing citizens trait due to shopkeeper removal for NPC " + npc.getId());
		shopkeeperId = null;
		this.getNPC().removeTrait(CitizensShopkeeperTrait.class);
	}

	public void onTraitDeletion() {
		Shopkeeper shopkeeper = this.getShopkeeper();
		if (shopkeeper != null) {
			Log.debug("Removing shopkeeper " + shopkeeper.getId() + " due to citizens trait removal for NPC " + npc.getId());
			assert shopkeeper.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN();
			CitizensShop shopObject = (CitizensShop) shopkeeper.getShopObject();
			shopObject.onTraitRemoval();
			// this should keep the citizens npc and only remove the shopkeeper data:
			shopkeeper.delete();
			// save:
			shopkeeper.save();
		} else {
			// TODO what if the trait gets removed and Shopkeepers is disabled?
			// -> does a new npc get created when Shopkeepers enables again?
		}
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

		if (plugin.getShopkeeperRegistry().getActiveShopkeeper(CitizensShop.getId(npc.getId())) != null) {
			// there is already a shopkeeper for this npc:
			// the trait was probably re-attached after a reload of citizens:
			return false;
		}

		return true;
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
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// create a new shopkeeper if there isn't one already for this npc:
			if (!this.isMissingShopkeeper()) {
				return;
			}

			NPC npc = this.getNPC();
			Log.debug("Creating shopkeeper for NPC " + npc.getId());

			Location location = null;
			Entity entity = npc.getEntity();
			if (entity != null) {
				location = entity.getLocation();
			} else {
				location = npc.getStoredLocation();
			}

			if (location != null) {
				ShopCreationData creationData = ShopCreationData.create(null, DefaultShopTypes.ADMIN(), DefaultShopObjectTypes.CITIZEN(), location, null);
				creationData.setValue(CitizensShop.CREATION_DATA_NPC_ID_KEY, npc.getId());
				Shopkeeper shopkeeper = plugin.handleShopkeeperCreation(creationData);
				if (shopkeeper != null) {
					shopkeeperId = shopkeeper.getObjectId();
				} else {
					Log.warning("Shopkeeper creation via trait failed. Removing trait again.");
					shopkeeperId = null;
					Bukkit.getScheduler().runTask(plugin, () -> npc.removeTrait(CitizensShopkeeperTrait.class));
				}
			} else {
				// well.. no idea what to do in that case.. we cannot create a shopkeeper without a location, right?
				Log.debug("Shopkeeper NPC Trait: Failed to create shopkeeper due to missing npc location.");
			}
		}, 5L);
	}
}
