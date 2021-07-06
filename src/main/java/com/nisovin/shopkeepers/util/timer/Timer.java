package com.nisovin.shopkeepers.util.timer;

import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.java.TimeUtils;

public class Timer implements Timings {

	private long counter = 0L;
	private long totalTimeNanos = 0L;
	private long maxTimeNanos = 0L;

	// Current timing:
	private boolean started = false;
	private boolean paused = false;
	private long startTimeNanos;
	private long elapsedTimeNanos;

	public Timer() {
	}

	public void start() {
		assert !started && !paused;
		// Reset:
		started = true;
		paused = false;
		elapsedTimeNanos = 0L;
		// Start timing:
		startTimeNanos = System.nanoTime();
	}

	public void startPaused() {
		this.start();
		this.pause();
	}

	public void pause() {
		assert started && !paused;
		paused = true;
		// Update timing:
		elapsedTimeNanos += (System.nanoTime() - startTimeNanos);
	}

	public void resume() {
		assert started && paused;
		paused = false;
		// Continue timing:
		startTimeNanos = System.nanoTime();
	}

	public void stop() {
		assert started;
		if (!paused) {
			// Update timing by pausing:
			this.pause();
		}
		assert paused;

		// Update timings:
		counter++;
		totalTimeNanos += elapsedTimeNanos;

		// Update max timing:
		if (elapsedTimeNanos > maxTimeNanos) {
			maxTimeNanos = elapsedTimeNanos;
		}
	}

	// TIMINGS

	@Override
	public void reset() {
		counter = 0L;
		totalTimeNanos = 0L;
		maxTimeNanos = 0L;
	}

	@Override
	public long getCounter() {
		return counter;
	}

	@Override
	public double getAverageTimeMillis() {
		double avgTimeNanos = (double) totalTimeNanos / (counter == 0L ? 1L : counter);
		return TimeUtils.convert(avgTimeNanos, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
	}

	@Override
	public double getMaxTimeMillis() {
		return TimeUtils.convert(maxTimeNanos, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
	}
}
