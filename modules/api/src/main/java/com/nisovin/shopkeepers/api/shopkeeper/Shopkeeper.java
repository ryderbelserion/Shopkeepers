package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.api.ui.UIType;
import com.nisovin.shopkeepers.api.util.ChunkCoords;

/**
 * A shopkeeper.
 */
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

	// LIFE CYCLE

	/**
	 * Checks whether this shopkeeper instance is currently valid.
	 * <p>
	 * The shopkeeper is marked as valid after it has been freshly created or loaded from the storage and then added to
	 * the {@link ShopkeeperRegistry}. It is marked as 'invalid' once it is removed from the {@link ShopkeeperRegistry}
	 * again, for example because it is being deleted or unloaded.
	 * 
	 * @return <code>true</code> if the shopkeeper is currently valid
	 */
	public boolean isValid();

	/**
	 * Persistently removes this shopkeeper.
	 */
	public void delete();

	/**
	 * Persistently removes this shopkeeper.
	 * 
	 * @param player
	 *            the player responsible for the deletion, can be <code>null</code>
	 */
	public void delete(Player player);

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
	 * Gets a String containing both the shopkeeper's id and unique id.
	 * 
	 * @return a string representation of this shopkeeper's id and unique id
	 */
	public String getIdString();

	/**
	 * Gets the type of this shopkeeper (ex: admin, selling player, buying player, trading player, etc.).
	 * 
	 * @return the shopkeeper type
	 */
	public ShopType<?> getType();

	/**
	 * Checks whether this shopkeeper is virtual.
	 * <p>
	 * Virtual shopkeepers are not located in any world.
	 * 
	 * @return <code>true</code> if the shopkeeper is virtual
	 */
	public boolean isVirtual();

	/**
	 * Gets the name of the world this shopkeeper is located in.
	 * 
	 * @return the world name, not empty, but <code>null</code> if the shopkeeper is {@link #isVirtual() virtual}
	 */
	public String getWorldName();

	/**
	 * Gets the x coordinate of the shopkeeper.
	 * 
	 * @return the x coordinate
	 */
	public int getX();

	/**
	 * Gets the y coordinate of the shopkeeper.
	 * 
	 * @return the y coordinate
	 */
	public int getY();

	/**
	 * Gets the z coordinate of the shopkeeper.
	 * 
	 * @return the z coordinate
	 */
	public int getZ();

	/**
	 * Gets the yaw of the shopkeeper, i.e. its horizontal orientation.
	 * <p>
	 * The yaw is set when the shopkeeper is created or repositioned. This is the default horizontal orientation that
	 * the shopkeeper spawns its shop object in. If the shop object is able to rotate, this yaw may not match the shop
	 * object's current yaw.
	 * 
	 * @return the yaw
	 */
	public float getYaw();

	/**
	 * Gets a String representation of the shopkeeper's location.
	 * 
	 * @return the String representation
	 */
	public String getPositionString();

	/**
	 * Gets the shopkeeper's location.
	 * <p>
	 * This returns <code>null</code> if the shopkeeper is {@link #isVirtual() virtual} or if the world is not loaded.
	 * 
	 * @return the location of the shopkeeper, or <code>null</code> if the shopkeeper is virtual or if the world isn't
	 *         loaded
	 */
	public Location getLocation();

	/**
	 * Gets the {@link ChunkCoords} identifying the chunk this shopkeeper spawns in.
	 * 
	 * @return the chunk coordinates, or <code>null</code> if this shopkeeper is virtual
	 * @see #isVirtual()
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

	/**
	 * Sets the name of the shopkeeper.
	 * 
	 * @param name
	 *            the new name, or <code>null</code> or empty to clear the shopkeeper's name
	 */
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
	 * Checks whether this shopkeeper has {@link #getTradingRecipes(Player) trading recipes} for the given player.
	 * <p>
	 * Ignoring exceptional cases, this method is expected to behave consistently with
	 * {@link #getTradingRecipes(Player)}, but will usually be cheaper to invoke.
	 * 
	 * @param player
	 *            the trading player, or <code>null</code> to not take player-specific trading recipes into account
	 * @return <code>true</code> if there are trading recipes for the given player
	 */
	public boolean hasTradingRecipes(Player player);

	/**
	 * Gets the shopkeeper's current trading recipes for the given player.
	 * <p>
	 * Depending on the type of this shopkeeper, this might access the world (eg. check container contents) in order to
	 * determine the available stock.
	 * <p>
	 * Managing (adding, removing, editing) the trading recipes usually differs depending on the type of shopkeeper and
	 * is therefore not part of the general {@link Shopkeeper} interface.
	 * <p>
	 * The <code>player</code> parameter can be used to request player-specific trading recipes, if this type of
	 * shopkeeper supports that.
	 * 
	 * @param player
	 *            the trading player, or <code>null</code> to not take player-specific trading recipes into account
	 * @return an unmodifiable view on the current trading recipes of this shopkeeper for the given player
	 */
	public List<? extends TradingRecipe> getTradingRecipes(Player player);

	// SHOPKEEPER UIs

	/**
	 * Gets all currently active {@link UISession UI sessions} involving this shopkeeper.
	 * 
	 * @return an unmodifiable view on the current UI sessions
	 * @see UIRegistry#getUISessions(Shopkeeper)
	 */
	public Collection<? extends UISession> getUISessions();

	/**
	 * Gets all currently active {@link UISession UI sessions} involving this shopkeeper and the specified
	 * {@link UIType}.
	 * 
	 * @param uiType
	 *            the UI type
	 * @return an unmodifiable view on the current UI sessions
	 * @see UIRegistry#getUISessions(Shopkeeper, UIType)
	 */
	public Collection<? extends UISession> getUISessions(UIType uiType);

	/**
	 * {@link UISession#deactivateUI() Deactivates} all currently active UIs (trading, editing, hiring, etc.) involving
	 * this shopkeeper and {@link UISession#abort() aborts} them within the next tick.
	 */
	public void abortUISessionsDelayed();

	/**
	 * Attempts to open the interface for the given {@link UIType} for the specified player.
	 * <p>
	 * This fails if this shopkeeper doesn't support the specified interface type, if the player cannot open this
	 * interface type for this shopkeeper (for example because of missing permissions), or if something else goes wrong.
	 * 
	 * @param uiType
	 *            the requested UI type
	 * @param player
	 *            the player requesting the interface
	 * @return <code>true</code> the player's request was successful and the interface was opened, false otherwise
	 */
	public boolean openWindow(UIType uiType, Player player);

	// Shortcuts for the default UI types:

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
