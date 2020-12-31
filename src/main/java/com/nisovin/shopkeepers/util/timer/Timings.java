package com.nisovin.shopkeepers.util.timer;

public interface Timings {

	public void reset();

	public int getCounter();

	public double getAverageTimeMillis();

	public double getMaxTimeMillis();
}
