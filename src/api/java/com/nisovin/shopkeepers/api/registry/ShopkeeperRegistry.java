package com.nisovin.shopkeepers.api.registry;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.api.Shopkeeper;
import com.nisovin.shopkeepers.api.util.ChunkCoords;

/**
 * Keeps track of all loaded shopkeepers and handles their activation and deactivation.
 */
public interface ShopkeeperRegistry {

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
