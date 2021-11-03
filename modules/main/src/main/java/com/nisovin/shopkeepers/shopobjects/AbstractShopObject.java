package com.nisovin.shopkeepers.shopobjects;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperPropertyValuesHolder;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorHandler;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.validation.java.StringValidators;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValuesHolder;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Abstract base class for all shop object implementations.
 * <p>
 * Implementation hints:<br>
 * <ul>
 * <li>Make sure to call {@link AbstractShopkeeper#markDirty()} on every change of data that might need to be persisted.
 * </ul>
 */
public abstract class AbstractShopObject implements ShopObject {

	private static final String DATA_KEY_SHOP_OBJECT_TYPE = "type";

	/**
	 * Shop object type id.
	 */
	public static final Property<String> SHOP_OBJECT_TYPE_ID = new BasicProperty<String>()
			.name("type-id")
			.dataKeyAccessor(DATA_KEY_SHOP_OBJECT_TYPE, StringSerializers.STRICT)
			.validator(StringValidators.NON_EMPTY)
			.build();

	/**
	 * Shop object type, derived from the serialized {@link #SHOP_OBJECT_TYPE_ID shop object type id}.
	 */
	public static final Property<AbstractShopObjectType<?>> SHOP_OBJECT_TYPE = new BasicProperty<AbstractShopObjectType<?>>()
			.dataKeyAccessor(DATA_KEY_SHOP_OBJECT_TYPE, new DataSerializer<AbstractShopObjectType<?>>() {
				@Override
				public Object serialize(AbstractShopObjectType<?> value) {
					Validate.notNull(value, "value is null");
					return value.getIdentifier();
				}

				@Override
				public AbstractShopObjectType<?> deserialize(Object data) throws InvalidDataException {
					String shopObjectTypeId = StringSerializers.STRICT_NON_EMPTY.deserialize(data);
					AbstractShopObjectType<?> shopObjectType = SKShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().get(shopObjectTypeId);
					if (shopObjectType == null) {
						throw new InvalidDataException("Unknown shop object type: " + shopObjectTypeId);
					}
					return shopObjectType;
				}
			})
			.build();

	protected final AbstractShopkeeper shopkeeper; // Not null
	protected final PropertyValuesHolder properties; // Not null

	private Object lastId = null;
	private boolean tickActivity = false;

	// Fresh creation
	protected AbstractShopObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		assert shopkeeper != null;
		this.shopkeeper = shopkeeper;
		this.properties = new ShopkeeperPropertyValuesHolder(shopkeeper);
	}

	@Override
	public abstract AbstractShopObjectType<?> getType();

	/**
	 * Gets the shopkeeper associated with this shop object.
	 * 
	 * @return the shopkeeper, not <code>null</code>
	 */
	public final AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}

	/**
	 * Loads the shop object's data from the given {@link ShopObjectData}.
	 * <p>
	 * The data is expected to already have been {@link ShopkeeperData#migrate(String) migrated}.
	 * <p>
	 * The given shop object data is expected to contain the shop object's type identifier. Loading fails if the given
	 * data was originally meant for a different shop object type.
	 * <p>
	 * Any stored data elements (such as for example item stacks, etc.) and collections of data elements are assumed to
	 * not be modified, neither by the shop object, nor in contexts outside of the shop object. If the shop object can
	 * guarantee not to modify these data elements, it is allowed to directly store them without copying them first.
	 * 
	 * @param shopObjectData
	 *            the shop object data, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the data cannot be loaded
	 */
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		Validate.notNull(shopObjectData, "shopObjectData is null");
		ShopObjectType<?> shopObjectType = shopObjectData.get(SHOP_OBJECT_TYPE);
		assert shopObjectType != null;
		if (shopObjectType != this.getType()) {
			throw new InvalidDataException("Shop object data is for a different shop object type (expected: "
					+ this.getType().getIdentifier() + ", got: " + shopObjectType.getIdentifier() + ")!");
		}
	}

	/**
	 * Saves the shop object's data to the given {@link ShopObjectData}.
	 * <p>
	 * It is assumed that the data stored in the given {@link ShopObjectData} does not change afterwards and can be
	 * serialized asynchronously. The shop object must therefore ensure that this data is not modified, for example by
	 * only inserting immutable data, or always making copies of the inserted data.
	 * 
	 * @param shopObjectData
	 *            the shop object data, not <code>null</code>
	 */
	public void save(ShopObjectData shopObjectData) {
		Validate.notNull(shopObjectData, "shopObjectData is null");
		shopObjectData.set("type", this.getType().getIdentifier());
	}

	/**
	 * This is called at the end of shopkeeper construction, when the shopkeeper has been fully loaded and setup, and
	 * can be used to perform any remaining initial shop object setup.
	 * <p>
	 * The shopkeeper has not yet been registered at this point! If the registration fails, or if the shopkeeper is
	 * created for some other purpose, the {@link #remove()} and {@link #delete()} methods may never get called for this
	 * shop object. For any setup that relies on cleanup during {@link #remove()} or {@link #delete()},
	 * {@link #onShopkeeperAdded(ShopkeeperAddedEvent.Cause)} may be better suited.
	 */
	public void setup() {
	}

	// LIFE CYCLE

	/**
	 * This is called when the shopkeeper is added to the {@link ShopkeeperRegistry}.
	 * <p>
	 * The shopkeeper has not yet been spawned or activated at this point.
	 * 
	 * @param cause
	 *            the cause of the addition
	 */
	public void onShopkeeperAdded(ShopkeeperAddedEvent.Cause cause) {
	}

	/**
	 * This is called when the {@link ShopObject} is removed, usually when the corresponding shopkeeper is removed from
	 * the {@link ShopkeeperRegistry}.
	 * <p>
	 * This can for example be used to disable any active components (ex. listeners) for this shop object.
	 */
	public void remove() {
	}

	/**
	 * This is called when the {@link ShopObject} is permanently deleted.
	 * <p>
	 * This is called after {@link #remove()}.
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
	 * Gets an object that uniquely identifies this {@link ShopObject} while it is {@link #isSpawned() spawned}.
	 * <p>
	 * The id has to be unique among all currently spawned shop objects, including other types of shop objects. It has
	 * to be suitable to be used as key in {@link Object#hashCode() hash-based} data structures, and not change while
	 * the shop object is spawned. The id may change whenever the shop object is respawned.
	 * 
	 * @return the id of the shop object, or <code>null</code> if it is not spawned currently
	 */
	public abstract Object getId();

	/**
	 * This has to be invoked whenever the id of this shop object might have changed.
	 * <p>
	 * This updates the id by which the shopkeeper is currently stored inside the shopkeeper registry.
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
	 * <p>
	 * This may have no effect if the shop object has already been spawned. To respawn this shop object if is is
	 * currently already spawned, one can use {@link #respawn()}.
	 * <p>
	 * This needs to call {@link #onIdChanged()} if the shop object was successfully spawned.
	 * 
	 * @return <code>false</code> if the spawning failed, or <code>true</code> if the shop object either is already
	 *         spawned or has successfully been spawned
	 */
	public abstract boolean spawn();

	/**
	 * Removes this shop object from the world.
	 * <p>
	 * This has no effect if the shop object is not spawned currently.
	 * <p>
	 * This needs to call {@link #onIdChanged()} if the shop object was successfully despawned.
	 */
	public abstract void despawn();

	/**
	 * Respawns this shop object.
	 * <p>
	 * This is the same as calling both {@link #despawn()} and then {@link #spawn()}, but only if this shop object is
	 * currently already {@link #isSpawned() spawned}.
	 * 
	 * @return <code>true</code> if the shop object was successfully respawned
	 */
	public final boolean respawn() {
		if (!this.isSpawned()) return false;
		this.despawn();
		return this.spawn();
	}

	@Override
	public abstract Location getLocation();

	/**
	 * Gets the location at which particles for the shopkeeper's tick visualization shall be spawned.
	 * 
	 * @return the location, or possibly (but not necessarily) <code>null</code> if the shop object is not spawned
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
	 * {@link AbstractShopObjectType#mustBeSpawned() manually}.
	 * <p>
	 * Any changes to the shopkeeper's activation state or {@link AbstractShopObject#getId() shop object id} might only
	 * be processed after the ticking of all currently ticked shop objects completes.
	 * <p>
	 * If the checks to perform are potentially heavy or not required to happen every second, the shop object may decide
	 * to only run it every X invocations.
	 * <p>
	 * The ticking of shop objects in active chunks may be spread across multiple ticks and may therefore not happen for
	 * all shopkeepers within the same tick.
	 * <p>
	 * If any of the shopkeepers whose shop objects are ticked are marked as {@link AbstractShopkeeper#isDirty() dirty},
	 * a {@link ShopkeeperStorage#saveDelayed() delayed save} will subsequently be triggered.
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

	/**
	 * Creates the editor buttons for editing this shop object.
	 * <p>
	 * This is usually only invoked once, when the {@link EditorHandler} is setup for the shopkeeper. So it is not
	 * possible to dynamically add or remove buttons with this method.
	 * <p>
	 * In order to allow for subtypes to more easily add or modify the returned editor buttons, this method is expected
	 * to return a new modifiable list with each invocation.
	 * 
	 * @return the editor buttons
	 */
	public List<Button> createEditorButtons() {
		return new ArrayList<>(); // None by default, modifiable by subtypes
	}
}
