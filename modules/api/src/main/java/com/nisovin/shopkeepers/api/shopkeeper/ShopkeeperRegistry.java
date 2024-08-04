package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.util.ChunkCoords;

/**
 * Keeps track of all loaded shopkeepers and handles their activation and deactivation.
 */
public interface ShopkeeperRegistry {

	// SHOPKEEPER CREATION

	/**
	 * Creates a shopkeeper from the given creation data and spawns it into the world.
	 * 
	 * @param creationData
	 *            the shop creation data containing the necessary arguments (spawn location, object
	 *            type, owner, etc.) for creating the shopkeeper
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created
	 */
	public Shopkeeper createShopkeeper(ShopCreationData creationData)
			throws ShopkeeperCreateException;

	// QUERYING

	/**
	 * Gets all shopkeepers.
	 * 
	 * @return an unmodifiable view on all shopkeepers
	 */
	public Collection<? extends Shopkeeper> getAllShopkeepers();

	/**
	 * Gets all {@link Shopkeeper#isVirtual() virtual} shopkeepers.
	 * 
	 * @return an unmodifiable view on the virtual shopkeepers
	 */
	public Collection<? extends Shopkeeper> getVirtualShopkeepers();

	/**
	 * Gets the shopkeeper by its {@link Shopkeeper#getUniqueId() unique id}.
	 * 
	 * @param shopkeeperUniqueId
	 *            the shopkeeper's unique id
	 * @return the shopkeeper for the given unique id, or <code>null</code>
	 */
	public @Nullable Shopkeeper getShopkeeperByUniqueId(UUID shopkeeperUniqueId);

	/**
	 * Gets the shopkeeper by its {@link Shopkeeper#getId() id}.
	 * 
	 * @param shopkeeperId
	 *            the shopkeeper's id
	 * @return the shopkeeper for the given id, or <code>null</code>
	 */
	public @Nullable Shopkeeper getShopkeeperById(int shopkeeperId);

	// PLAYER SHOPS

	/**
	 * Gets all player shopkeepers.
	 * 
	 * @return an unmodifiable view on all shopkeepers
	 */
	public Collection<? extends PlayerShopkeeper> getAllPlayerShopkeepers();

	/**
	 * Gets the player shopkeepers owned by the specified player.
	 * 
	 * @param ownerUUID
	 *            the owner uuid
	 * @return an unmodifiable view on the player shopkeepers
	 */
	public Collection<? extends PlayerShopkeeper> getPlayerShopkeepersByOwner(UUID ownerUUID);

	// BY NAME

	/**
	 * Searches for shopkeepers whose names match the given name.
	 * <p>
	 * The comparison of shop names ignores case, colors and normalizes whitespace.
	 * <p>
	 * Note that shopkeeper names are not unique.
	 * <p>
	 * The returned {@link Stream} may lazily search for only as many matching shopkeepers as
	 * required.
	 * 
	 * @param shopName
	 *            the shop name
	 * @return a stream over the matching shopkeepers
	 */
	public Stream<? extends Shopkeeper> getShopkeepersByName(String shopName);

	/**
	 * Searches for shopkeepers whose names start with the specified prefix.
	 * <p>
	 * The comparison of shop names ignores case, colors and normalizes whitespace.
	 * <p>
	 * Note that shopkeeper names are not unique.
	 * <p>
	 * The returned {@link Stream} may lazily search for only as many matching shopkeepers as
	 * required.
	 * 
	 * @param shopNamePrefix
	 *            the shop name prefix
	 * @return a stream over the matching shopkeepers
	 */
	public Stream<? extends Shopkeeper> getShopkeepersByNamePrefix(String shopNamePrefix);

	// BY WORLD

	/**
	 * Gets the names of all worlds that contain shopkeepers.
	 * 
	 * @return an unmodifiable view on the world names
	 */
	public Collection<? extends String> getWorldsWithShopkeepers();

	/**
	 * Gets all shopkeepers in the specified world.
	 * 
	 * @param worldName
	 *            the world name
	 * @return an unmodifiable view on the shopkeepers, may be empty
	 */
	public Collection<? extends Shopkeeper> getShopkeepersInWorld(String worldName);

	/**
	 * Gets all shopkeepers in the specified world grouped by the chunks they are in.
	 * 
	 * @param worldName
	 *            the world name
	 * @return an unmodifiable view on the shopkeepers grouped by chunks
	 */
	public Map<? extends ChunkCoords, ? extends Collection<? extends Shopkeeper>> getShopkeepersByChunks(
			String worldName
	);

	// ACTIVE CHUNKS

	/**
	 * Gets the currently active chunks in the specified world.
	 * <p>
	 * Chunks are activated and deactivated when they are loaded and unloaded. During activation,
	 * the shopkeepers inside the chunk are spawned.
	 * <p>
	 * However, in order to not spawn shopkeepers for chunks that only stay briefly loaded, the
	 * activation of chunks may be deferred. Consequently, this might not return chunks even if they
	 * are currently already loaded.
	 * 
	 * @param worldName
	 *            the world name
	 * @return an unmodifiable view on the active chunks
	 */
	public Collection<? extends ChunkCoords> getActiveChunks(String worldName);

	/**
	 * Checks if the specified chunk is active.
	 * 
	 * @param chunkCoords
	 *            the chunk
	 * @return <code>true</code> if the chunk is active
	 * @see #getActiveChunks(String)
	 */
	public boolean isChunkActive(ChunkCoords chunkCoords);

	/**
	 * Gets all shopkeepers in currently {@link #getActiveChunks(String) active chunks}.
	 * <p>
	 * Since the chunk activation may be deferred from chunk loading, this might not return
	 * shopkeepers even if their chunk is currently already loaded.
	 * <p>
	 * Similarly, the spawning of {@link ShopObject shop objects} may be deferred from chunk
	 * activation, or even fail. This is not reflected by this method, which only considers the
	 * activation state of chunks as a whole. Use {@link ShopObject#isSpawned()} or
	 * {@link ShopObject#isActive()} to check if a shop object is actually spawned and active.
	 * <p>
	 * The opposite situation can also apply: Some types of shop objects may handle their spawning
	 * and despawning independently of chunk activations. It is therefore possible for a shop object
	 * to already be spawned and active before its corresponding chunk has been activated.
	 * 
	 * @return an unmodifiable view on the active shopkeepers
	 */
	public Collection<? extends Shopkeeper> getActiveShopkeepers();

	/**
	 * Gets all shopkeepers in currently {@link #getActiveChunks(String) active chunks} in the
	 * specified world.
	 * <p>
	 * Since the chunk activation may be deferred from chunk loading, this might not return
	 * shopkeepers even if their chunk is currently already loaded.
	 * <p>
	 * Similarly, the spawning of {@link ShopObject shop objects} may be deferred from chunk
	 * activation, or even fail. This is not reflected by this method, which only considers the
	 * activation state of chunks as a whole. Use {@link ShopObject#isSpawned()} or
	 * {@link ShopObject#isActive()} to check if a shop object is actually spawned and active.
	 * <p>
	 * The opposite situation can also apply: Some types of shop objects may handle their spawning
	 * and despawning independently of chunk activations. It is therefore possible for a shop object
	 * to already be spawned and active before its corresponding chunk has been activated.
	 * 
	 * @param worldName
	 *            the world name
	 * @return an unmodifiable view on the active shopkeepers
	 */
	public Collection<? extends Shopkeeper> getActiveShopkeepers(String worldName);

	// BY CHUNK

	/**
	 * Gets all shopkeepers in the specified chunk.
	 * 
	 * @param chunkCoords
	 *            the chunk
	 * @return an unmodifiable view on the shopkeepers, may be empty
	 */
	public Collection<? extends Shopkeeper> getShopkeepersInChunk(ChunkCoords chunkCoords);

	// BY LOCATION

	/**
	 * Gets all shopkeepers at the specified location.
	 * 
	 * @param location
	 *            the location
	 * @return an unmodifiable view on the shopkeepers, may be empty
	 */
	// TODO Replace with getShopkeeperAtLocation? We already prevent players from creating more than
	// a single shopkeeper at the same location.
	public Collection<? extends Shopkeeper> getShopkeepersAtLocation(Location location);

	// BY SHOP OBJECT

	/**
	 * Gets the shopkeeper that is represented by the given entity.
	 * <p>
	 * The return value may only be accurate if the shopkeeper's {@link ShopObject} is currently
	 * {@link ShopObject#isSpawned() spawned}. For example, if the shopkeeper's chunk is not
	 * {@link #isChunkActive(ChunkCoords) active}, or if the entity is no longer
	 * {@link Entity#isValid() valid}, this may or may not return <code>false</code> for that
	 * entity.
	 * 
	 * @param entity
	 *            the entity
	 * @return the shopkeeper, or <code>null</code> if the given entity is not a shopkeeper
	 */
	public @Nullable Shopkeeper getShopkeeperByEntity(Entity entity);

	/**
	 * Checks if the given entity is a shopkeeper.
	 * 
	 * @param entity
	 *            the entity
	 * @return <code>true</code> if the entity is a shopkeeper
	 * @see #getShopkeeperByEntity(Entity)
	 */
	public boolean isShopkeeper(Entity entity);

	/**
	 * Gets the shopkeeper that is represented by the given block (for example in case of sign
	 * shops).
	 * <p>
	 * The return value may only be accurate if the shopkeeper's {@link ShopObject} is currently
	 * {@link ShopObject#isSpawned() spawned}. For example, if the shopkeeper's chunk is not
	 * {@link #isChunkActive(ChunkCoords) active}, or if the block could not be placed with its
	 * intended state, this may or may not return <code>false</code> for that block.
	 * <p>
	 * In order to get the shopkeepers at a specific location, regardless of whether that chunk is
	 * currently loaded and whether the shopkeepers inside it have already been spawned, use
	 * {@link #getShopkeepersAtLocation(Location)} instead.
	 * 
	 * @param block
	 *            the block
	 * @return the shopkeeper, or <code>null</code> if the given block is not a shopkeeper
	 */
	public @Nullable Shopkeeper getShopkeeperByBlock(Block block);

	/**
	 * Gets the shopkeeper that is represented by the block at the specified coordinates (for
	 * example in the case of sign shops).
	 * <p>
	 * The return value may only be accurate if the shopkeeper's {@link ShopObject} is currently
	 * {@link ShopObject#isSpawned() spawned}. For example, if the shopkeeper's chunk is not
	 * {@link #isChunkActive(ChunkCoords) active}, or if the block could not be placed with its
	 * intended state, this may or may not return <code>false</code> for that block.
	 * <p>
	 * In order to get the shopkeepers at a specific location, regardless of whether that chunk is
	 * currently loaded and whether the shopkeepers inside it have already been spawned, use
	 * {@link #getShopkeepersAtLocation(Location)} instead.
	 * 
	 * @param worldName
	 *            the world name
	 * @param x
	 *            the block's x coordinate
	 * @param y
	 *            the block's y coordinate
	 * @param z
	 *            the block's z coordinate
	 * @return the shopkeeper, or <code>null</code> if the specified block is not a shopkeeper
	 */
	public @Nullable Shopkeeper getShopkeeperByBlock(String worldName, int x, int y, int z);

	/**
	 * Checks if the given block is a shopkeeper.
	 * 
	 * @param block
	 *            the block
	 * @return <code>true</code> if the block is a shopkeeper
	 * @see #getShopkeeperByBlock(Block)
	 */
	public boolean isShopkeeper(Block block);
}
