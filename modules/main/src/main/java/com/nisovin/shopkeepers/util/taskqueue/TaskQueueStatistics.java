package com.nisovin.shopkeepers.util.taskqueue;

/**
 * Provides statistics on a {@link TaskQueue}.
 */
public interface TaskQueueStatistics {

	/**
	 * Gets the number of currently pending work units.
	 * 
	 * @return the number of pending work units
	 */
	public int getPendingCount();

	/**
	 * Gets the maximum number of work units that were at some point pending at the same time.
	 * 
	 * @return the maximum number of pending work units
	 */
	public int getMaxPendingCount();
}
