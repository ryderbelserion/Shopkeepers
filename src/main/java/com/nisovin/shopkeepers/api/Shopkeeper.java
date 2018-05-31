package com.nisovin.shopkeepers.api;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.api.util.TradingRecipe;

public interface Shopkeeper {

	/**
	 * Gets the shop's unique id.
	 * 
	 * <p>
	 * This id is meant to be unique and never change.
	 * 
	 * @return the shop's unique id
	 */
	public UUID getUniqueId();

	/**
	 * Gets the shop's session id.
	 * 
	 * <p>
	 * This id is unique across all currently loaded shops, but may change across server restarts or when the shops are
	 * getting reloaded.<br>
	 * To reliable identify a shop use {@link #getUniqueId()} instead.
	 * 
	 * @return the shop's session id
	 */
	public int getSessionId();

	/**
	 * Gets the type of this shopkeeper (ex: admin, normal player, book player, buying player, trading player, etc.).
	 * 
	 * @return the shopkeeper type
	 */
	public ShopType<?> getType();

	/**
	 * Gets the object representing this shopkeeper in the world.
	 * 
	 * @return the shop object
	 */
	public ShopObject getShopObject();

	/**
	 * Spawns the shopkeeper into the world at its spawn location and overwrites it's AI.
	 * 
	 * @return <code>true</code> on success
	 */
	public boolean spawn();

	/**
	 * Whether or not this shopkeeper needs to be spawned and despawned with chunk load and unloads.
	 * 
	 * @return <code>true</code> if spawning is required
	 */
	public boolean needsSpawning();

	/**
	 * Checks if the shopkeeper is active (is present in the world).
	 * 
	 * @return <code>true</code> if the shopkeeper is active
	 */
	public boolean isActive();

	/**
	 * Removes this shopkeeper from the world.
	 */
	public void despawn();

	/**
	 * Persistently removes this shopkeeper.
	 */
	public void delete();

	/**
	 * The shopkeeper gets marked as 'invalid' when being unregistered (ex. on deletion or if being replaced with a
	 * freshly loaded shopkeeper instance).
	 * 
	 * @return <code>true</code> if still valid
	 */
	public boolean isValid();

	/**
	 * Gets the ChunkCoords identifying the chunk this shopkeeper spawns in.
	 * 
	 * @return the chunk coordinates
	 */
	public ChunkCoords getChunkCoords();

	public String getPositionString();

	public Location getActualLocation();

	/**
	 * Gets the name of the world this shopkeeper lives in.
	 * 
	 * @return the world name
	 */
	public String getWorldName();

	public int getX();

	public int getY();

	public int getZ();

	/**
	 * This only works if the world is loaded.
	 * 
	 * @return <code>null</code> if the world this shopkeeper is in isn't loaded
	 */
	public Location getLocation();

	/**
	 * Sets the stored location of this Shopkeeper.
	 * <p>
	 * This will not actually move the shopkeeper entity until the next time teleport() is called.
	 * 
	 * @param location
	 *            the new stored location of this shopkeeper
	 */
	public void setLocation(Location location);

	/**
	 * Gets the shopkeeper's object ID. This is can change when the shopkeeper object (ex. shopkeeper entity) respawns.
	 * 
	 * @return the object id, or <code>null</code> if the shopkeeper object is currently not active
	 */
	public String getObjectId();

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
	public List<TradingRecipe> getTradingRecipes(Player player);

	// SHOPKEEPER UIs:

	public boolean isUIActive();

	public void deactivateUI();

	public void activateUI();

	/**
	 * Deactivates all currently open UIs (purchasing, editing, hiring, etc.) and closes them 1 tick later.
	 */
	public void closeAllOpenWindows();

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
	public boolean openWindow(UIType uiType, Player player);

	// shortcuts for the default window types:

	/**
	 * Attempts to open the editor interface of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player requesting the editor interface
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openEditorWindow(Player player);

	/**
	 * Attempts to open the trading interface of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player requesting the trading interface
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openTradingWindow(Player player);

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
	public boolean openHireWindow(Player player);

	/**
	 * Attempts to open the chest inventory of this shopkeeper for the specified player.
	 * <p>
	 * Fails if this shopkeeper type doesn't have a chest (ex. admin shops).
	 * 
	 * @param player
	 *            the player requesting the chest inventory window
	 * @return <code>true</code> if the interface was successfully opened for the player
	 */
	public boolean openChestWindow(Player player);

	// NAMING:

	public String getName();

	public void setName(String name);
}
