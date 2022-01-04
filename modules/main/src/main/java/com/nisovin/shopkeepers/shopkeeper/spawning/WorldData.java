package com.nisovin.shopkeepers.shopkeeper.spawning;

import com.nisovin.shopkeepers.shopkeeper.spawning.WorldSaveDespawner.RespawnShopkeepersAfterWorldSaveTask;
import com.nisovin.shopkeepers.util.java.Validate;

final class WorldData {

	private final String worldName;
	private RespawnShopkeepersAfterWorldSaveTask worldSaveRespawnTask = null;

	WorldData(String worldName) {
		Validate.notNull(worldName, "worldName is null");
		this.worldName = worldName;
	}

	public String getWorldName() {
		return worldName;
	}

	boolean isWorldSaveRespawnPending() {
		return (worldSaveRespawnTask != null);
	}

	void setWorldSaveRespawnTask(RespawnShopkeepersAfterWorldSaveTask worldSaveRespawnTask) {
		this.worldSaveRespawnTask = worldSaveRespawnTask;
	}

	void cancelWorldSaveRespawnTask() {
		if (worldSaveRespawnTask != null) {
			worldSaveRespawnTask.cancel();
			worldSaveRespawnTask = null;
		}
	}

	void cleanUp() {
		this.cancelWorldSaveRespawnTask();
	}
}
