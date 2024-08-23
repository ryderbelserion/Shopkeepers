package com.nisovin.shopkeepers.shopkeeper.registry;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.activation.ShopkeeperChunkActivator;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Queries that combine information from the {@link ShopkeeperChunkMap} and the
 * {@link ShopkeeperChunkActivator}.
 */
public class ActiveChunkQueries {

	private final ShopkeeperChunkMap shopkeeperChunkMap;
	private final ShopkeeperChunkActivator shopkeeperActivator;

	// Note: Already unmodifiable.
	private final Set<? extends AbstractShopkeeper> shopkeepersInActiveChunksView = new AbstractSet<AbstractShopkeeper>() {
		@Override
		public Iterator<AbstractShopkeeper> iterator() {
			return Unsafe.assertNonNull(shopkeeperChunkMap).getWorldsWithShopkeepers().stream()
					.<AbstractShopkeeper>flatMap(worldName -> {
						return Unsafe.initialized(ActiveChunkQueries.this)
								.getShopkeepersInActiveChunks(worldName)
								.stream();
					}).iterator();
		}

		@Override
		public int size() {
			return Unsafe.assertNonNull(shopkeeperChunkMap).getWorldsWithShopkeepers().stream()
					.mapToInt(worldName -> {
						return Unsafe.initialized(ActiveChunkQueries.this)
								.getShopkeepersInActiveChunks(worldName)
								.size();
					}).sum();
		}
	};

	ActiveChunkQueries(
			ShopkeeperChunkMap shopkeeperChunkMap,
			ShopkeeperChunkActivator shopkeeperActivator
	) {
		Validate.notNull(shopkeeperChunkMap, "shopkeeperChunkMap is null");
		Validate.notNull(shopkeeperActivator, "shopkeeperActivator is null");
		this.shopkeeperChunkMap = shopkeeperChunkMap;
		this.shopkeeperActivator = shopkeeperActivator;
	}

	private boolean isChunkActive(ChunkCoords chunkCoords) {
		assert chunkCoords != null;
		return shopkeeperActivator.isChunkActive(chunkCoords);
	}

	// QUERIES

	public Set<? extends AbstractShopkeeper> getShopkeepersInActiveChunks() {
		return shopkeepersInActiveChunksView;
	}

	// TODO Cache these query objects per world?

	public Set<? extends ChunkCoords> getActiveChunks(String worldName) {
		WorldShopkeepers worldShopkeepers = shopkeeperChunkMap.getWorldShopkeepers(worldName);
		if (worldShopkeepers == null) return Collections.emptySet();

		// Note: Already unmodifiable.
		Set<? extends ChunkCoords> activeChunksView = new AbstractSet<ChunkCoords>() {
			@Override
			public Iterator<ChunkCoords> iterator() {
				return Unsafe.cast(worldShopkeepers.getShopkeepersByChunk().keySet().stream()
						.filter(ActiveChunkQueries.this::isChunkActive)
						.iterator());
			}

			@Override
			public int size() {
				return worldShopkeepers.getShopkeepersByChunk().keySet().stream()
						.filter(ActiveChunkQueries.this::isChunkActive)
						.mapToInt(chunkCoords -> 1)
						.sum();
			}
		};
		return activeChunksView;
	}

	public Set<? extends AbstractShopkeeper> getShopkeepersInActiveChunks(String worldName) {
		WorldShopkeepers worldShopkeepers = shopkeeperChunkMap.getWorldShopkeepers(worldName);
		if (worldShopkeepers == null) return Collections.emptySet();

		// Note: Already unmodifiable.
		Set<? extends AbstractShopkeeper> shopkeepersInActiveChunksView = new AbstractSet<AbstractShopkeeper>() {
			@Override
			public Iterator<AbstractShopkeeper> iterator() {
				return worldShopkeepers.getShopkeepersByChunk().entrySet().stream()
						.filter(chunkEntry -> isChunkActive(chunkEntry.getKey()))
						.<AbstractShopkeeper>flatMap(chunkEntry -> chunkEntry.getValue().stream())
						.iterator();
			}

			@Override
			public int size() {
				return worldShopkeepers.getShopkeepersByChunk().entrySet().stream()
						.filter(chunkEntry -> isChunkActive(chunkEntry.getKey()))
						.mapToInt(chunkEntry -> chunkEntry.getValue().size())
						.sum();
			}
		};
		return shopkeepersInActiveChunksView;
	}
}
