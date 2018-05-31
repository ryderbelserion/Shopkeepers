package com.nisovin.shopkeepers.api;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang.Validate;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shoptypes.DefaultShopTypes;
import com.nisovin.shopkeepers.api.shoptypes.ShopType;
import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;
import com.nisovin.shopkeepers.api.types.SelectableTypeRegistry;
import com.nisovin.shopkeepers.api.ui.UIRegistry;
import com.nisovin.shopkeepers.api.util.ChunkCoords;

public final class ShopkeepersAPI {

	private ShopkeepersAPI() {
	}

	private static ShopkeepersPlugin plugin = null;

	public static void enable(ShopkeepersPlugin plugin) {
		Validate.notNull(plugin, "Plugin is null!");
		Validate.isTrue(ShopkeepersAPI.plugin == null, "API is already enabled!");
		ShopkeepersAPI.plugin = plugin;
	}

	public static void disable() {
		Validate.notNull(ShopkeepersAPI.plugin, "API is already disabled!");
		ShopkeepersAPI.plugin = null;
	}

	public static ShopkeepersPlugin getPlugin() {
		if (plugin == null) {
			throw new IllegalStateException("API is not enabled!");
		}
		return plugin;
	}

	// PERMISSIONS

	/**
	 * Checks if the given player has the permission to create any shopkeeper.
	 * 
	 * @param player
	 *            the player
	 * @return <code>false</code> if he cannot create shops at all, <code>true</code> otherwise
	 */
	public static boolean hasCreatePermission(Player player) {
		return getPlugin().hasCreatePermission(player);
	}

	// SHOP TYPES

	public static SelectableTypeRegistry<? extends ShopType<?>> getShopTypeRegistry() {
		return getPlugin().getShopTypeRegistry();
	}

	public static DefaultShopTypes getDefaultShopTypes() {
		return getPlugin().getDefaultShopTypes();
	}

	// SHOP OBJECT TYPES

	public static SelectableTypeRegistry<? extends ShopObjectType<?>> getShopObjectTypeRegistry() {
		return getPlugin().getShopObjectTypeRegistry();
	}

	public static DefaultShopObjectTypes getDefaultShopObjectTypes() {
		return getPlugin().getDefaultShopObjectTypes();
	}
	// UI

	public static UIRegistry<?> getUIRegistry() {
		return getPlugin().getUIRegistry();
	}

	// STORAGE

	/**
	 * Gets the {@link ShopkeeperStorage}.
	 * 
	 * @return the shopkeeper storage
	 */
	public static ShopkeeperStorage getShopkeeperStorage() {
		return getPlugin().getShopkeeperStorage();
	}

	// SHOPKEEPER REGISTRY

	/**
	 * Creates a new shopkeeper and spawns it into the world.
	 * 
	 * @param shopCreationData
	 *            the shop creation data containing the necessary arguments (spawn location, object type, etc.) for
	 *            creating this shopkeeper
	 * @return the new shopkeeper, or <code>null</code> if the creation wasn't successful for some reason
	 */
	public static Shopkeeper createShopkeeper(ShopCreationData shopCreationData) {
		return getPlugin().createShopkeeper(shopCreationData);
	}

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
	public static Shopkeeper getShopkeeper(UUID shopkeeperId) {
		return getPlugin().getShopkeeper(shopkeeperId);
	}

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
	public static Shopkeeper getShopkeeper(int shopkeeperSessionId) {
		return getPlugin().getShopkeeper(shopkeeperSessionId);
	}

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
	public static Shopkeeper getShopkeeperByName(String shopName) {
		return getPlugin().getShopkeeperByName(shopName);
	}

	/**
	 * Gets the shopkeeper for a given entity.
	 * 
	 * @param entity
	 *            the entity
	 * @return the shopkeeper, or <code>null</code> if the given entity is not a shopkeeper
	 */
	public static Shopkeeper getShopkeeperByEntity(Entity entity) {
		return getPlugin().getShopkeeperByEntity(entity);
	}

	/**
	 * Gets the shopkeeper for a given block (ex: sign shops).
	 * 
	 * @param block
	 *            the block
	 * @return the shopkeeper, or <code>null</code> if the given block is not a shopkeeper
	 */
	public static Shopkeeper getShopkeeperByBlock(Block block) {
		return getPlugin().getShopkeeperByBlock(block);
	}

	/**
	 * Gets all shopkeepers for a given chunk.
	 * 
	 * @param chunk
	 *            the chunk
	 * @return an unmodifiable list of the shopkeepers in the specified chunk, empty if there are none
	 */
	public static List<? extends Shopkeeper> getShopkeepersInChunk(Chunk chunk) {
		return getPlugin().getShopkeepersInChunk(chunk);
	}

	/**
	 * Gets all shopkeepers for a given chunk.
	 * 
	 * @param chunkCoords
	 *            specifies the chunk
	 * @return an unmodifiable list of the shopkeepers in the specified chunk, empty if there are none
	 */
	public static List<? extends Shopkeeper> getShopkeepersInChunk(ChunkCoords chunkCoords) {
		return getPlugin().getShopkeepersInChunk(chunkCoords);
	}

	/**
	 * Gets all shopkeepers in the specified world.
	 * 
	 * @param world
	 *            the world
	 * @param onlyLoadedChunks
	 *            <code>true</code> to only include shopkeepers from loaded chunks
	 * @return an unmodifiable view on the shopkeepers
	 */
	public static List<? extends Shopkeeper> getShopkeepersInWorld(World world, boolean onlyLoadedChunks) {
		return getPlugin().getShopkeepersInWorld(world, onlyLoadedChunks);
	}

	/**
	 * Checks if a given entity is a Shopkeeper.
	 * 
	 * @param entity
	 *            the entity to check
	 * @return whether the entity is a Shopkeeper
	 */
	public static boolean isShopkeeper(Entity entity) {
		return getPlugin().isShopkeeper(entity);
	}

	/**
	 * Gets all shopkeepers.
	 * 
	 * @return an unmodifiable view on all shopkeepers
	 */
	public static Collection<? extends Shopkeeper> getAllShopkeepers() {
		return getPlugin().getAllShopkeepers();
	}

	/**
	 * Gets all shopkeepers grouped by the chunks they are in.
	 * 
	 * @return an unmodifiable view on all shopkeepers grouped by the chunks they are in
	 */
	public static Map<ChunkCoords, ? extends List<? extends Shopkeeper>> getAllShopkeepersByChunks() {
		return getPlugin().getAllShopkeepersByChunks();
	}

	/**
	 * Gets all active shopkeepers. Some shopkeeper types might be always active (like sign shops),
	 * others are only active as long as their chunk they are in is loaded.
	 * 
	 * @return an unmodifiable view on all active shopkeepers
	 */
	public static Collection<? extends Shopkeeper> getActiveShopkeepers() {
		return getPlugin().getActiveShopkeepers();
	}
}
