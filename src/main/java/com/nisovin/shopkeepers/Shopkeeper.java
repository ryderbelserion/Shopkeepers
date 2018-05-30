package com.nisovin.shopkeepers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.util.ChunkCoords;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public abstract class Shopkeeper {

	private int sessionId;
	private UUID uniqueId;
	protected ShopObject shopObject;
	protected String worldName;
	protected int x;
	protected int y;
	protected int z;
	protected ChunkCoords chunkCoords;
	protected String name = "";

	private boolean valid = false;

	protected final Map<String, UIHandler> uiHandlers = new HashMap<String, UIHandler>();
	private boolean uiActive = true; // can be used to deactivate UIs for this shopkeeper

	/**
	 * Creates a not fully initialized shopkeeper object. Do not attempt to use this object until initialization has
	 * been finished!
	 * Only use this from inside a constructor of an extending class.
	 * Depending on how the shopkeeper was created it is required to call either
	 * {@link #initOnLoad(ConfigurationSection)} or {@link #initOnCreation(ShopCreationData)}.
	 * Afterwards it is also required to call {@link #onInitDone()}.
	 */
	protected Shopkeeper() {
	}

	/**
	 * Call this at the beginning of the constructor of an extending class, if the shopkeeper was loaded from config.
	 * This will do the required initialization and then spawn the shopkeeper.
	 * 
	 * @param config
	 *            the config section containing the shopkeeper's data
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper cannot be properly initialized or loaded
	 */
	protected void initOnLoad(ConfigurationSection config) throws ShopkeeperCreateException {
		this.load(config);
	}

	/**
	 * Call this at the beginning of the constructor of an extending class, if the shopkeeper was freshly created by a
	 * player.
	 * This will do the required initialization and then spawn the shopkeeper.
	 * 
	 * @param creationData
	 *            the shop creation data
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper cannot be properly initialized
	 */
	protected void initOnCreation(ShopCreationData creationData) throws ShopkeeperCreateException {
		assert creationData != null;

		this.uniqueId = UUID.randomUUID();
		Location spawnLocation = creationData.getSpawnLocation();
		assert spawnLocation != null;
		this.worldName = spawnLocation.getWorld().getName();
		this.x = spawnLocation.getBlockX();
		this.y = spawnLocation.getBlockY();
		this.z = spawnLocation.getBlockZ();
		this.updateChunkCoords();

		ShopObjectType shopObjectType = creationData.getShopObjectType();
		assert shopObjectType != null;
		this.shopObject = shopObjectType.createObject(this, creationData);
	}

	/**
	 * Call this at the beginning of the constructor of an extending class, after either
	 * {@link #initOnLoad(ConfigurationSection)} or {@link #initOnCreation(ShopCreationData)} have been called.
	 */
	protected void onInitDone() {
		// nothing by default
	}

	/**
	 * Loads a shopkeeper's saved data from a config section of a config file.
	 * 
	 * @param config
	 *            the config section
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper cannot be properly loaded
	 */
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		String uniqueIdString = config.getString("uniqueId", "");
		try {
			this.uniqueId = UUID.fromString(uniqueIdString);
		} catch (IllegalArgumentException e) {
			if (!uniqueIdString.isEmpty()) {
				Log.warning("Invalid shop uuid '" + uniqueIdString + "'. Creating a new one.");
			}
			this.uniqueId = UUID.randomUUID();
		}

		this.name = Utils.colorize(config.getString("name", ""));
		this.worldName = config.getString("world");
		this.x = config.getInt("x");
		this.y = config.getInt("y");
		this.z = config.getInt("z");
		this.updateChunkCoords();

		ShopObjectType objectType = ShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().get(config.getString("object"));
		if (objectType == null) {
			// use default shop object type:
			objectType = ShopkeepersPlugin.getInstance().getDefaultShopObjectType();
			Log.warning("Invalid object type '" + config.getString("object") + "' for shopkeeper '" + uniqueId
					+ "'. Did you edit the save file? Switching to default type '" + objectType.getIdentifier() + "'.");
		}
		this.shopObject = objectType.createObject(this, null);
		this.shopObject.load(config);
	}

	/**
	 * Saves the shopkeeper's data to the specified configuration section.
	 * 
	 * @param config
	 *            the config section
	 */
	protected void save(ConfigurationSection config) {
		config.set("uniqueId", uniqueId.toString());
		config.set("name", Utils.decolorize(name));
		config.set("world", worldName);
		config.set("x", x);
		config.set("y", y);
		config.set("z", z);
		config.set("type", this.getType().getIdentifier());
		shopObject.save(config);
	}

	/**
	 * Gets the shop's unique id.
	 * 
	 * <p>
	 * This id is meant to be unique and never change.
	 * </p>
	 * 
	 * @return the shop's unique id
	 */
	public UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * Gets the shop's session id.
	 * 
	 * <p>
	 * This id is unique across all currently loaded shops, but may change across server restarts or when the shops are
	 * getting reloaded.<br>
	 * To reliable identify a shop use {@link #getUniqueId()} instead.
	 * </p>
	 * 
	 * @return the shop's session id
	 */
	public int getSessionId() {
		return sessionId;
	}

	/**
	 * Gets the type of this shopkeeper (ex: admin, normal player, book player, buying player, trading player, etc.).
	 * 
	 * @return the shopkeeper type
	 */
	public abstract ShopType<?> getType();

	public ShopObject getShopObject() {
		return shopObject;
	}

	protected void onChunkLoad() {
		shopObject.onChunkLoad();
	}

	protected void onChunkUnload() {
		shopObject.onChunkUnload();
	}

	/**
	 * Spawns the shopkeeper into the world at its spawn location and overwrites it's AI.
	 */
	public boolean spawn() {
		return shopObject.spawn();
	}

	/**
	 * Whether or not this shopkeeper needs to be spawned and despawned with chunk load and unloads.
	 * 
	 * @return
	 */
	public boolean needsSpawning() {
		return shopObject.getObjectType().needsSpawning();
	}

	/**
	 * Checks if the shopkeeper is active (is present in the world).
	 * 
	 * @return whether the shopkeeper is active
	 */
	public boolean isActive() {
		return shopObject.isActive();
	}

	/**
	 * See {@link ShopObject#check()}.
	 * 
	 * @return whether to update this shopkeeper in the activeShopkeepers collection
	 */
	public boolean check() {
		return shopObject.check();
	}

	/**
	 * Removes this shopkeeper from the world.
	 */
	public void despawn() {
		shopObject.despawn();
	}

	/**
	 * Persistently removes this shopkeeper.
	 */
	public void delete() {
		ShopkeepersPlugin.getInstance().deleteShopkeeper(this);
	}

	protected void onDeletion() {
		shopObject.delete();
		valid = false;
	}

	/**
	 * The shopkeepers gets invalid, when he was deleted.
	 * 
	 * @return <code>true</code> if not deleted
	 */
	public boolean isValid() {
		return valid;
	}

	protected void onRegistration(int sessionId) {
		assert !valid;
		this.sessionId = sessionId;
		valid = true;
	}

	/**
	 * Gets the ChunkCoords identifying the chunk this shopkeeper spawns in.
	 * 
	 * @return the chunk coordinates
	 */
	public ChunkCoords getChunkCoords() {
		return chunkCoords;
	}

	public String getPositionString() {
		return Utils.getLocationString(worldName, x, y, z);
	}

	public Location getActualLocation() {
		return shopObject.getActualLocation();
	}

	/**
	 * Gets the name of the world this shopkeeper lives in.
	 * 
	 * @return the world name
	 */
	public String getWorldName() {
		return worldName;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	/**
	 * This only works if the world is loaded.
	 * 
	 * @return null, if the world this shopkeeper is in isn't loaded
	 */
	public Location getLocation() {
		World world = Bukkit.getWorld(worldName);
		if (world == null) return null;
		return new Location(world, x, y, z);
	}

	/**
	 * Sets the stored location of this Shopkeeper.
	 * This will not actually move the shopkeeper entity until the next time teleport() is called.
	 * 
	 * @param location
	 *            The new stored location of this shopkeeper.
	 */
	public void setLocation(Location location) {
		ChunkCoords oldChunk = this.getChunkCoords();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
		worldName = location.getWorld().getName();
		this.updateChunkCoords();

		// update shopkeeper in chunk map:
		ShopkeepersPlugin.getInstance().onShopkeeperMove(this, oldChunk);
	}

	private void updateChunkCoords() {
		this.chunkCoords = ChunkCoords.fromBlockPos(worldName, x, z);
	}

	/**
	 * Gets the shopkeeper's object ID. This is can change when the shopkeeper object (ex. shopkeeper entity) respawns.
	 * 
	 * @return the object id, or null if the shopkeeper object is currently not active
	 */
	public String getObjectId() {
		return shopObject.getId();
	}

	/**
	 * Gets the shopkeeper's currently available trading recipes for the given player.
	 * <p>
	 * Depending on the type of shopkeeper this might access the world data to determine available stock (chest
	 * contents).<br>
	 * Managing (adding, removing, editing, validating) the overall available trading recipes of this shopkeeper might
	 * differ between different shopkeeper types and is therefore in their responsibility.
	 * <p>
	 * The <code>player</code> parameter can be used to request player-specific trading recipes, if the shopkeeper types
	 * supports that.
	 * 
	 * @param player
	 *            the player (can be <code>null</code>), allows for returning player-specific trading recipes if the
	 *            shopkeeper supports that
	 * @return an unmodifiable view on the currently available trading recipes of this shopkeeper for the given player
	 */
	public abstract List<TradingRecipe> getTradingRecipes(Player player);

	// SHOPKEEPER UIs:

	public boolean isUIActive() {
		return uiActive;
	}

	public void deactivateUI() {
		uiActive = false;
	}

	public void activateUI() {
		uiActive = true;
	}

	/**
	 * Deactivates all currently open UIs (purchasing, editing, hiring, etc.) and closes them 1 tick later.
	 */
	public void closeAllOpenWindows() {
		ShopkeepersPlugin.getInstance().getUIManager().closeAllDelayed(this);
	}

	/**
	 * Registers an {@link UIHandler} which handles a specific type of user interface for this shopkeeper.
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

	/**
	 * Attempts to open the interface for the given {@link UIType} for the specified player.
	 * <p>
	 * This fails if this shopkeeper doesn't support the specified interface type, if the player cannot open this
	 * interface type for this shopkeeper (for example because of missing permissions), or if something else goes wrong.
	 * 
	 * @param uiType
	 *            the requested ui type
	 * @param player
	 *            the player requesting the interface
	 * @return <code>true</code> the player's request was successful and the interface was opened, false otherwise
	 */
	public boolean openWindow(UIType uiType, Player player) {
		return ShopkeepersPlugin.getInstance().getUIManager().requestUI(uiType, this, player);
	}

	// shortcuts for the default window types:

	/**
	 * Attempts to open the editor interface of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player requesting the editor interface
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openEditorWindow(Player player) {
		return this.openWindow(DefaultUIs.EDITOR_WINDOW, player);
	}

	/**
	 * Attempts to open the trading interface of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player requesting the trading interface
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openTradingWindow(Player player) {
		return this.openWindow(DefaultUIs.TRADING_WINDOW, player);
	}

	// TODO move these into PlayerShopkeeper
	/**
	 * Attempts to open the hiring interface of this shopkeeper for the specified player.
	 * <p>
	 * Fails if this shopkeeper type doesn't support hiring (ex. admin shops).
	 * 
	 * @param player
	 *            the player requesting the hiring interface
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openHireWindow(Player player) {
		return this.openWindow(DefaultUIs.HIRING_WINDOW, player);
	}

	/**
	 * Attempts to open the chest inventory of this shopkeeper for the specified player.
	 * <p>
	 * Fails if this shopkeeper type doesn't have a chest (ex. admin shops).
	 * 
	 * @param player
	 *            the player requesting the chest inventory window
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openChestWindow(Player player) {
		return false;
	}

	// NAMING:

	public String getName() {
		return name;
	}

	public void setName(String name) {
		if (name == null) name = "";
		name = Utils.colorize(name);
		this.name = shopObject.trimToNameLength(name);
		shopObject.setName(this.name);
	}

	public void startNaming(Player player) {
		ShopkeepersPlugin.getInstance().onNaming(player, this);
	}

	public boolean isValidName(String name) {
		return name != null && name.matches("^" + Settings.nameRegex + "$");
	}

	public boolean requestNameChange(Player player, String newName) {
		if (player == null) return false;
		if (!this.isValid()) return false;

		// update name:
		if (newName.isEmpty() || newName.equals("-")) {
			// remove name:
			newName = "";
		} else {
			// validate name:
			if (!this.isValidName(newName)) {
				Utils.sendMessage(player, Settings.msgNameInvalid);
				return false;
			}
		}

		// apply new name:
		this.setName(newName);

		// inform player:
		Utils.sendMessage(player, Settings.msgNameSet);

		// close all open windows:
		this.closeAllOpenWindows(); // TODO really needed?

		// run shopkeeper-edited event:
		Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, this));

		// save:
		ShopkeepersPlugin.getInstance().save();

		return true;
	}

	// HANDLE INTERACTION:

	/**
	 * Called when a player interacts with this shopkeeper.
	 * 
	 * @param player
	 *            the interacting player
	 */
	protected void onPlayerInteraction(Player player) {
		assert player != null;
		if (player.isSneaking()) {
			// open editor window:
			this.openEditorWindow(player);
		} else {
			// open trading window:
			this.openTradingWindow(player);
		}
	}
}
