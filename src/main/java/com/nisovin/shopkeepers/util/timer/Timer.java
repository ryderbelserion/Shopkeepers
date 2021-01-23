package com.nisovin.shopkeepers.util.timer;

import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.TimeUtils;

public class Timer implements Timings {

	private long counter = 0L;
	private long totalTime = 0L; // In nano seconds
	private long maxTime = 0L; // In nano seconds

	// Current timing:
	private boolean started = false;
	private boolean paused = false;
	private long startTime; // Nano time
	private long elapsedTime; // In nano seconds

	public Timer() {
	}

	public void start() {
		assert !started && !paused;
		// Reset:
		started = true;
		paused = false;
		elapsedTime = 0L;
		// Start timing:
		startTime = System.nanoTime();
	}

	public void startPaused() {
		this.start();
		this.pause();
	}

	public void pause() {
		assert started && !paused;
		paused = true;
		// Update timing:
		elapsedTime += (System.nanoTime() - startTime);
	}

	public void resume() {
		assert started && paused;
		paused = false;
		// Continue timing:
		startTime = System.nanoTime();
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
		totalTime += elapsedTime;

		// Update max timing:
		if (elapsedTime > maxTime) {
			maxTime = elapsedTime;
		}
	}

	// TIMINGS

	@Override
	public void reset() {
		counter = 0L;
		totalTime = 0L;
		maxTime = 0L;
	}

	@Override
	public long getCounter() {
		return counter;
	}

	@Override
	public double getAverageTimeMillis() {
		double avgTimeNanos = (double) totalTime / (counter == 0L ? 1L : counter);
		return TimeUtils.convert(avgTimeNanos, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
	}

	@Override
	public double getMaxTimeMillis() {
		return TimeUtils.convert(maxTime, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
	}
}
