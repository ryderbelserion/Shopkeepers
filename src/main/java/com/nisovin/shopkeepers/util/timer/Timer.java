package com.nisovin.shopkeepers.util.timer;

import java.util.Arrays;

import com.nisovin.shopkeepers.util.MathUtils;

public class Timer implements Timings {

	private long[] timingsHistory;
	private int counter = 0;

	// Current timing:
	private boolean started = false;
	private boolean paused = false;
	private long startTime; // Nano time
	private long elapsedTime; // Nanos

	public Timer() {
		this(100);
	}

	public Timer(int historySize) {
		assert historySize > 0;
		timingsHistory = new long[historySize];
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

		// Update timings history:
		counter++;
		int historyIndex = (counter % timingsHistory.length);
		timingsHistory[historyIndex] = elapsedTime;
	}

	// TIMINGS

	@Override
	public void reset() {
		counter = 0;
		Arrays.fill(timingsHistory, 0L);
	}

	@Override
	public int getCounter() {
		return counter;
	}

	@Override
	public double getAverageTimeMillis() {
		return (MathUtils.average(timingsHistory) * 1.0E-6D);
	}

	@Override
	public double getMaxTimeMillis() {
		return (MathUtils.max(timingsHistory) * 1.0E-6D);
	}
}
