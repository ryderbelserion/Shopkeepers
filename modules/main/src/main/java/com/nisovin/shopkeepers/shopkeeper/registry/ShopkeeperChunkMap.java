package com.nisovin.shopkeepers.shopkeeper.registry;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Stores shopkeepers and provides methods to query them by world and by chunk.
 */
class ShopkeeperChunkMap {

	/**
	 * An instance of this can be registered during the construction of the
	 * {@link ShopkeeperChunkMap} and is then invoked whenever a shopkeeper is added or removed from
	 * the chunk map.
	 */
	static class ChangeListener {

		public void onShopkeeperAdded(
				AbstractShopkeeper shopkeeper,
				ChunkShopkeepers chunkShopkeepers
		) {
		}

		public void onShopkeeperRemoved(
				AbstractShopkeeper shopkeeper,
				ChunkShopkeepers chunkShopkeepers
		) {
		}

		public void onWorldAdded(WorldShopkeepers worldShopkeepers) {
		}

		public void onWorldRemoved(WorldShopkeepers worldShopkeepers) {
		}

		public void onChunkAdded(ChunkShopkeepers chunkShopkeepers) {
		}

		public void onChunkRemoved(ChunkShopkeepers chunkShopkeepers) {
		}
	}

	// By world name:
	private final Map<String, WorldShopkeepers> shopkeepersByWorld = new LinkedHashMap<>();
	private final Set<String> shopkeeperWorldsView = Collections.unmodifiableSet(shopkeepersByWorld.keySet());

	private final ChangeListener changeListener; // Not null

	ShopkeeperChunkMap() {
		this(new ChangeListener());
	}

	ShopkeeperChunkMap(ChangeListener changeListener) {
		Validate.notNull(changeListener, "changeListener is null");
		this.changeListener = changeListener;
	}

	// Returns null if there are no shopkeepers in the specified world.
	@Nullable
	WorldShopkeepers getWorldShopkeepers(String worldName) {
		return shopkeepersByWorld.get(worldName);
	}

	// Returns null if there are no shopkeepers in the specified chunk:
	@Nullable
	ChunkShopkeepers getChunkShopkeepers(@Nullable ChunkCoords chunkCoords) {
		if (chunkCoords == null) return null;
		String worldName = chunkCoords.getWorldName();
		WorldShopkeepers worldShopkeepers = this.getWorldShopkeepers(worldName);
		if (worldShopkeepers == null) return null; // There are no shopkeepers in this world
		return worldShopkeepers.getChunkShopkeepers(chunkCoords);
	}

	// Only called for non-virtual shopkeepers.
	ChunkShopkeepers addShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null && !shopkeeper.isVirtual();
		assert shopkeeper.getLastChunkCoords() == null;
		String worldName = Unsafe.assertNonNull(shopkeeper.getWorldName());
		ChunkCoords shopkeeperChunk = Unsafe.assertNonNull(shopkeeper.getChunkCoords());
		assert worldName.equals(shopkeeperChunk.getWorldName());
		WorldShopkeepers worldShopkeepers = shopkeepersByWorld.computeIfAbsent(
				worldName,
				WorldShopkeepers::new
		);
		assert worldShopkeepers != null;
		ChunkShopkeepers chunkShopkeepers = worldShopkeepers.addShopkeeper(shopkeeper);

		// Inform change listener:
		if (worldShopkeepers.getShopkeeperCount() == 1) {
			changeListener.onWorldAdded(worldShopkeepers);
		}
		if (chunkShopkeepers.getShopkeepers().size() == 1) {
			changeListener.onChunkAdded(chunkShopkeepers);
		}
		changeListener.onShopkeeperAdded(shopkeeper, chunkShopkeepers);
		return chunkShopkeepers;
	}

	// Only called for non-virtual shopkeepers.
	@Nullable
	ChunkShopkeepers removeShopkeeper(AbstractShopkeeper shopkeeper) {
		return this.removeShopkeeper(shopkeeper, false);
	}

	private @Nullable ChunkShopkeepers removeShopkeeper(
			AbstractShopkeeper shopkeeper,
			boolean skipWorldCleanup
	) {
		assert shopkeeper != null && !shopkeeper.isVirtual();
		ChunkCoords lastChunkCoords = Unsafe.assertNonNull(shopkeeper.getLastChunkCoords());
		String worldName = lastChunkCoords.getWorldName();
		WorldShopkeepers worldShopkeepers = shopkeepersByWorld.get(worldName);
		if (worldShopkeepers == null) return null; // Could not find the shopkeeper

		ChunkShopkeepers chunkShopkeepers = worldShopkeepers.removeShopkeeper(shopkeeper);
		boolean worldRemoved = false;
		if (!skipWorldCleanup && worldShopkeepers.getShopkeeperCount() == 0) {
			worldRemoved = true;
			shopkeepersByWorld.remove(worldName);
		}

		// Inform change listener:
		changeListener.onShopkeeperRemoved(shopkeeper, chunkShopkeepers);
		if (chunkShopkeepers.getShopkeepers().isEmpty()) {
			changeListener.onChunkRemoved(chunkShopkeepers);
		}
		if (worldRemoved) {
			changeListener.onWorldRemoved(worldShopkeepers);
		}
		return chunkShopkeepers;
	}

	// Updates the shopkeeper's location inside the chunk map, moving it from its previous chunk to
	// its current chunk.
	// Returns true if the shopkeeper was moved to a different chunk.
	boolean moveShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		ChunkCoords oldChunk = Unsafe.assertNonNull(shopkeeper.getLastChunkCoords());
		ChunkCoords newChunk = Unsafe.assertNonNull(shopkeeper.getChunkCoords());
		if (newChunk.equals(oldChunk)) {
			// The shopkeeper's chunk did not change.
			return false;
		}

		// If the shopkeeper is moved from one chunk to another within the same world, we skip any
		// world data cleanup.
		boolean skipWorldCleanup = oldChunk.getWorldName().equals(newChunk.getWorldName());
		this.removeShopkeeper(shopkeeper, skipWorldCleanup);
		this.addShopkeeper(shopkeeper);
		return true;
	}

	void ensureEmpty() {
		if (!shopkeepersByWorld.isEmpty()) {
			Log.warning("Some shopkeepers were not properly removed from the chunk map!");
			shopkeepersByWorld.clear();
		}
	}

	// QUERIES

	public Collection<? extends String> getWorldsWithShopkeepers() {
		return shopkeeperWorldsView;
	}
}
