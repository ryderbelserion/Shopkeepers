package com.nisovin.shopkeepers.util.java;

import java.util.concurrent.Callable;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;

public final class Retry {

	public interface Callback {
		/**
		 * Gets invoked after every failed execution attempt of
		 * {@link Retry#retry(Callable, int, Retry.Callback)}.
		 * <p>
		 * This can be used to process the failed attempt (e.g. log it) and to prepare a subsequent
		 * reattempt.
		 * 
		 * @param attemptNumber
		 *            the number of the last failed attempt, starting at <code>1</code> for the
		 *            first failed attempt
		 * @param exception
		 *            the exception thrown during of the last failed attempt
		 * @param retry
		 *            <code>true</code> if there will be a subsequent retry, <code>false</code> if
		 *            this was the final attempt
		 * @throws Exception
		 *             aborts the retrying with this exception
		 */
		void onFailure(int attemptNumber, Exception exception, boolean retry) throws Exception;
	}

	/**
	 * Runs the given {@link Callable} and returns its return value.
	 * <p>
	 * If the callable throws an {@link Exception} (checked or unchecked), the exception is silently
	 * ignored and the callable is executed another time, up until the specified limit of attempts
	 * is reached. Any other type of {@link Throwable} is forwarded to the caller and does not
	 * trigger another reattempt.
	 * <p>
	 * In case of success, the return value of the callable is returned. In case of failure, the
	 * exception thrown by the callable during the last failed attempt is forwarded.
	 * 
	 * @param <T>
	 *            the callable's return type
	 * @param callable
	 *            the callable to execute
	 * @param maxAttempts
	 *            the maximum number of times the callable is called
	 * @return the return value of the callable in case of successful execution
	 * @throws Exception
	 *             forwarded exception thrown by the callable during the last failed attempt
	 * @see #retry(Callable, int, Retry.Callback)
	 */
	public static <T> T retry(Callable<T> callable, int maxAttempts) throws Exception {
		return retry(callable, maxAttempts, null);
	}

	/**
	 * Runs the given {@link Callable} and returns its return value.
	 * <p>
	 * If the callable throws an {@link Exception} (checked or unchecked), the exception is silently
	 * ignored and the callable is executed another time, up until the specified limit of attempts
	 * is reached. Any other type of {@link Throwable} is forwarded to the caller and does not
	 * trigger another reattempt.
	 * <p>
	 * In case of success, the return value of the callable is returned. In case of failure, the
	 * exception thrown by the callable during the last failed attempt is forwarded.
	 * <p>
	 * Optionally, a {@link Retry.Callback} can be provided which gets run after every failed
	 * execution attempt. It provides the number of the last failed attempt together with the thrown
	 * exception. This can be used to process failed attempts and to perform preparation for any
	 * subsequent reattempt. Any exception thrown by the {@link Retry.Callback} itself will abort
	 * the retrying with that exception.
	 * 
	 * @param <T>
	 *            the callable's return type
	 * @param callable
	 *            the callable to execute
	 * @param maxAttempts
	 *            the maximum number of times the callable is called
	 * @param retryCallback
	 *            the callback to invoke on every failed attempt, or <code>null</code>
	 * @return the return value of the callable in case of successful execution
	 * @throws Exception
	 *             forwarded exception thrown by the callable or the retry callback during the last
	 *             failed attempt
	 */
	public static <T> T retry(
			Callable<T> callable,
			int maxAttempts,
			@Nullable Callback retryCallback
	) throws Exception {
		Validate.isTrue(maxAttempts > 0, "maxAttempts has to be positive");
		int currentAttempt = 0;
		Exception lastException = null;
		while (++currentAttempt <= maxAttempts) {
			try {
				return callable.call();
			} catch (Exception e) {
				// Note: Any other type of Throwable is directly forwarded to the caller.
				lastException = e;

				// Inform the retry callback:
				if (retryCallback != null) {
					try {
						retryCallback.onFailure(
								currentAttempt,
								lastException,
								currentAttempt < maxAttempts
						);
					} catch (Exception e2) {
						// Abort retrying with this exception:
						lastException = e2;
						break;
					}
				}
				// Continue with the next attempt if the limit is not yet reached.
			}
		}
		throw Unsafe.assertNonNull(lastException);
	}

	private Retry() {
	}
}
