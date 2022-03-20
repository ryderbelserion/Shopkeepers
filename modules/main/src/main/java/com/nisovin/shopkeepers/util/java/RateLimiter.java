package com.nisovin.shopkeepers.util.java;

/**
 * A time and task agnostic rate limiter that can be used to limit the rate at which a certain
 * operation is performed.
 * <p>
 * This {@link RateLimiter} consists of a {@link #getThreshold() threshold} and a
 * {@link #getRemainingThreshold() remaining threshold}. Each invocation of {@link #request()} or
 * {@link #request(int)} represents one or more execution requests and adjusts the
 * {@link #getRemainingThreshold() remaining threshold} accordingly. Once the
 * {@link #getRemainingThreshold() remaining threshold} reaches {@code zero}, the execution is
 * permitted and the {@code remaining threshold} is reset to the {@code threshold}.
 */
public class RateLimiter {

	/**
	 * Creates a new {@link RateLimiter} with the specified threshold and a random initial threshold
	 * between {@code 1} and the {@code threshold} (inclusive).
	 * 
	 * @param threshold
	 *            the threshold
	 * @return the rate limiter
	 */
	public static RateLimiter withRandomInitialThreshold(int threshold) {
		return new RateLimiter(threshold, MathUtils.randomIntInRange(0, threshold) + 1);
	}

	/**
	 * Creates a new {@link RateLimiter} with the specified threshold and an initial threshold that
	 * is calculated by adding the given {@code offset} to the {@code threshold}.
	 * 
	 * @param threshold
	 *            the threshold
	 * @param offset
	 *            the offset
	 * @return the rate limiter
	 */
	public static RateLimiter withInitialOffset(int threshold, int offset) {
		return new RateLimiter(threshold, threshold + offset);
	}

	/**
	 * Creates a new {@link RateLimiter} with the specified threshold and an initial threshold that
	 * is calculated by adding a random offset between {@code 0} and the {@code threshold}
	 * (exclusive) to the {@code threshold}.
	 * 
	 * @param threshold
	 *            the threshold
	 * @return the rate limiter
	 */
	public static RateLimiter withRandomInitialOffset(int threshold) {
		return new RateLimiter(threshold, threshold + MathUtils.randomIntInRange(0, threshold));
	}

	private int threshold;
	private int remainingThreshold;

	/**
	 * Creates a new {@link RateLimiter} with the specified threshold and an initial threshold of
	 * {@code 1} (the first requested execution is immediately permitted).
	 * 
	 * @param threshold
	 *            the threshold, has to be positive
	 */
	public RateLimiter(int threshold) {
		this(threshold, 1);
	}

	/**
	 * Creates a new {@link RateLimiter} with the specified threshold and initial threshold.
	 * 
	 * @param threshold
	 *            the threshold, has to be positive
	 * @param initialThreshold
	 *            the initial threshold, has to be positive
	 */
	public RateLimiter(int threshold, int initialThreshold) {
		Validate.isTrue(threshold >= 1, "threshold has to be positive");
		Validate.isTrue(initialThreshold >= 1, "initialThreshold has to be positive");
		this.threshold = threshold;
		this.remainingThreshold = initialThreshold;
	}

	/**
	 * Gets the threshold.
	 * <p>
	 * Whenever the {@link #getRemainingThreshold() remaining threshold} reaches {@code zero}, it is
	 * reset to this value.
	 * 
	 * @return the threshold
	 */
	public final int getThreshold() {
		return threshold;
	}

	/**
	 * Sets the {@link #getThreshold() threshold}.
	 * 
	 * @param threshold
	 *            the new threshold, has to be positive
	 */
	public void setThreshold(int threshold) {
		Validate.isTrue(threshold >= 1, "threshold has to be positive");
		this.threshold = threshold;
	}

	/**
	 * Gets the remaining threshold.
	 * <p>
	 * This threshold is decreased by invocations to {@link #request()} and {@link #request(int)}.
	 * Once it reaches {@code zero}, the last requested execution is permitted and the
	 * {@link #getRemainingThreshold() remaining threshold} is reset to the {@link #getThreshold()
	 * threshold}.
	 * <p>
	 * This value may be greater than the {@link #getThreshold() threshold}, for example if the
	 * {@code initialThreshold} specified during the construction of this {@link RateLimiter}, or
	 * the remaining threshold specified via {@link #setRemainingThreshold(int)} is greater than the
	 * {@link #getThreshold() threshold}.
	 * 
	 * @return the remaining threshold
	 */
	public final int getRemainingThreshold() {
		return remainingThreshold;
	}

	/**
	 * Sets the {@link #getRemainingThreshold() remaining threshold}.
	 * 
	 * @param remainingThreshold
	 *            the new remaining threshold, has to be positive
	 */
	public void setRemainingThreshold(int remainingThreshold) {
		Validate.isTrue(remainingThreshold >= 1, "remainingThreshold has to be positive");
		this.remainingThreshold = remainingThreshold;
	}

	/**
	 * Makes one execution request.
	 * <p>
	 * This decreases the {@link #getRemainingThreshold() remaining threshold} by {@code one}. Once
	 * the {@link #getRemainingThreshold() remaining threshold} reaches {@code zero}, it is reset to
	 * the {@link #getThreshold() threshold} and the execution is permitted.
	 * 
	 * @return <code>true</code> if the execution is permitted, <code>false</code> otherwise
	 */
	public boolean request() {
		return this.handleRequest(1);
	}

	/**
	 * Makes a certain number of execution requests.
	 * <p>
	 * This decreases the {@link #getRemainingThreshold() remaining threshold} by the given number
	 * of requests. Once the {@link #getRemainingThreshold() remaining threshold} reaches
	 * {@code zero}, it is reset to the {@link #getThreshold() threshold} and the execution is
	 * permitted.
	 * <p>
	 * If the number of requests exceeds the {@link #getRemainingThreshold() remaining threshold},
	 * any excess requests are ignored and not taken into account towards future requests. I.e. the
	 * {@link #getRemainingThreshold() remaining threshold} is reset to the {@link #getThreshold()
	 * threshold}, regardless of the number of excess requests.
	 * 
	 * @param requests
	 *            the number of requests, has to be positive
	 * @return <code>true</code> if the execution is permitted, <code>false</code> otherwise
	 */
	public boolean request(int requests) {
		Validate.isTrue(requests > 0, "requests has to be positive");
		return this.handleRequest(requests);
	}

	private boolean handleRequest(int requests) {
		assert requests > 0;
		remainingThreshold -= requests;
		if (remainingThreshold <= 0) {
			// The threshold has been reached:
			// Reset the threshold, ignoring any excess requests.
			remainingThreshold = threshold;
			return true;
		} else {
			// The threshold has not been reached yet:
			return false;
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RateLimiter [threshold=");
		builder.append(threshold);
		builder.append(", remainingThreshold=");
		builder.append(remainingThreshold);
		builder.append("]");
		return builder.toString();
	}
}
