package com.nisovin.shopkeepers.shopkeeper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
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
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.compat.MC_1_16_Utils;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shopobjects.living.types.CatShop;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

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

	// the maximum supported name length:
	// the actual maximum name length that can be used might be lower depending on config settings
	// and on shop object specific limits
	public static final int MAX_NAME_LENGTH = 128;

	private final int id;
	private UUID uniqueId; // not null after initialization
	private AbstractShopObject shopObject; // not null after initialization
	// TODO move location information into ShopObject?
	// TODO store yaw?
	private String worldName; // not empty, null for virtual shops
	private int x;
	private int y;
	private int z;
	private ChunkCoords chunkCoords; // null for virtual shops
	private String name = ""; // not null, can be empty

	// has unsaved data changes:
	private boolean dirty = false;
	// is currently registered:
	private boolean valid = false;

	// ui type identifier -> ui handler
	private final Map<String, UIHandler> uiHandlers = new HashMap<>();
	private boolean uiActive = true; // can be used to deactivate UIs for this shopkeeper

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
			// virtual shops ignore any potentially available spawn location:
			this.worldName = null;
			this.x = 0;
			this.y = 0;
			this.z = 0;
		} else {
			Location spawnLocation = shopCreationData.getSpawnLocation();
			assert spawnLocation != null && spawnLocation.getWorld() != null;
			this.worldName = spawnLocation.getWorld().getName();
			this.x = spawnLocation.getBlockX();
			this.y = spawnLocation.getBlockY();
			this.z = spawnLocation.getBlockZ();
		}
		this.updateChunkCoords();

		this.shopObject = this.createShopObject((AbstractShopObjectType<?>) shopObjectType, shopCreationData);

		// automatically mark new shopkeepers as dirty:
		this.markDirty();
	}

	/**
	 * This gets called at the end of construction, after the shopkeeper data has been loaded.
	 * <p>
	 * This can be used to perform any remaining setup.
	 * <p>
	 * This might setup defaults for some things, if not yet specified by the sub-classes. So if you are overriding this
	 * method, consider doing your own setup before calling the overridden method. And also take into account that
	 * further sub-classes might perform their setup prior to calling your setup method as well. So don't replace any
	 * components that have already been setup by further sub-classes.
	 */
	protected void setup() {
		// add a default trading handler, if none is provided:
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new TradingHandler(SKDefaultUITypes.TRADING(), this));
		}
	}

	/**
	 * This gets called after {@link #setup()} and might be used to perform any setup that is intended to definitely
	 * happen last.
	 */
	protected void postSetup() {
		// inform shop object:
		this.getShopObject().setup();
	}

	// STORAGE

	/**
	 * Loads the shopkeeper's saved data from the given config section.
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

		// shop object:
		String objectTypeId;
		ConfigurationSection objectSection = configSection.getConfigurationSection("object");
		if (objectSection == null) {
			// load from legacy data:
			// TODO remove again at some point
			objectTypeId = configSection.getString("object");
			objectSection = configSection;
			this.markDirty();
		} else {
			objectTypeId = objectSection.getString("type");
		}

		// convert legacy object identifiers:
		if (objectTypeId != null) {
			// 'block' -> 'sign'
			if (objectTypeId.equalsIgnoreCase("block")) {
				objectTypeId = "sign";
				this.markDirty();
			}

			// normalize:
			String normalizedOjectTypeId = StringUtils.normalize(objectTypeId);
			if (!normalizedOjectTypeId.equals(objectTypeId)) {
				objectTypeId = normalizedOjectTypeId;
				this.markDirty();
			}

			// MC 1.14:
			// convert ocelots to cats:
			if (objectTypeId.equals("ocelot")) {
				String ocelotType = objectSection.getString("catType");
				if (ocelotType != null) {
					if (ocelotType.equals("WILD_OCELOT")) {
						// stays an ocelot, but remove cat type data:
						objectSection.set("catType", null);
						this.markDirty();
					} else {
						// convert to cat:
						objectTypeId = "cat";
						String catType = CatShop.fromOcelotType(ocelotType).name();
						objectSection.set("catType", catType);
						this.markDirty();
						Log.warning("Migrated ocelot shopkeeper '" + id + "' of type '" + ocelotType
								+ "' to cat shopkeeper of type '" + catType + "'.");
					}
				} // else: stays ocelot
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
			// couldn't find object type by id, try to find object type via matching:
			objectType = SKShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().match(objectTypeId);
			if (objectType != null) {
				// mark dirty, so the correct id gets saved:
				this.markDirty();
			} else {
				throw new ShopkeeperCreateException("Invalid object type for shopkeeper '" + id + "': " + objectTypeId);
			}
		}
		assert objectType != null;

		// normalize empty world name to null:
		String storedWorldName = StringUtils.getNotEmpty(configSection.getString("world"));
		int storedX = configSection.getInt("x");
		int storedY = configSection.getInt("y");
		int storedZ = configSection.getInt("z");

		if (objectType instanceof VirtualShopObjectType) {
			if (storedWorldName != null || storedX != 0 || storedY != 0 || storedZ != 0) {
				Log.warning("Ignoring stored world and coordinates ("
						+ TextUtils.getLocationString(StringUtils.getNotNull(storedWorldName), storedX, storedY, storedZ)
						+ ") for virtual shopkeeper '" + id + "'!");
				this.markDirty();
			}
			this.worldName = null;
			this.x = 0;
			this.y = 0;
			this.z = 0;
		} else {
			if (storedWorldName == null) {
				throw new ShopkeeperCreateException("Missing world name for shopkeeper '" + id + "'!");
			}
			this.worldName = storedWorldName;
			this.x = storedX;
			this.y = storedY;
			this.z = storedZ;
		}
		this.updateChunkCoords();

		this.shopObject = this.createShopObject(objectType, null);
		this.shopObject.load(objectSection);
	}

	// shopCreationData can be null if the shopkeeper is getting loaded
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
		// null world name gets stored as empty string:
		configSection.set("world", StringUtils.getNotNull(worldName));
		configSection.set("x", x);
		configSection.set("y", y);
		configSection.set("z", z);
		configSection.set("type", this.getType().getIdentifier());

		// shop object:
		ConfigurationSection objectSection = configSection.createSection("object");
		shopObject.save(objectSection);
	}

	@Override
	public void save() {
		this.markDirty();
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}

	@Override
	public void saveDelayed() {
		this.markDirty();
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().saveDelayed();
	}

	/**
	 * Marks this shopkeeper as 'dirty'. Its data gets saved with the next save of the {@link ShopkeeperStorage}.
	 * <p>
	 * The shopkeeper and the shop object implementations are responsible for marking the shopkeeper as dirty on every
	 * change affecting data that needs to be persisted.
	 */
	public void markDirty() {
		dirty = true;
		// inform the storage that there are dirty shopkeepers:
		if (this.isValid()) {
			// if the shopkeeper gets marked dirty during creation or loading (while it is not yet valid),
			// the storage gets marked dirty by the shopkeeper registry after the creation/loading was successful
			SKShopkeepersPlugin.getInstance().getShopkeeperStorage().markDirty();
		}
	}

	@Override
	public boolean isDirty() {
		return dirty;
	}

	// called by shopkeeper storage once the shopkeeper data gets saved
	public void onSave() {
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
		this.onAdded(cause);
	}

	/**
	 * This gets called once the shopkeeper has been added to the {@link ShopkeeperRegistry}.
	 * <p>
	 * The shopkeeper has not yet been activated at this point.
	 * 
	 * @param cause
	 *            the cause for the addition
	 */
	protected void onAdded(ShopkeeperAddedEvent.Cause cause) {
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
	 * This gets called once the shopkeeper is about to be removed from the {@link ShopkeeperRegistry}.
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

	@Override
	public void delete(Player player) {
		this.markDirty();
		SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().deleteShopkeeper(this);
	}

	/**
	 * This gets called if the shopkeeper is about to be removed due to permanent deletion.
	 * <p>
	 * This gets called after {@link #onRemoval(com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent.Cause)}.
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
	public String getPositionString() {
		if (worldName == null) return "[virtual]";
		return TextUtils.getLocationString(worldName, x, y, z);
	}

	@Override
	public Location getLocation() {
		if (worldName == null) return null;
		World world = Bukkit.getWorld(worldName);
		if (world == null) return null;
		return new Location(world, x, y, z);
	}

	/**
	 * Sets the stored location of this shopkeeper.
	 * <p>
	 * This will not actually move the shop object on its own, until the next time it gets spawned at or teleported to
	 * its intended location.
	 * 
	 * @param location
	 *            the new stored location of this shopkeeper
	 */
	public void setLocation(Location location) {
		Validate.isTrue(!this.isVirtual(), "Cannot set location of virtual shopkeeper!");
		Validate.notNull(location, "Location is null!");
		World world = location.getWorld();
		Validate.notNull(world, "Location's world is null!");

		ChunkCoords oldChunk = this.getChunkCoords();
		// TODO changing the world is not safe (at least not for all types of shops)! consider for example player shops
		// which currently use the worldname to locate their chest
		worldName = world.getName();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
		this.updateChunkCoords();
		this.markDirty();

		// update shopkeeper in chunk map:
		SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().onShopkeeperMove(this, oldChunk);
	}

	@Override
	public ChunkCoords getChunkCoords() {
		return chunkCoords;
	}

	private void updateChunkCoords() {
		this.chunkCoords = this.isVirtual() ? null : ChunkCoords.fromBlockPos(worldName, x, z);
	}

	// TODO this has to be aware of sub types in order to replace arguments with empty strings
	// -> move into Shop type and abstract shop type and let all registered types provide arguments
	// TODO not yet used anywhere
	/*public final Map<String, Object> getShopkeeperMsgArgs(Shopkeeper shopkeeper) {
		Map<String, Object> msgArgs = new HashMap<>();
		msgArgs.put("uuid", shopkeeper.getUniqueId().toString());
		msgArgs.put("id", String.valueOf(shopkeeper.getId()));
		msgArgs.put("name", shopkeeper.getName());
		msgArgs.put("location", shopkeeper.getPositionString());
		msgArgs.put("shopType", shopkeeper.getType().getIdentifier());
		msgArgs.put("objectType", shopkeeper.getShopObject().getType().getIdentifier());
		PlayerShopkeeper playerShop = (shopkeeper instanceof PlayerShopkeeper) ? (PlayerShopkeeper) shopkeeper : null;
		msgArgs.put("ownerName", (playerShop == null) ? "" : playerShop.getOwnerName());
		msgArgs.put("ownerUUID", (playerShop == null) ? "" : playerShop.getOwnerUUID().toString());
		return msgArgs;
	}*/

	// NAMING

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String newName) {
		// prepare and apply new name:
		String preparedName = newName;
		if (preparedName == null) preparedName = "";
		preparedName = TextUtils.colorize(preparedName);
		preparedName = this.trimName(preparedName);
		this.name = preparedName;

		// update shop object:
		shopObject.setName(preparedName);
		this.markDirty(); // mark dirty
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
	public abstract List<TradingRecipe> getTradingRecipes(Player player);

	// USER INTERFACES

	@Override
	public boolean isUIActive() {
		return uiActive;
	}

	@Override
	public void deactivateUI() {
		uiActive = false;
	}

	@Override
	public void activateUI() {
		uiActive = true;
	}

	@Override
	public void closeAllOpenWindows() {
		ShopkeepersPlugin.getInstance().getUIRegistry().closeAllDelayed(this);
	}

	/**
	 * Registers an {@link UIHandler} which handles a specific type of user interface for this shopkeeper.
	 * <p>
	 * This replaces any {@link UIHandler} which has been previously registered for the same {@link UIType}.
	 * 
	 * @param uiHandler
	 *            the ui handler
	 */
	public void registerUIHandler(UIHandler uiHandler) {
		Validate.notNull(uiHandler, "UI handler is null!");
		uiHandlers.put(uiHandler.getUIType().getIdentifier(), uiHandler);
	}

	/**
	 * Gets the {@link UIHandler} this shopkeeper is using for the specified {@link UIType}.
	 * 
	 * @param uiType
	 *            the ui type
	 * @return the ui handler, or <code>null</code> if none is available
	 */
	public UIHandler getUIHandler(UIType uiType) {
		Validate.notNull(uiType, "UI type is null!");
		return uiHandlers.get(uiType.getIdentifier());
	}

	@Override
	public boolean openWindow(UIType uiType, Player player) {
		return SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(uiType, this, player);
	}

	// shortcuts for the default UI types:

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
			// open editor window:
			this.openEditorWindow(player);
		} else {
			// open trading window:
			this.openTradingWindow(player);
		}
	}

	// TICKING

	/**
	 * This is called periodically (roughly once per second) for shopkeepers in active chunks.
	 * <p>
	 * This can for example be used for checks that need to happen periodically, such as checking if the chest for a
	 * player shop still exists.
	 * <p>
	 * If the check to perform is potentially heavy or not required to happen every second, the shopkeeper may decide to
	 * only run it every X invocations.
	 * <p>
	 * If any of the ticked shopkeepers are marked as {@link Shopkeeper#isDirty() dirty}, a
	 * {@link ShopkeeperStorage#save() save} will be triggered after all shopkeepers in active chunks have been ticked.
	 */
	public void tick() {
		// nothing to do by default
	}

	// HASHCODE AND EQUALS

	@Override
	public final int hashCode() {
		return System.identityHashCode(this);
	}

	@Override
	public final boolean equals(Object obj) {
		return (this == obj); // identity based comparison
	}
}
