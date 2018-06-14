package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.util.ChunkCoords;

/**
 * Keeps track of all loaded shopkeepers and handles their activation and deactivation.
 */
public interface ShopkeeperRegistry {

	// SHOPKEEPER CREATION

	/**
	 * Creates a shopkeeper from the given creation data and spawns it into the world.
	 * 
	 * @param id
	 *            the shopkeepers id
	 * @param creationData
	 *            the shop creation data containing the necessary arguments (spawn location, object type, owner, etc.)
	 *            for creating the shopkeeper
	 * @return the created Shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created
	 */
	public Shopkeeper createShopkeeper(ShopCreationData creationData) throws ShopkeeperCreateException;

	/**
	 * Recreates a shopkeeper by loading its previously saved data from the given config section.
	 * 
	 * @param shopType
	 *            the shop type
	 * @param id
	 *            the shopkeepers id
	 * @param configSection
	 *            the config section to load the shopkeeper data from
	 * @return the loaded shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be loaded
	 */
	public Shopkeeper loadShopkeeper(ShopType<?> shopType, int id, ConfigurationSection configSection) throws ShopkeeperCreateException;

	// QUERYING

	/**
	 * Gets the shopkeeper by its {@link Shopkeeper#getUniqueId() unique id}.
	 * 
	 * @param shopkeeperUniqueId
	 *            the shopkeeper's unique id
	 * @return the shopkeeper for the given unique id, or <code>null</code>
	 */
	public Shopkeeper getShopkeeperByUniqueId(UUID shopkeeperUniqueId);

	/**
	 * Gets the shopkeeper by its {@link Shopkeeper#getId() id}.
	 * 
	 * @param shopkeeperId
	 *            the shopkeeper's id
	 * @return the shopkeeper for the given id, or <code>null</code>
	 */
	public Shopkeeper getShopkeeperById(int shopkeeperId);

	/**
	 * Tries to find a shopkeeper with the given name.
	 * 
	 * <p>
	 * This search ignores colors in the shop names.<br>
	 * Note: Shop names are not unique!
	 * 
	 * @param shopName
	 *            the shop name
	 * @return the shopkeeper, or <code>null</code>
	 */
	public Shopkeeper getShopkeeperByName(String shopName);

	public Shopkeeper getActiveShopkeeper(String objectId);

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
