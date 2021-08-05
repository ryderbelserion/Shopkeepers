package com.nisovin.shopkeepers.shopkeeper;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObject;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObjectType;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.compat.MC_1_16_Utils;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.types.CatShop;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.trading.TradingHandler;
import com.nisovin.shopkeepers.util.bukkit.ColorUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;
import com.nisovin.shopkeepers.util.text.MessageArguments;

/**
 * Abstract base class for all shopkeeper implementations.
 * <p>
 * Implementation hints:<br>
 * <ul>
 * <li>Make sure to follow the initialization instructions outlined in the constructor description.
 * <li>Make sure to call {@link #markDirty()} on every change of data that might need to be persisted.
 * </ul>
 */
public abstract class AbstractShopkeeper implements Shopkeeper {

	/**
	 * The ticking period of active shopkeepers and shop objects in ticks.
	 */
	public static final int TICKING_PERIOD_TICKS = 20; // 1 second
	/**
	 * For load balancing purposes, shopkeepers are ticked in groups.
	 * <p>
	 * This number is chosen as a balance between {@code 1} group (all shopkeepers are ticked within the same tick; no
	 * load balancing), and the maximum of {@code 20} groups (groups are as small as possible for the tick rate of once
	 * every second, i.e. once every {@code 20} ticks; best load balancing; but this is associated with a large overhead
	 * due to having to iterate the active shopkeepers each Minecraft tick).
	 * <p>
	 * With {@code 4} ticking groups, the active shopkeepers are iterated once every {@code 5} ticks, and one fourth of
	 * them are actually processed.
	 */
	public static final int TICKING_GROUPS = 4;
	private static final CyclicCounter nextTickingGroup = new CyclicCounter(TICKING_GROUPS);

	// The maximum supported name length:
	// The actual maximum name length that can be used might be lower depending on config settings
	// and on shop object specific limits.
	public static final int MAX_NAME_LENGTH = 128;

	// Shopkeeper tick visualization:
	// Particles of different colors indicate the different ticking groups.
	// Note: The client seems to randomly change the color slightly each time a dust particle is spawned.
	// Note: The particle size also determines the effect duration.
	private static final DustOptions[] TICK_VISUALIZATION_DUSTS = new DustOptions[AbstractShopkeeper.TICKING_GROUPS];
	static {
		// Even distribution of colors in the HSB color space: Ensures a distinct color for each ticking group.
		float hueStep = (1.0F / AbstractShopkeeper.TICKING_GROUPS);
		for (int i = 0; i < AbstractShopkeeper.TICKING_GROUPS; ++i) {
			float hue = i * hueStep; // Starts with red
			int rgb = ColorUtils.HSBtoRGB(hue, 1.0F, 1.0F);
			Color color = Color.fromRGB(rgb);
			TICK_VISUALIZATION_DUSTS[i] = new DustOptions(color, 1.0F);
		}
	}

	// This is called on plugin enable and can be used to setup or reset any initial static state.
	static void setupOnEnable() {
		// Resetting the ticking group counter ensures that shopkeepers retain their ticking group across reloads (if
		// there are no changes in the order of the loaded shopkeepers). This ensures that the particle colors of our
		// tick visualization remain the same across reloads (avoids possible confusion for users).
		nextTickingGroup.reset();
	}

	private final int id;
	private UUID uniqueId; // Not null after initialization
	private AbstractShopObject shopObject; // Not null after initialization
	// TODO Move location information into ShopObject?
	private String worldName; // Not empty, null for virtual shops
	private int x;
	private int y;
	private int z;
	private float yaw;

	private ChunkCoords chunkCoords; // Null for virtual shops
	// The ChunkCoords under which the shopkeeper is currently stored:
	private ChunkCoords lastChunkCoords = null;
	private String name = ""; // Not null, can be empty

	// Map of dynamically evaluated message arguments:
	private final Map<String, Supplier<Object>> messageArgumentsMap = new HashMap<>();
	private final MessageArguments messageArguments = MessageArguments.ofMap(messageArgumentsMap);

	// Whether there have been changes to the shopkeeper's data that the storage is not yet aware of. A value of 'false'
	// only indicates that the storage is aware of the latest data of the shopkeeper, not that it has actually persisted
	// the data to disk yet.
	private boolean dirty = false;
	// Is currently registered:
	private boolean valid = false;

	// UI type identifier -> UI handler
	private final Map<String, UIHandler> uiHandlers = new HashMap<>();

	// Internally used for load balancing purposes:
	private final int tickingGroup = nextTickingGroup.getAndIncrement();

	// CONSTRUCTION AND SETUP

	/**
	 * Creates a shopkeeper.
	 * <p>
	 * Important: Depending on whether the shopkeeper gets freshly created or loaded, either
	 * {@link #initOnCreation(ShopCreationData)} or {@link #initOnLoad(ConfigurationSection)} need to be called to
	 * complete the initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected AbstractShopkeeper(int id) {
		this.id = id;
	}

	/**
	 * Initializes the shopkeeper by using the data from the given {@link ShopCreationData}.
	 * 
	 * @param shopCreationData
	 *            the shop creation data
	 * @throws ShopkeeperCreateException
	 *             in case the shopkeeper could not be created
	 */
	protected final void initOnCreation(ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.loadFromCreationData(shopCreationData);
		this.commonSetup();
	}

	/**
	 * Initializes the shopkeeper by loading its previously saved data from the given config section.
	 * <p>
	 * No assumptions are made regarding whether or not the given config section and its sub sections are mutable.
	 * However, any other stored data elements (such as for example item stacks, etc.) and collections of data elements
	 * are assumed to be immutable and the loaded shopkeeper may therefore directly store these elements without copying
	 * them first.
	 * 
	 * @param configSection
	 *            the config section
	 * @throws ShopkeeperCreateException
	 *             in case the shopkeeper could not be loaded
	 */
	protected final void initOnLoad(ConfigurationSection configSection) throws ShopkeeperCreateException {
		this.loadFromSaveData(configSection);
		this.commonSetup();
	}

	private void commonSetup() {
		this.setup();
		this.postSetup();
	}

	/**
	 * Initializes the shopkeeper by using the data from the given {@link ShopCreationData}.
	 * 
	 * @param shopCreationData
	 *            the shop creation data
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper cannot be properly initialized
	 */
	protected void loadFromCreationData(ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		assert shopCreationData != null;
		this.uniqueId = UUID.randomUUID();

		ShopObjectType<?> shopObjectType = shopCreationData.getShopObjectType();
		Validate.isTrue(shopObjectType instanceof AbstractShopObjectType,
				"Expecting an AbstractShopObjectType, got " + shopObjectType.getClass().getName());

		if (shopObjectType instanceof VirtualShopObjectType) {
			// Virtual shops ignore any potentially available spawn location:
			this.worldName = null;
			this.x = 0;
			this.y = 0;
			this.z = 0;
			this.yaw = 0.0F;
		} else {
			Location spawnLocation = shopCreationData.getSpawnLocation();
			assert spawnLocation != null && spawnLocation.getWorld() != null;
			this.worldName = spawnLocation.getWorld().getName();
			this.x = spawnLocation.getBlockX();
			this.y = spawnLocation.getBlockY();
			this.z = spawnLocation.getBlockZ();
			this.yaw = spawnLocation.getYaw();
		}
		this.updateChunkCoords();

		this.shopObject = this.createShopObject((AbstractShopObjectType<?>) shopObjectType, shopCreationData);

		// Automatically mark new shopkeepers as dirty:
		this.markDirty();
	}

	/**
	 * This is called at the end of construction, after the shopkeeper data has been loaded, and can be used to perform
	 * any remaining setup.
	 * <p>
	 * This might setup defaults for some things, if not yet specified by the sub-classes. So if you are overriding this
	 * method, consider doing your own setup before calling the overridden method. And also take into account that
	 * further sub-classes might perform their setup prior to calling your setup method as well. So don't replace any
	 * components that have already been setup by further sub-classes.
	 * <p>
	 * The shopkeeper has not yet been registered at this point! If the registration fails, or if the shopkeeper is
	 * created for some other purpose, the {@link #onRemoval(ShopkeeperRemoveEvent.Cause)} and {@link #onDeletion()}
	 * methods may never get called for this shopkeeper. For any setup that relies on cleanup during
	 * {@link #onRemoval(ShopkeeperRemoveEvent.Cause)} or {@link #onDeletion()},
	 * {@link #onAdded(ShopkeeperAddedEvent.Cause)} may be better suited.
	 */
	protected void setup() {
		// Add a default trading handler, if none is provided:
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new TradingHandler(SKDefaultUITypes.TRADING(), this));
		}
	}

	/**
	 * This gets called after {@link #setup()} and might be used to perform any setup that is intended to definitely
	 * happen last.
	 */
	protected void postSetup() {
		// Inform shop object:
		this.getShopObject().setup();
	}

	// STORAGE

	/**
	 * Loads the shopkeeper's saved data from the given config section.
	 * <p>
	 * No assumptions are made regarding whether or not the given config section and its sub sections are mutable.
	 * However, any other stored data elements (such as for example item stacks, etc.) and collections of data elements
	 * are assumed to be immutable and the loaded shopkeeper may therefore directly store these elements without copying
	 * them first.
	 * 
	 * @param configSection
	 *            the config section
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper cannot be properly loaded
	 */
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		String uniqueIdString = configSection.getString("uniqueId", "");
		try {
			this.uniqueId = UUID.fromString(uniqueIdString);
		} catch (IllegalArgumentException e) {
			if (!uniqueIdString.isEmpty()) {
				Log.warning("Shopkeeper '" + id + "' has an invalid unique id '" + uniqueIdString + "'. Creating a new one.");
			}
			this.uniqueId = UUID.randomUUID();
			this.markDirty();
		}

		this.name = this.trimName(TextUtils.colorize(configSection.getString("name", "")));

		// Shop object:
		String objectTypeId;
		ConfigurationSection objectSection = configSection.getConfigurationSection("object");
		if (objectSection == null) {
			// Load from legacy data:
			// TODO Remove again at some point.
			objectTypeId = configSection.getString("object");
			objectSection = configSection;
			this.markDirty();
		} else {
			objectTypeId = objectSection.getString("type");
		}

		// Convert legacy object identifiers:
		if (objectTypeId != null) {
			// 'block' -> 'sign'
			if (objectTypeId.equalsIgnoreCase("block")) {
				objectTypeId = "sign";
				this.markDirty();
			}

			// Normalize:
			String normalizedOjectTypeId = StringUtils.normalize(objectTypeId);
			if (!normalizedOjectTypeId.equals(objectTypeId)) {
				objectTypeId = normalizedOjectTypeId;
				this.markDirty();
			}

			// MC 1.14:
			// Convert ocelots to cats:
			if (objectTypeId.equals("ocelot")) {
				String ocelotType = objectSection.getString("catType");
				if (ocelotType != null) {
					if (ocelotType.equals("WILD_OCELOT")) {
						// Stays an ocelot, but remove cat type data:
						objectSection.set("catType", null);
						this.markDirty();
					} else {
						// Convert to cat:
						objectTypeId = "cat";
						String catType = CatShop.fromOcelotType(ocelotType).name();
						objectSection.set("catType", catType);
						this.markDirty();
						Log.warning("Migrated ocelot shopkeeper '" + id + "' of type '" + ocelotType
								+ "' to cat shopkeeper of type '" + catType + "'.");
					}
				} // Else: Stays ocelot.
			}

			// MC 1.16:
			// Convert pig-zombie to zombified-piglin (but only if we run on MC 1.16 or above):
			if (MC_1_16_Utils.getZombifiedPiglin() != null && objectTypeId.equals("pig-zombie")) {
				objectTypeId = "zombified-piglin";
				Log.warning("Migrated object type of shopkeeper '" + id + "' from 'pig-zombie' to 'zombified-piglin'.");
				this.markDirty();
			}
		}

		AbstractShopObjectType<?> objectType = SKShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().get(objectTypeId);
		if (objectType == null) {
			// Couldn't find object type by id, try to find object type via matching:
			objectType = SKShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().match(objectTypeId);
			if (objectType != null) {
				// Mark dirty, so the correct id gets saved:
				this.markDirty();
			} else {
				throw new ShopkeeperCreateException("Invalid object type for shopkeeper '" + id + "': " + objectTypeId);
			}
		}
		assert objectType != null;

		// Normalize empty world name to null:
		String storedWorldName = StringUtils.getNotEmpty(configSection.getString("world"));
		int storedX = configSection.getInt("x");
		int storedY = configSection.getInt("y");
		int storedZ = configSection.getInt("z");
		float storedYaw = (float) configSection.getDouble("yaw"); // 0 (south) if missing (eg. in pre 2.13.4 versions)

		if (objectType instanceof VirtualShopObjectType) {
			if (storedWorldName != null || storedX != 0 || storedY != 0 || storedZ != 0 || storedYaw != 0.0F) {
				Log.warning("Ignoring stored location ("
						+ TextUtils.getLocationString(StringUtils.getOrEmpty(storedWorldName), storedX, storedY, storedZ, storedYaw)
						+ ") for virtual shopkeeper '" + id + "'!");
				this.markDirty();
			}
			this.worldName = null;
			this.x = 0;
			this.y = 0;
			this.z = 0;
			this.yaw = 0.0F;
		} else {
			if (storedWorldName == null) {
				throw new ShopkeeperCreateException("Missing world name for shopkeeper '" + id + "'!");
			}
			this.worldName = storedWorldName;
			this.x = storedX;
			this.y = storedY;
			this.z = storedZ;
			this.yaw = storedYaw;
		}
		this.updateChunkCoords();

		this.shopObject = this.createShopObject(objectType, null);
		this.shopObject.load(objectSection);
	}

	// shopCreationData can be null if the shopkeeper is getting loaded.
	private AbstractShopObject createShopObject(AbstractShopObjectType<?> objectType, ShopCreationData shopCreationData) {
		assert objectType != null;
		AbstractShopObject shopObject = objectType.createObject(this, shopCreationData);
		Validate.State.notNull(shopObject, "Shop object type '" + objectType.getIdentifier() + "' created null shop object for shopkeeper '" + id + "'!");
		return shopObject;
	}

	/**
	 * Saves the shopkeeper's data to the specified configuration section.
	 * <p>
	 * Note: The serialization of the inserted data may happen asynchronously, so make sure that this is not a problem
	 * (ex. only insert immutable objects, or always create copies of the data you insert and/or make sure to not modify
	 * the inserted objects).
	 * 
	 * @param configSection
	 *            the config section
	 */
	public void save(ConfigurationSection configSection) {
		configSection.set("uniqueId", uniqueId.toString());
		configSection.set("name", TextUtils.decolorize(name));
		// Null world name gets stored as empty string:
		configSection.set("world", StringUtils.getOrEmpty(worldName));
		configSection.set("x", x);
		configSection.set("y", y);
		configSection.set("z", z);
		configSection.set("yaw", yaw);
		configSection.set("type", this.getType().getIdentifier());

		// Shop object:
		ConfigurationSection objectSection = configSection.createSection("object");
		shopObject.save(objectSection);
	}

	@Override
	public final void save() {
		this.markDirty();
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}

	@Override
	public final void saveDelayed() {
		this.markDirty();
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().saveDelayed();
	}

	/**
	 * Marks this shopkeeper as 'dirty'.
	 * <p>
	 * This indicates that there have been changes to the shopkeeper's data that the storage is not yet aware of. The
	 * shopkeeper and shop object implementations have to invoke this on every change of data that needs to be
	 * persisted.
	 * <p>
	 * If this shopkeeper is currently {@link #isValid() loaded}, or about to be loaded, its data is saved with the next
	 * successful save of the {@link ShopkeeperStorage}. If the shopkeeper has already been deleted or unloaded,
	 * invoking this method will have no effect on the data that is stored by the storage.
	 */
	public final void markDirty() {
		dirty = true;
		// Inform the storage that the shopkeeper is dirty:
		if (this.isValid()) {
			// If the shopkeeper is marked as dirty during creation or loading (while it is not yet valid), the storage
			// is informed once the shopkeeper becomes valid.
			SKShopkeepersPlugin.getInstance().getShopkeeperStorage().markDirty(this);
		}
	}

	/**
	 * Checks whether this shopkeeper had changes to its data that the storage is not yet aware of.
	 * <p>
	 * A return value of {@code false} indicates that the {@link ShopkeeperStorage} is aware of the shopkeeper's latest
	 * data, but not necessarily that this data has already been successfully persisted to disk.
	 * 
	 * @return <code>true</code> if there are data changes that the storage is not yet aware of
	 */
	public final boolean isDirty() {
		return dirty;
	}

	// Called by shopkeeper storage when it has retrieved the shopkeeper's latest data for the next save. The data might
	// not yet have been persisted at that point.
	// This may not be called if the shopkeeper was deleted.
	public final void onSave() {
		dirty = false;
	}

	// LIFE CYCLE

	@Override
	public boolean isValid() {
		return valid;
	}

	public final void informAdded(ShopkeeperAddedEvent.Cause cause) {
		assert !valid;
		valid = true;

		// If the shopkeeper has been marked as dirty earlier (eg. due to data migrations during loading, or when being
		// newly created), we inform the storage here:
		if (this.isDirty()) {
			this.markDirty();
		}

		// Custom processing done by sub-classes:
		this.onAdded(cause);
	}

	/**
	 * This is called when the shopkeeper is added to the {@link ShopkeeperRegistry}.
	 * <p>
	 * The shopkeeper has not yet been spawned or activated at this point.
	 * 
	 * @param cause
	 *            the cause of the addition
	 */
	protected void onAdded(ShopkeeperAddedEvent.Cause cause) {
		shopObject.onShopkeeperAdded(cause);
	}

	public final void informRemoval(ShopkeeperRemoveEvent.Cause cause) {
		assert valid;
		this.onRemoval(cause);
		if (cause == ShopkeeperRemoveEvent.Cause.DELETE) {
			this.onDeletion();
		}
		valid = false;
	}

	/**
	 * This is called once the shopkeeper is about to be removed from the {@link ShopkeeperRegistry}.
	 * <p>
	 * The shopkeeper has already been deactivated at this point.
	 * 
	 * @param cause
	 *            the cause for the removal
	 */
	protected void onRemoval(ShopkeeperRemoveEvent.Cause cause) {
		shopObject.remove();
	}

	@Override
	public void delete() {
		this.delete(null);
	}

	// TODO Make this final and provide the involved player to the onDeletion method somehow.
	@Override
	public void delete(Player player) {
		SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().deleteShopkeeper(this);
	}

	/**
	 * This is called if the shopkeeper is about to be removed due to permanent deletion.
	 * <p>
	 * This is called after {@link #onRemoval(ShopkeeperRemoveEvent.Cause)}.
	 */
	protected void onDeletion() {
		shopObject.delete();
	}

	// ATTRIBUTES

	@Override
	public int getId() {
		return id;
	}

	@Override
	public UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public String getIdString() {
		return id + " (" + uniqueId.toString() + ")";
	}

	@Override
	public abstract AbstractShopType<?> getType();

	@Override
	public final boolean isVirtual() {
		assert (worldName != null) ^ (shopObject instanceof VirtualShopObject); // xor
		return (worldName == null);
	}

	@Override
	public String getWorldName() {
		return worldName;
	}

	@Override
	public int getX() {
		return x;
	}

	@Override
	public int getY() {
		return y;
	}

	@Override
	public int getZ() {
		return z;
	}

	@Override
	public float getYaw() {
		return yaw;
	}

	@Override
	public String getPositionString() {
		if (worldName == null) return "[virtual]";
		return TextUtils.getLocationString(worldName, x, y, z);
	}

	@Override
	public Location getLocation() {
		if (worldName == null) return null;
		World world = Bukkit.getWorld(worldName);
		if (world == null) return null;
		return new Location(world, x, y, z, yaw, 0.0F);
	}

	/**
	 * Sets the stored location of this shopkeeper.
	 * <p>
	 * This will not actually move the shop object on its own, until the next time it is spawned or teleported to its
	 * new location.
	 * 
	 * @param location
	 *            the new stored location of this shopkeeper
	 */
	public void setLocation(Location location) {
		Validate.isTrue(!this.isVirtual(), "Cannot set location of virtual shopkeeper!");
		Validate.notNull(location, "Location is null!");
		World world = location.getWorld();
		Validate.notNull(world, "Location's world is null!");

		// TODO Changing the world is not safe (at least not for all types of shops)! Consider for example player shops
		// which currently use the world name to locate their container.
		worldName = world.getName();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
		yaw = location.getYaw();
		this.updateChunkCoords();
		this.markDirty();

		// Update shopkeeper in chunk map:
		SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().onShopkeeperMoved(this);
	}

	/**
	 * Sets the yaw of this shopkeeper.
	 * <p>
	 * This will not automatically rotate the shop object until the next time it is spawned or teleported.
	 * 
	 * @param yaw
	 *            the new yaw
	 */
	public void setYaw(float yaw) {
		Validate.isTrue(!this.isVirtual(), "Cannot set the yaw of a virtual shopkeeper!");
		this.yaw = yaw;
		this.markDirty();
	}

	@Override
	public ChunkCoords getChunkCoords() {
		return chunkCoords;
	}

	private void updateChunkCoords() {
		this.chunkCoords = this.isVirtual() ? null : ChunkCoords.fromBlock(worldName, x, z);
	}

	/**
	 * Gets the {@link ChunkCoords} under which the shopkeeper is currently stored.
	 * <p>
	 * Internal use only!
	 * 
	 * @return the chunk coordinates
	 */
	public final ChunkCoords getLastChunkCoords() {
		return lastChunkCoords;
	}

	/**
	 * Update the {@link ChunkCoords} for which the shopkeeper is currently stored.
	 * <p>
	 * Internal use only!
	 * 
	 * @param chunkCoords
	 *            the chunk coordinates
	 */
	public final void setLastChunkCoords(ChunkCoords chunkCoords) {
		this.lastChunkCoords = chunkCoords;
	}

	/**
	 * Gets the {@link MessageArguments} for this shopkeeper.
	 * <p>
	 * The provided message arguments may be {@link Supplier Suppliers} that lazily and dynamically calculate the actual
	 * message arguments only when they are requested.
	 * 
	 * @param contextPrefix
	 *            this prefix is added in front of all message keys, not <code>null</code>, but may be empty
	 * @return the message arguments
	 */
	public final MessageArguments getMessageArguments(String contextPrefix) {
		// Lazily populated map of message argument suppliers:
		if (messageArgumentsMap.isEmpty()) {
			this.populateMessageArguments(messageArgumentsMap);
			assert !messageArgumentsMap.isEmpty();
		}
		return messageArguments.prefixed(contextPrefix);
	}

	/**
	 * Populates the given {@link Map} with the possible message arguments for this shopkeeper.
	 * <p>
	 * In order to not calculate all message arguments in advance when they might not actually be required, the message
	 * arguments are meant to be {@link Supplier Suppliers} that lazily calculate the actual message arguments only when
	 * they are requested.
	 * <p>
	 * In order to be able to reuse the once populated Map, these {@link Supplier Suppliers} are also meant to be
	 * stateless: Any message arguments that depend on dynamic state of this shopkeeper are meant to dynamically
	 * retrieve the current values whenever their {@link Supplier Suppliers} are invoked.
	 * 
	 * @param messageArguments
	 *            the Map of lazily and dynamically evaluated message arguments
	 */
	protected void populateMessageArguments(Map<String, Supplier<Object>> messageArguments) {
		messageArguments.put("id", () -> String.valueOf(this.getId()));
		messageArguments.put("uuid", () -> this.getUniqueId().toString());
		messageArguments.put("name", () -> this.getName());
		messageArguments.put("world", () -> StringUtils.getOrEmpty(this.getWorldName()));
		messageArguments.put("x", () -> this.getX());
		messageArguments.put("y", () -> this.getY());
		messageArguments.put("z", () -> this.getZ());
		// TODO The decimal format is not localized. Move it into the language file?
		messageArguments.put("yaw", () -> TextUtils.DECIMAL_FORMAT.format(this.getYaw()));
		// TODO Rename to 'position'?
		messageArguments.put("location", () -> this.getPositionString());
		messageArguments.put("type", () -> this.getType().getIdentifier());
		messageArguments.put("object_type", () -> this.getShopObject().getType().getIdentifier());
	}

	// NAMING

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String newName) {
		// Prepare and apply new name:
		String preparedName = newName;
		if (preparedName == null) preparedName = "";
		preparedName = TextUtils.colorize(preparedName);
		preparedName = this.trimName(preparedName);
		this.name = preparedName;

		// Update shop object:
		shopObject.setName(preparedName);
		this.markDirty(); // Mark dirty
	}

	public boolean isValidName(String name) {
		return (name != null && name.length() <= MAX_NAME_LENGTH
				&& Settings.DerivedSettings.shopNamePattern.matcher(name).matches());
	}

	private String trimName(String name) {
		assert name != null;
		if (name.length() <= MAX_NAME_LENGTH) {
			return name;
		}
		String trimmedName = name.substring(0, MAX_NAME_LENGTH);
		Log.warning("Name of shopkeeper " + id + " is longer than " + MAX_NAME_LENGTH
				+ ". Trimming name '" + name + "' to '" + trimmedName + "'.");
		return trimmedName;
	}

	// SHOP OBJECT

	@Override
	public AbstractShopObject getShopObject() {
		return shopObject;
	}

	// TRADING

	@Override
	public abstract boolean hasTradingRecipes(Player player);

	@Override
	public abstract List<? extends TradingRecipe> getTradingRecipes(Player player);

	// USER INTERFACES

	@Override
	public Collection<? extends UISession> getUISessions() {
		return ShopkeepersPlugin.getInstance().getUIRegistry().getUISessions(this);
	}

	@Override
	public Collection<? extends UISession> getUISessions(UIType uiType) {
		return ShopkeepersPlugin.getInstance().getUIRegistry().getUISessions(this, uiType);
	}

	@Override
	public void abortUISessionsDelayed() {
		ShopkeepersPlugin.getInstance().getUIRegistry().abortUISessionsDelayed(this);
	}

	@Deprecated
	@Override
	public void closeAllOpenWindows() {
		this.abortUISessionsDelayed();
	}

	/**
	 * Registers an {@link UIHandler} which handles a specific type of user interface for this shopkeeper.
	 * <p>
	 * This replaces any {@link UIHandler} which has been previously registered for the same {@link UIType}.
	 * 
	 * @param uiHandler
	 *            the UI handler
	 */
	public void registerUIHandler(UIHandler uiHandler) {
		Validate.notNull(uiHandler, "UI handler is null!");
		uiHandlers.put(uiHandler.getUIType().getIdentifier(), uiHandler);
	}

	/**
	 * Gets the {@link UIHandler} this shopkeeper is using for the specified {@link UIType}.
	 * 
	 * @param uiType
	 *            the UI type
	 * @return the UI handler, or <code>null</code> if none is available
	 */
	public UIHandler getUIHandler(UIType uiType) {
		Validate.notNull(uiType, "UI type is null!");
		return uiHandlers.get(uiType.getIdentifier());
	}

	@Override
	public boolean openWindow(UIType uiType, Player player) {
		return SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(uiType, this, player);
	}

	// Shortcuts for the default UI types:

	@Override
	public boolean openEditorWindow(Player player) {
		return this.openWindow(DefaultUITypes.EDITOR(), player);
	}

	@Override
	public boolean openTradingWindow(Player player) {
		return this.openWindow(DefaultUITypes.TRADING(), player);
	}

	// INTERACTION HANDLING

	/**
	 * Called when a player interacts with this shopkeeper.
	 * 
	 * @param player
	 *            the interacting player
	 */
	public void onPlayerInteraction(Player player) {
		assert player != null;
		if (player.isSneaking()) {
			// Open editor window:
			this.openEditorWindow(player);
		} else {
			// Open trading window:
			this.openTradingWindow(player);
		}
	}

	// ACTIVATION AND TICKING

	public void onChunkActivation() {
	}

	public void onChunkDeactivation() {
	}

	// For internal purposes only.
	final int getTickingGroup() {
		return tickingGroup;
	}

	// TODO Maybe also tick shopkeepers if the container chunk is loaded? This might make sense once a shopkeeper can be
	// linked to multiple containers, and for virtual player shopkeepers.
	/**
	 * This is called periodically (roughly once per second) for shopkeepers in active chunks.
	 * <p>
	 * Consequently, this is not called for {@link Shopkeeper#isVirtual() virtual} shopkeepers.
	 * <p>
	 * This can for example be used for checks that need to happen periodically, such as checking if the container of a
	 * player shop still exists.
	 * <p>
	 * If the check to perform is potentially heavy or not required to happen every second, the shopkeeper may decide to
	 * only run it every X invocations.
	 * <p>
	 * The ticking of shopkeepers in active chunks may be spread across multiple ticks and may therefore not happen for
	 * all shopkeepers within the same tick.
	 * <p>
	 * If any of the ticked shopkeepers are marked as {@link AbstractShopkeeper#isDirty() dirty}, a
	 * {@link ShopkeeperStorage#saveDelayed() delayed save} will subsequently be triggered.
	 * <p>
	 * Any changes to the shopkeeper's activation state or {@link AbstractShopObject#getId() shop object id} may only be
	 * processed after the ticking of all currently ticked shopkeepers completes.
	 * <p>
	 * If you are overriding this method, consider calling the parent class version of this method.
	 */
	public void tick() {
		// Nothing to do by default.
	}

	/**
	 * Visualizes the shopkeeper's activity during the last tick.
	 */
	public void visualizeLastTick() {
		Location particleLocation = shopObject.getTickVisualizationParticleLocation();
		if (particleLocation == null) return;

		assert particleLocation.isWorldLoaded();
		World world = particleLocation.getWorld();
		assert world != null;
		world.spawnParticle(Particle.REDSTONE, particleLocation, 1, TICK_VISUALIZATION_DUSTS[tickingGroup]);
	}

	// TOSTRING

	@Override
	public String toString() {
		return "Shopkeeper " + this.getIdString();
	}

	// HASHCODE AND EQUALS

	@Override
	public final int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public final boolean equals(Object obj) {
		return (this == obj); // Identity based comparison
	}
}
