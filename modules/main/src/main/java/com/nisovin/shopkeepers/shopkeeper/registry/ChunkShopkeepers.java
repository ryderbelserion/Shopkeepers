package com.nisovin.shopkeepers.shopkeeper.registry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;

final class ChunkShopkeepers {

	private final ChunkCoords chunkCoords;
	// List instead of Set or Map: We don't expect there to be excessive amounts of shopkeepers
	// inside a single chunk, so removal from the list should be sufficiently fast.
	private final List<AbstractShopkeeper> shopkeepers = new ArrayList<>();
	private final List<? extends AbstractShopkeeper> shopkeepersView = Collections.unmodifiableList(shopkeepers);
	// Unmodifiable:
	private @Nullable List<? extends AbstractShopkeeper> shopkeepersSnapshot = null;

	ChunkShopkeepers(ChunkCoords chunkCoords) {
		Validate.notNull(chunkCoords, "chunkCoords is null");
		this.chunkCoords = chunkCoords;
	}

	public ChunkCoords getChunkCoords() {
		return chunkCoords;
	}

	void addShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		assert shopkeeper.getLastChunkCoords() == null;
		assert this.getChunkCoords().equals(shopkeeper.getChunkCoords());
		assert !this.getShopkeepers().contains(shopkeeper);
		shopkeepers.add(shopkeeper);
		shopkeeper.setLastChunkCoords(chunkCoords);
		shopkeepersSnapshot = null; // Reset snapshot
	}

	void removeShopkeeper(AbstractShopkeeper shopkeeper) {
		assert shopkeeper != null;
		assert this.getChunkCoords().equals(shopkeeper.getLastChunkCoords());
		assert this.getShopkeepers().contains(shopkeeper);
		shopkeepers.remove(shopkeeper);
		shopkeeper.setLastChunkCoords(null);
		shopkeepersSnapshot = null; // Reset snapshot
	}

	// QUERIES

	public List<? extends AbstractShopkeeper> getShopkeepers() {
		return shopkeepersView;
	}

	/**
	 * Gets an unmodifiable snapshot of the current shopkeepers of this chunk that can be iterated
	 * without the risk of encountering a {@link ConcurrentModificationException} if the shopkeepers
	 * of this chunk are modified during the iteration. Any such modifications are not reflected by
	 * the returned snapshot.
	 * <p>
	 * As long as the shopkeepers of this chunk are not modified, this may reuse a previously
	 * created snapshot.
	 * 
	 * @return an unmodifiable snapshot of the chunk's shopkeepers, not <code>null</code>
	 */
	public List<? extends AbstractShopkeeper> getShopkeepersSnapshot() {
		if (shopkeepersSnapshot == null) {
			shopkeepersSnapshot = Collections.unmodifiableList(new ArrayList<>(shopkeepers));
		}
		assert shopkeepersSnapshot != null;
		return shopkeepersSnapshot;
	}
}
