package com.nisovin.shopkeepers.util.timer;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import com.nisovin.shopkeepers.util.MathUtils;
import com.nisovin.shopkeepers.util.TimeUtils;

public class Timer implements Timings {

	private static final long UNSET = -1L;

	private long[] timingsHistory;
	private long counter = 0L;

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
		this.reset();
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
		int historyIndex = (int) (counter % timingsHistory.length);
		timingsHistory[historyIndex] = elapsedTime;
	}

	// TIMINGS

	@Override
	public void reset() {
		counter = 0L;
		Arrays.fill(timingsHistory, UNSET);
	}

	@Override
	public long getCounter() {
		return counter;
	}

	@Override
	public double getAverageTimeMillis() {
		double avgTimeNanos = MathUtils.average(timingsHistory, UNSET);
		return TimeUtils.convert(avgTimeNanos, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
	}

	@Override
	public double getMaxTimeMillis() {
		long maxTimeNanos = MathUtils.max(timingsHistory, UNSET);
		return TimeUtils.convert(maxTimeNanos, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
	}
}
