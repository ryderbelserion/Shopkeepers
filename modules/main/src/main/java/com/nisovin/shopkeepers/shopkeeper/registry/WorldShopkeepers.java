package com.nisovin.shopkeepers.shopkeeper.registry;

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;

final class WorldShopkeepers {

	private final String worldName;
	private final Map<@NonNull ChunkCoords, @NonNull ChunkShopkeepers> shopkeepersByChunk = new HashMap<>();
	// Unmodifiable entries:
	private final Map<@NonNull ChunkCoords, @NonNull List<? extends @NonNull AbstractShopkeeper>> shopkeeperViewsByChunk = new LinkedHashMap<>();
	// Unmodifiable map with unmodifiable entries:
	private final Map<@NonNull ChunkCoords, @NonNull List<? extends @NonNull AbstractShopkeeper>> shopkeepersByChunkView = Collections.unmodifiableMap(shopkeeperViewsByChunk);
	private int shopkeeperCount = 0;

	// Note: Already unmodifiable.
	private final Set<? extends @NonNull AbstractShopkeeper> shopkeepersView = new AbstractSet<@NonNull AbstractShopkeeper>() {
		@Override
		public Iterator<AbstractShopkeeper> iterator() {
			if (this.isEmpty()) {
				return Collections.emptyIterator();
			}
			return shopkeepersByChunkView.values().stream()
					.<AbstractShopkeeper>flatMap(Collection::stream)
					.iterator();
		}

		@Override
		public int size() {
			return shopkeeperCount;
		}
	};

	WorldShopkeepers(String worldName) {
		Validate.notEmpty(worldName, "worldName is null or empty");
		this.worldName = worldName;
	}

	public String getWorldName() {
		return worldName;
	}

	// Returns null if there are no shopkeepers in the specified chunk:
	@Nullable
	ChunkShopkeepers getChunkShopkeepers(ChunkCoords chunkCoords) {
		assert chunkCoords != null;
		assert chunkCoords.getWorldName().equals(this.getWorldName());
		return shopkeepersByChunk.get(chunkCoords);
	}

	ChunkShopkeepers addShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		assert shopkeeper.getLastChunkCoords() == null;
		ChunkCoords chunkCoords = Unsafe.assertNonNull(shopkeeper.getChunkCoords());
		assert chunkCoords.getWorldName().equals(this.getWorldName());
		ChunkShopkeepers chunkShopkeepers = shopkeepersByChunk.computeIfAbsent(
				chunkCoords,
				chkCoords -> {
					ChunkShopkeepers newChunkShopkeepers = new ChunkShopkeepers(chkCoords);
					shopkeeperViewsByChunk.put(chkCoords, newChunkShopkeepers.getShopkeepers());
					return newChunkShopkeepers;
				}
		);
		assert chunkShopkeepers != null;
		assert !chunkShopkeepers.getShopkeepers().contains(shopkeeper);
		chunkShopkeepers.addShopkeeper(shopkeeper);
		shopkeeperCount += 1;
		return chunkShopkeepers;
	}

	ChunkShopkeepers removeShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		ChunkCoords chunkCoords = Unsafe.assertNonNull(shopkeeper.getLastChunkCoords());
		assert chunkCoords.getWorldName().equals(this.getWorldName());
		ChunkShopkeepers chunkShopkeepers = Unsafe.assertNonNull(shopkeepersByChunk.get(chunkCoords));
		assert chunkShopkeepers.getShopkeepers().contains(shopkeeper);
		chunkShopkeepers.removeShopkeeper(shopkeeper);
		shopkeeperCount -= 1;
		if (chunkShopkeepers.getShopkeepers().isEmpty()) {
			shopkeepersByChunk.remove(chunkCoords);
			shopkeeperViewsByChunk.remove(chunkCoords);
		}
		return chunkShopkeepers;
	}

	// QUERIES

	public int getShopkeeperCount() {
		return shopkeeperCount;
	}

	public Set<? extends @NonNull AbstractShopkeeper> getShopkeepers() {
		return shopkeepersView;
	}

	public Map<? extends @NonNull ChunkCoords, ? extends @NonNull List<? extends @NonNull AbstractShopkeeper>> getShopkeepersByChunk() {
		return shopkeepersByChunkView;
	}
}
