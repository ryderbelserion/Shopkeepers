package com.nisovin.shopkeepers.util.timer;

/**
 * Monitors a specific task and provides information about its processing time and the number of
 * times it was executed.
 */
public interface Timings {

	/**
	 * Resets the timings.
	 */
	public void reset();

	/**
	 * Gets the number of times the task was executed.
	 * 
	 * @return the number of times the task was executed
	 */
	public long getCounter();

	/**
	 * Gets the task's average processing time in milliseconds.
	 * 
	 * @return the average processing time in milliseconds
	 */
	public double getAverageTimeMillis();

	/**
	 * Gets the task's maximum processing time in milliseconds.
	 * 
	 * @return the maximum processing time in milliseconds
	 */
	public double getMaxTimeMillis();
}
