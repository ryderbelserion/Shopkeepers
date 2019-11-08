package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;

public interface Shopkeeper {

	// STORAGE

	/**
	 * Requests a {@link ShopkeeperStorage#save() save} of all shopkeepers data.
	 * 
	 * @see ShopkeeperStorage#save()
	 */
	public void save();

	/**
	 * Requests a {@link ShopkeeperStorage#saveDelayed() delayed save} of all shopkeepers data.
	 * 
	 * @see ShopkeeperStorage#saveDelayed()
	 */
	public void saveDelayed();

	/**
	 * Checks whether this shopkeeper has unsaved changes to its data.
	 * 
	 * @return <code>true</code> if marked as dirty
	 */
	public boolean isDirty();

	// LIFE CYCLE

	/**
	 * Checks whether this shopkeeper object is currently valid.
	 * <p>
	 * The shopkeeper gets marked as 'invalid' once it gets removed form the {@link ShopkeeperRegistry}.
	 * 
	 * @return <code>true</code> if valid
	 */
	public boolean isValid();

	/**
	 * Persistently removes this shopkeeper.
	 */
	public void delete();

	// ATTRIBUTES

	/**
	 * Gets the shop's id.
	 * 
	 * <p>
	 * This id is unique across all currently loaded shops, but there is no guarantee for it to be globally unique
	 * across server sessions.
	 * 
	 * @return the shop's id
	 */
	public int getId();

	/**
	 * Gets the shop's unique id.
	 * 
	 * <p>
	 * This id is globally unique across all shopkeepers.
	 * 
	 * @return the shop's unique id
	 */
	public UUID getUniqueId();

	/**
	 * Gets the type of this shopkeeper (ex: admin, selling player, buying player, trading player, etc.).
	 * 
	 * @return the shopkeeper type
	 */
	public ShopType<?> getType();

	/**
	 * Gets the name of the world this shopkeeper lives in.
	 * 
	 * @return the world name
	 */
	public String getWorldName();

	public int getX();

	public int getY();

	public int getZ();

	public String getPositionString();

	/**
	 * Gets the shopkeeper's location.
	 * <p>
	 * This only works if the world is loaded.
	 * 
	 * @return the location of the shopkeeper, or <code>null</code> if the world isn't loaded
	 */
	public Location getLocation();

	/**
	 * Gets the {@link ChunkCoords} identifying the chunk this shopkeeper spawns in.
	 * 
	 * @return the chunk coordinates
	 */
	public ChunkCoords getChunkCoords();

	// NAMING

	/**
	 * Gets the shopkeeper's name.
	 * <p>
	 * The name may include color codes.
	 * 
	 * @return the shopkeeper's name, not <code>null</code> but may be empty if not set
	 */
	public String getName();

	public void setName(String name);

	// SHOP OBJECT

	/**
	 * Gets the object representing this shopkeeper in the world.
	 * 
	 * @return the shop object, not <code>null</code>
	 */
	public ShopObject getShopObject();

	// TRADING

	/**
	 * Gets the shopkeeper's currently available trading recipes for the given player.
	 * <p>
	 * Depending on the type of shopkeeper this might access the world data to determine available stock (chest
	 * contents).<br>
	 * Managing (adding, removing, editing, validating) the overall available trading recipes of this shopkeeper might
	 * differ between different shopkeeper types and is therefore in their responsibility.
	 * <p>
	 * The <code>player</code> parameter can be used to request player-specific trading recipes, if the shopkeeper type
	 * supports that.
	 * 
	 * @param player
	 *            the player (can be <code>null</code>), allows for returning player-specific trading recipes if the
	 *            shopkeeper supports that
	 * @return an unmodifiable view on the currently available trading recipes of this shopkeeper for the given player
	 */
	public List<TradingRecipe> getTradingRecipes(Player player);

	// SHOPKEEPER UIs

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

	// shortcuts for the default UI types:

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
}
