package com.nisovin.shopkeepers.util.timer;

public interface Timings {

	public void reset();

	public long getCounter();

	public double getAverageTimeMillis();

	public double getMaxTimeMillis();
}
