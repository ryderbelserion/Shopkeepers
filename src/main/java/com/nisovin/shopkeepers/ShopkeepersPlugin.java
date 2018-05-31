package com.nisovin.shopkeepers;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shoptypes.DefaultShopTypes;
import com.nisovin.shopkeepers.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.types.SelectableTypeRegistry;
import com.nisovin.shopkeepers.ui.UIRegistry;
import com.nisovin.shopkeepers.util.ChunkCoords;

public interface ShopkeepersPlugin extends Plugin {

	// PERMISSIONS

	public static final String HELP_PERMISSION = "shopkeeper.help";
	public static final String TRADE_PERMISSION = "shopkeeper.trade";
	public static final String RELOAD_PERMISSION = "shopkeeper.reload";
	public static final String DEBUG_PERMISSION = "shopkeeper.debug";
	public static final String LIST_OWN_PERMISSION = "shopkeeper.list.own";
	public static final String LIST_OTHERS_PERMISSION = "shopkeeper.list.others";
	public static final String LIST_ADMIN_PERMISSION = "shopkeeper.list.admin";
	public static final String REMOVE_OWN_PERMISSION = "shopkeeper.remove.own";
	public static final String REMOVE_OTHERS_PERMISSION = "shopkeeper.remove.others";
	public static final String REMOVE_ALL_PERMISSION = "shopkeeper.remove.all";
	public static final String REMOVE_ADMIN_PERMISSION = "shopkeeper.remove.admin";
	public static final String REMOTE_PERMISSION = "shopkeeper.remote";
	public static final String TRANSFER_PERMISSION = "shopkeeper.transfer";
	public static final String SETTRADEPERM_PERMISSION = "shopkeeper.settradeperm";
	public static final String SETFORHIRE_PERMISSION = "shopkeeper.setforhire";
	public static final String HIRE_PERMISSION = "shopkeeper.hire";
	public static final String BYPASS_PERMISSION = "shopkeeper.bypass";
	public static final String ADMIN_PERMISSION = "shopkeeper.admin";
	public static final String PLAYER_NORMAL_PERMISSION = "shopkeeper.player.normal";
	public static final String PLAYER_BUY_PERMISSION = "shopkeeper.player.buy";
	public static final String PLAYER_TRADE_PERMISSION = "shopkeeper.player.trade";
	public static final String PLAYER_BOOK_PERMISSION = "shopkeeper.player.book";

	/**
	 * Checks if the given player has the permission to create any shopkeeper.
	 * 
	 * @param player
	 *            the player
	 * @return <code>false</code> if he cannot create shops at all, <code>true</code> otherwise
	 */
	public boolean hasCreatePermission(Player player);

	// SHOP TYPES

	public SelectableTypeRegistry<? extends ShopType<?>> getShopTypeRegistry();

	public DefaultShopTypes getDefaultShopTypes();

	// SHOP OBJECT TYPES

	public SelectableTypeRegistry<? extends ShopObjectType> getShopObjectTypeRegistry();

	public DefaultShopObjectTypes getDefaultShopObjectTypes();

	// UI

	public UIRegistry getUIRegistry();

	// STORAGE

	/**
	 * Gets the {@link ShopkeeperStorage}.
	 * 
	 * @return the shopkeeper storage
	 */
	public ShopkeeperStorage getShopkeeperStorage();

	// SHOPKEEPER REGISTRY

	/**
	 * Creates a new shopkeeper and spawns it into the world.
	 * 
	 * @param shopCreationData
	 *            the shop creation data containing the necessary arguments (spawn location, object type, etc.) for
	 *            creating this shopkeeper
	 * @return the new shopkeeper, or <code>null</code> if the creation wasn't successful for some reason
	 */
	public Shopkeeper createShopkeeper(ShopCreationData shopCreationData);

	/**
	 * Gets the shopkeeper by its unique id.
	 * 
	 * <p>
	 * Note: This is not the entity uuid, but the id from {@link Shopkeeper#getUniqueId()}.
	 * </p>
	 * 
	 * @param shopkeeperId
	 *            the shopkeeper's unique id
	 * @return the shopkeeper for the given id, or <code>null</code>
	 */
	public Shopkeeper getShopkeeper(UUID shopkeeperId);

	/**
	 * Gets the shopkeeper by its current session id.
	 * 
	 * <p>
	 * This id is only guaranteed to be valid until the next server restart or reload of the shopkeepers. See
	 * {@link Shopkeeper#getSessionId()} for details.
	 * </p>
	 * 
	 * @param shopkeeperSessionId
	 *            the shopkeeper's session id
	 * @return the shopkeeper for the given session id, or <code>null</code>
	 */
	public Shopkeeper getShopkeeper(int shopkeeperSessionId);

	/**
	 * Tries to find a shopkeeper with the given name.
	 * 
	 * <p>
	 * This search ignores colors in the shop names.<br>
	 * Note: Shop names are not unique!
	 * </p>
	 * 
	 * @param shopName
	 *            the shop name
	 * @return the shopkeeper, or <code>null</code>
	 */
	public Shopkeeper getShopkeeperByName(String shopName);

	/**
	 * Gets the shopkeeper for a given entity.
	 * 
	 * @param entity
	 *            the entity
	 * @return the shopkeeper, or <code>null</code> if the given entity is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByEntity(Entity entity);

	/**
	 * Gets the shopkeeper for a given block (ex: sign shops).
	 * 
	 * @param block
	 *            the block
	 * @return the shopkeeper, or <code>null</code> if the given block is not a shopkeeper
	 */
	public Shopkeeper getShopkeeperByBlock(Block block);

	/**
	 * Gets all shopkeepers for a given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @return an unmodifiable list of the shopkeepers in the specified chunk, empty if there are none
	 */
	public List<? extends Shopkeeper> getShopkeepersInChunk(Chunk chunk);

	/**
	 * Gets all shopkeepers for a given chunk.
	 * 
	 * @param chunkCoords
	 *            specifies the chunk
	 * @return an unmodifiable list of the shopkeepers in the specified chunk, empty if there are none
	 */
	public List<? extends Shopkeeper> getShopkeepersInChunk(ChunkCoords chunkCoords);

	/**
	 * Gets all shopkeepers in the specified world.
	 * 
	 * @param world
	 *            the world
	 * @param onlyLoadedChunks
	 *            <code>true</code> to only include shopkeepers from loaded chunks
	 * @return an unmodifiable view on the shopkeepers
	 */
	public List<? extends Shopkeeper> getShopkeepersInWorld(World world, boolean onlyLoadedChunks);

	/**
	 * Checks if a given entity is a Shopkeeper.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return whether the entity is a Shopkeeper
	 */
	public boolean isShopkeeper(Entity entity);

	/**
	 * Gets all shopkeepers.
	 * 
	 * @return an unmodifiable view on all shopkeepers
	 */
	public Collection<? extends Shopkeeper> getAllShopkeepers();

	/**
	 * Gets all shopkeepers grouped by the chunks they are in.
	 * 
	 * @return an unmodifiable view on all shopkeepers grouped by the chunks they are in
	 */
	public Map<ChunkCoords, ? extends List<? extends Shopkeeper>> getAllShopkeepersByChunks();

	/**
	 * Gets all active shopkeepers. Some shopkeeper types might be always active (like sign shops),
	 * others are only active as long as their chunk they are in is loaded.
	 * 
	 * @return an unmodifiable view on all active shopkeepers
	 */
	public Collection<? extends Shopkeeper> getActiveShopkeepers();
}
