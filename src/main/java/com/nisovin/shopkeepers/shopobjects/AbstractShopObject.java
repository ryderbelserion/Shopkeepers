package com.nisovin.shopkeepers.shopobjects;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;

/**
 * Abstract base class for all shop object implementations.
 * <p>
 * Implementation hints:<br>
 * <ul>
 * <li>Make sure to call {@link Shopkeeper#markDirty()} on every change of data that might need to be persisted.
 * </ul>
 */
public abstract class AbstractShopObject implements ShopObject {

	protected final AbstractShopkeeper shopkeeper; // Not null
	private Object lastId = null;
	private boolean tickActivity = false;

	// Fresh creation
	protected AbstractShopObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
	}

	@Override
	public abstract AbstractShopObjectType<?> getType();

	public void load(ConfigurationSection configSection) {
	}

	/**
	 * Saves the shop object's data to the specified configuration section.
	 * <p>
	 * Note: The serialization of the inserted data may happen asynchronously, so make sure that this is not a problem
	 * (ex. only insert immutable objects, or always create copies of the data you insert and/or make sure to not modify
	 * the inserted objects).
	 * 
	 * @param configSection
	 *            the config section
	 */
	public void save(ConfigurationSection configSection) {
		configSection.set("type", this.getType().getIdentifier());
	}

	/**
	 * This gets called at the end of shopkeeper construction, when the shopkeeper has been loaded and setup.
	 * <p>
	 * The shopkeeper has not yet been registered at this point!
	 * <p>
	 * This can be used to perform any remaining initial shop object setup.
	 */
	public void setup() {
	}

	// LIFE CYCLE

	/**
	 * This gets called when the {@link ShopObject} is meant to be removed.
	 * <p>
	 * This can for example be used to disable any active components (ex. listeners) for this shop object.
	 */
	public void remove() {
	}

	/**
	 * This gets called when the {@link ShopObject} is meant to be permanently deleted.
	 * <p>
	 * This gets called after {@link #remove()}.
	 * <p>
	 * This can for example be used to cleanup any persistent data corresponding to this shop object.
	 */
	public void delete() {
	}

	// ACTIVATION

	public void onChunkActivation() {
	}

	public void onChunkDeactivation() {
	}

	@Override
	public abstract boolean isActive();

	/**
	 * Gets an object that uniquely identifies this {@link ShopObject} while it is {@link #isActive() active}.
	 * <p>
	 * The id has to be unique among all currently active shop objects, including other types of shop objects. It has be
	 * be suitable to be used as key in {@link Object#hashCode() hash-based} data structures.
	 * <p>
	 * The returned id may only be valid while the shop object is active, and it may change whenever the shop object is
	 * respawned.
	 * 
	 * @return the id of the shop object, or possibly (but not necessarily) <code>null</code> if it is not active
	 *         currently
	 */
	public abstract Object getId();

	/**
	 * This has to be invoked whenever the id of this shop object has changed.
	 * <p>
	 * This is not required to be called if the object id changes due to spawning or during ticking.
	 * <p>
	 * This updates the id by which the shopkeeper is currently stored by inside the shopkeeper registry.
	 */
	protected final void onIdChanged() {
		SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().onShopkeeperObjectIdChanged(shopkeeper);
	}

	/**
	 * Gets the object id the shopkeeper is currently stored by inside the shopkeeper registry.
	 * 
	 * @return the object id, or <code>null</code>
	 */
	public final Object getLastId() {
		return lastId;
	}

	/**
	 * Sets the object id the shopkeeper is currently stored by inside the shopkeeper registry.
	 * 
	 * @param lastId
	 *            the object id, can be <code>null</code>
	 */
	public final void setLastId(Object lastId) {
		this.lastId = lastId; // can be null
	}

	/**
	 * Spawns the shop object into the world at its spawn location.
	 * 
	 * @return <code>true</code> on success
	 */
	public abstract boolean spawn();

	/**
	 * Removes this shop object from the world.
	 * <p>
	 * This has no effect if the shop object is not spawned currently.
	 */
	public abstract void despawn();

	@Override
	public abstract Location getLocation();

	/**
	 * Gets the location at which particles for the shopkeeper's tick visualization shall be spawned.
	 * 
	 * @return the location, or possibly (but not necessarily) <code>null</code> if the shop object is not active
	 *         currently, or if it does not support the tick visualization
	 */
	public abstract Location getTickVisualizationParticleLocation();

	// TICKING

	/**
	 * This is called periodically (roughly once per second) for shopkeepers in active chunks.
	 * <p>
	 * This can for example be used to check if everything is still alright with the shop object, such as if it still
	 * exists and if it is still in its expected location and state. If any of these checks fail, the shop object may be
	 * respawned, teleported back into place, and brought back into its expected state.
	 * <p>
	 * This is also called for shop objects that manage their spawning and despawning
	 * {@link AbstractShopObjectType#isSpawnedWithChunks() manually}.
	 * <p>
	 * Any changes to the shopkeeper's activation state or {@link AbstractShopObject#getId() shop object id} may only be
	 * processed after the ticking of all currently ticked shop objects completes.
	 * <p>
	 * If the checks to perform are potentially heavy or not required to happen every second, the shop object may decide
	 * to only run it every X invocations.
	 * <p>
	 * The ticking of shop objects in active chunks may be spread across multiple ticks and may therefore not happen for
	 * all shopkeepers within the same tick.
	 * <p>
	 * If any of the shopkeepers whose shop objects are ticked are marked as {@link Shopkeeper#isDirty() dirty}, a
	 * {@link ShopkeeperStorage#saveDelayed() delayed save} will subsequently be triggered.
	 * <p>
	 * If you are overriding this method, consider calling the parent class version of this method.
	 */
	public void tick() {
		// Reset activity indicator:
		tickActivity = false;
	}

	/**
	 * Optional: Subclasses can call this method to indicate activity during their last tick. If the tick visualization
	 * is enabled, this will result in a default visualization of that ticking activity.
	 */
	protected void indicateTickActivity() {
		tickActivity = true;
	}

	/**
	 * Visualizes the shop object's activity during the last tick.
	 */
	public void visualizeLastTick() {
		// Default visualization:
		if (!tickActivity) return;

		Location particleLocation = this.getTickVisualizationParticleLocation();
		if (particleLocation == null) return;

		assert particleLocation.isWorldLoaded();
		World world = particleLocation.getWorld();
		assert world != null;
		world.spawnParticle(Particle.VILLAGER_ANGRY, particleLocation, 1);
	}

	// NAMING

	@Override
	public int getNameLengthLimit() {
		return AbstractShopkeeper.MAX_NAME_LENGTH;
	}

	@Override
	public String prepareName(String name) {
		if (name == null) return null;
		// Trim to max name length:
		int lengthLimit = this.getNameLengthLimit();
		if (name.length() > lengthLimit) name = name.substring(0, lengthLimit);
		return name;
	}

	@Override
	public abstract void setName(String name);

	@Override
	public abstract String getName();

	// PLAYER SHOP OWNER

	/**
	 * This is called by {@link PlayerShopkeeper player shopkeepers} when their owner changes.
	 */
	public void onShopOwnerChanged() {
	}

	// EDITOR ACTIONS

	public List<EditorHandler.Button> getEditorButtons() {
		return Collections.emptyList(); // None by default
	}
}
