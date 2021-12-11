package com.nisovin.shopkeepers.shopkeeper;

import java.util.function.Consumer;

import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.util.taskqueue.TaskQueue;

/**
 * A queue for load balancing the spawning of shopkeepers.
 * <p>
 * Creating and spawning lots of mobs, or placing lots of sign blocks with contents, is comparatively heavy
 * performance-wise. In order to avoid short performance drops (for instance when chunks with lots of shopkeepers are
 * loaded) we use this queue to always only spawn at most a few shopkeepers within the same tick.
 */
// Used by the ShopkeeperRegistry.
public class ShopkeeperSpawnQueue extends TaskQueue<AbstractShopkeeper> {

	// With this configuration we can spawn around ~40 shopkeepers per second.
	// A more frequently running task has a higher general overhead.
	private static final int SPAWN_TASK_PERIOD_TICKS = 3;
	// On my test setup, and without any GC taking place, the spawning of a shopkeeper seems to take between
	// 0.05-0.25ms, with an average of around 0.1ms.
	private static final int SPAWNS_PER_EXECUTION = 6;

	private final Consumer<AbstractShopkeeper> spawner;

	ShopkeeperSpawnQueue(Plugin plugin, Consumer<AbstractShopkeeper> spawner) {
		super(plugin, SPAWN_TASK_PERIOD_TICKS, SPAWNS_PER_EXECUTION);
		assert spawner != null;
		this.spawner = spawner;
	}

	private static class SpawnerTask implements Runnable {

		private final Runnable parentTask;

		SpawnerTask(Runnable parentTask) {
			assert parentTask != null;
			this.parentTask = parentTask;
		}

		@Override
		public void run() {
			parentTask.run();
		}
	}

	@Override
	protected Runnable createTask() {
		return new SpawnerTask(super.createTask());
	}

	@Override
	protected void process(AbstractShopkeeper shopkeeper) {
		// Spawn the shopkeeper:
		spawner.accept(shopkeeper);
	}
}
