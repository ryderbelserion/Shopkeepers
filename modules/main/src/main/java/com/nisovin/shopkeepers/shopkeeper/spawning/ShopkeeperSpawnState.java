package com.nisovin.shopkeepers.shopkeeper.spawning;

import com.nisovin.shopkeepers.component.Component;

/**
 * Internal spawn state of a shopkeeper used by {@link ShopkeeperSpawner}.
 */
public final class ShopkeeperSpawnState extends Component {

	public enum State {
		DESPAWNED,
		SPAWNED,
		QUEUED,
		PENDING_WORLD_SAVE_RESPAWN,
		SPAWNING,
		DESPAWNING
	}

	private State state = State.DESPAWNED;

	public ShopkeeperSpawnState() {
	}

	/**
	 * Gets the current spawn state.
	 * 
	 * @return the current spawn state, not <code>null</code>
	 */
	State getState() {
		return state;
	}

	/**
	 * Sets the current spawn state.
	 * 
	 * @param state
	 *            the new spawn state, not <code>null</code>
	 */
	void setState(State state) {
		assert state != null;
		this.state = state;
	}

	/**
	 * Checks if the spawning of the shopkeeper is scheduled.
	 * 
	 * @return <code>true</code> if a spawn of the shopkeeper is scheduled
	 */
	public boolean isSpawningScheduled() {
		switch (state) {
		case QUEUED:
		case PENDING_WORLD_SAVE_RESPAWN:
		case SPAWNING:
			return true;
		default:
			return false;
		}
	}
}
