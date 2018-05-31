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

import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.api.util.TradingRecipe;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObject;
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.shoptypes.AbstractShopType;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractShopkeeper implements Shopkeeper {

	private int sessionId;
	private UUID uniqueId;
	private AbstractShopObject shopObject;
	private String worldName;
	private int x;
	private int y;
	private int z;
	private ChunkCoords chunkCoords;
	private String name = "";

	private boolean valid = false;

	// ui type identifier -> ui handler
	private final Map<String, UIHandler> uiHandlers = new HashMap<>();
	private boolean uiActive = true; // can be used to deactivate UIs for this shopkeeper

	/**
	 * Creates a not fully initialized shopkeeper object. Do not attempt to use this object until initialization has
	 * been finished!
	 * Only use this from inside a constructor of an extending class.
	 * Depending on how the shopkeeper was created it is required to call either
	 * {@link #initOnLoad(ConfigurationSection)} or {@link #initOnCreation(ShopCreationData)}.
	 * Afterwards it is also required to call {@link #onInitDone()}.
	 */
	public AbstractShopkeeper() {
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

		ShopObjectType<?> shopObjectType = creationData.getShopObjectType();
		Validate.isTrue(shopObjectType instanceof AbstractShopObjectType,
				"Expecting an AbstractShopObjectType, got " + shopObjectType.getClass().getName());
		this.shopObject = ((AbstractShopObjectType<?>) shopObjectType).createObject(this, creationData);
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

		AbstractShopObjectType<?> objectType = SKShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().get(config.getString("object"));
		if (objectType == null) {
			// use default shop object type:
			objectType = SKShopkeepersPlugin.getInstance().getDefaultShopObjectType();
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
	public void save(ConfigurationSection config) {
		config.set("uniqueId", uniqueId.toString());
		config.set("name", Utils.decolorize(name));
		config.set("world", worldName);
		config.set("x", x);
		config.set("y", y);
		config.set("z", z);
		config.set("type", this.getType().getIdentifier());
		shopObject.save(config);
	}

	@Override
	public UUID getUniqueId() {
		return uniqueId;
	}

	@Override
	public int getSessionId() {
		return sessionId;
	}

	@Override
	public abstract AbstractShopType<?> getType();

	@Override
	public AbstractShopObject getShopObject() {
		return shopObject;
	}

	protected void onChunkLoad() {
		shopObject.onChunkLoad();
	}

	protected void onChunkUnload() {
		shopObject.onChunkUnload();
	}

	@Override
	public boolean spawn() {
		return shopObject.spawn();
	}

	@Override
	public boolean needsSpawning() {
		return shopObject.getObjectType().needsSpawning();
	}

	@Override
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

	@Override
	public void despawn() {
		shopObject.despawn();
	}

	@Override
	public void delete() {
		SKShopkeepersPlugin.getInstance().deleteShopkeeper(this);
	}

	protected void onDeletion() {
		// TODO actually: do this for every unregistration, including on reloads..
		shopObject.delete();
		valid = false;
	}

	@Override
	public boolean isValid() {
		return valid;
	}

	protected void onRegistration(int sessionId) {
		assert !valid;
		this.sessionId = sessionId;
		valid = true;
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
	public Location getLocation() {
		World world = Bukkit.getWorld(worldName);
		if (world == null) return null;
		return new Location(world, x, y, z);
	}

	@Override
	public void setLocation(Location location) {
		ChunkCoords oldChunk = this.getChunkCoords();
		x = location.getBlockX();
		y = location.getBlockY();
		z = location.getBlockZ();
		worldName = location.getWorld().getName();
		this.updateChunkCoords();

		// update shopkeeper in chunk map:
		SKShopkeepersPlugin.getInstance().onShopkeeperMove(this, oldChunk);
	}

	@Override
	public String getPositionString() {
		return Utils.getLocationString(worldName, x, y, z);
	}

	@Override
	public ChunkCoords getChunkCoords() {
		return chunkCoords;
	}

	private void updateChunkCoords() {
		this.chunkCoords = ChunkCoords.fromBlockPos(worldName, x, z);
	}

	@Override
	public Location getActualLocation() {
		return shopObject.getActualLocation();
	}

	@Override
	public String getObjectId() {
		return shopObject.getId();
	}

	@Override
	public abstract List<TradingRecipe> getTradingRecipes(Player player);

	// SHOPKEEPER UIs:

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

	// shortcuts for the default window types:

	@Override
	public boolean openEditorWindow(Player player) {
		return this.openWindow(DefaultUITypes.EDITOR(), player);
	}

	@Override
	public boolean openTradingWindow(Player player) {
		return this.openWindow(DefaultUITypes.TRADING(), player);
	}

	// TODO move these into PlayerShopkeeper
	@Override
	public boolean openHireWindow(Player player) {
		return this.openWindow(DefaultUITypes.HIRING(), player);
	}

	@Override
	public boolean openChestWindow(Player player) {
		return false;
	}

	// NAMING:

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		if (name == null) name = "";
		name = Utils.colorize(name);
		this.name = shopObject.trimToNameLength(name);
		shopObject.setName(this.name);
	}

	// TODO move these somewhere else

	public void startNaming(Player player) {
		SKShopkeepersPlugin.getInstance().onNaming(player, this);
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
		ShopkeepersPlugin.getInstance().getShopkeeperStorage().save();

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
