package com.nisovin.shopkeepers.shopkeeper.registry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Keeps track of spawned shop objects.
 */
public class ShopObjectRegistry {

	// Spawned shopkeepers by their shop object ids:
	// These shopkeepers are not necessarily ticked yet: Shopkeepers start ticking once their chunk
	// has been activated.
	// Since some types of shop objects may handle their spawning themselves, shop objects might
	// already be spawned before their chunk is activated.
	private final Map<Object, AbstractShopkeeper> shopkeepersByObjectId = new HashMap<>();

	ShopObjectRegistry() {
	}

	public void onEnable() {
	}

	public void onDisable() {
		this.ensureEmpty();
	}

	private void ensureEmpty() {
		if (!shopkeepersByObjectId.isEmpty()) {
			Log.warning("Some spawned shop objects were not properly unregistered!");
			shopkeepersByObjectId.clear();
		}
	}

	public boolean isRegistered(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Object objectId = shopkeeper.getShopObject().getLastId();
		assert (objectId == null) || (this.getShopkeeperByObjectId(objectId) == shopkeeper);
		return (objectId != null);
	}

	public @Nullable AbstractShopkeeper getShopkeeperByObjectId(Object objectId) {
		return shopkeepersByObjectId.get(objectId);
	}

	/**
	 * Handles the registration and unregistration of spawned shop objects.
	 * <p>
	 * If the shop object was previously already spawned but its object id has changed, this
	 * unregisters the previous object id and registers the new object id.
	 * <p>
	 * This needs to be called by shop objects whenever their object id might have changed, such as
	 * when they spawned or despawned.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code> and not {@link Shopkeeper#isVirtual()
	 *            virtual}
	 */
	public void updateShopObjectRegistration(AbstractShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.isTrue(!shopkeeper.isVirtual(), "shopkeeper is virtual");

		AbstractShopObject shopObject = shopkeeper.getShopObject();
		Object lastObjectId = shopObject.getLastId();
		Object currentObjectId = shopObject.getId();
		if (Objects.equals(lastObjectId, currentObjectId)) {
			// The current object id equals the last object id, so there is no need to update the
			// registration.
			return;
		}

		// Update the registration:
		this.registerShopObject(shopkeeper);
	}

	private void registerShopObject(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		AbstractShopObject shopObject = shopkeeper.getShopObject();

		// Unregister the shopkeeper by its previous object id (if there is one):
		this.unregisterShopObject(shopkeeper);
		assert shopObject.getLastId() == null;

		// Get the new object id:
		Object objectId = shopObject.getId();
		if (objectId == null) {
			// The shop object is not spawned currently.
			return;
		}

		// Keep track of the shopkeeper's shop object id, if there isn't already another shopkeeper
		// using the same object id:
		Object finalObjectId = objectId;
		Log.debug(DebugOptions.shopkeeperActivation, () -> shopkeeper.getLogPrefix()
				+ "Registering object with id '" + finalObjectId + "'.");

		AbstractShopkeeper otherShopkeeper = shopkeepersByObjectId.putIfAbsent(objectId, shopkeeper);
		assert otherShopkeeper != shopkeeper; // We unregistered the shopkeeper above
		if (otherShopkeeper != null) {
			Log.warning(shopkeeper.getLogPrefix() + "Object registration failed! Object id '"
					+ objectId + "' is already used by shopkeeper " + otherShopkeeper.getId() + ".");
			return;
		} else {
			shopObject.setLastId(objectId); // Remember the object id
		}
	}

	private void unregisterShopObject(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		AbstractShopObject shopObject = shopkeeper.getShopObject();
		Object objectId = shopObject.getLastId(); // Can be null
		if (objectId == null) return; // Already not registered

		Log.debug(DebugOptions.shopkeeperActivation, () -> shopkeeper.getLogPrefix()
				+ "Unregistering object with id '" + objectId + "'.");
		assert shopkeepersByObjectId.get(objectId) == shopkeeper;
		shopkeepersByObjectId.remove(objectId);
		shopObject.setLastId(null);
	}
}
