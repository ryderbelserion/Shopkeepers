package com.nisovin.shopkeepers.util.java;

import java.util.concurrent.Callable;

import org.checkerframework.checker.nullness.qual.Nullable;

public final class ThrowableUtils {

	/**
	 * Gets the root cause of the given {@link Throwable}, or the given {@link Throwable} itself if
	 * it has no cause.
	 * <p>
	 * This method assumes that there are no cycles in the cause chain!
	 * 
	 * @param throwable
	 *            the throwable to get the root cause of, not <code>null</code>
	 * @return the root cause, not <code>null</code>
	 */
	public static Throwable getRootCause(Throwable throwable) {
		Validate.notNull(throwable, "throwable is null");
		Throwable current = throwable;
		Throwable cause;
		while ((cause = current.getCause()) != null) {
			current = cause;
		}
		return current;
	}

	/**
	 * Creates a short description of this {@link Throwable}.
	 * <p>
	 * This recursively searches the {@link Throwable} and its chain of causes for a detail message
	 * that is neither <code>null</code> nor empty. If no such message is found, the Throwable's
	 * class name is returned instead.
	 * 
	 * @param throwable
	 *            the throwable
	 * @return a description of this throwable, not <code>null</code> or empty
	 */
	public static String getDescription(Throwable throwable) {
		String message = getMessageRecursively(throwable);
		if (message != null) {
			return message;
		} else {
			return throwable.getClass().getName();
		}
	}

	/**
	 * Recursively searches this {@link Throwable} and its chain of causes for a detail message that
	 * is neither <code>null</code> nor empty.
	 * 
	 * @param throwable
	 *            the throwable
	 * @return the first found non-empty detail message, or <code>null</code> if none is found
	 */
	public static @Nullable String getMessageRecursively(Throwable throwable) {
		String message = throwable.getMessage();
		if (!StringUtils.isEmpty(message)) {
			return message;
		}

		Throwable cause = throwable.getCause();
		if (cause != null) {
			return getMessageRecursively(cause);
		} else {
			return null;
		}
	}

	/**
	 * Throws the given, possibly checked exception, as an unchecked one.
	 * <p>
	 * Even though this method never returns any value, it declares {@link Error} as its return
	 * type. This allows clients to inform the compiler that the code path following the invocation
	 * of this method cannot be reached by calling it like this:
	 * 
	 * <pre>
	 * {@code
	 * throw ThrowableUtils.rethrow(throwable);
	 * }
	 * </pre>
	 * 
	 * @param <T>
	 *            the type of the unchecked throwable
	 * @param throwable
	 *            the throwable
	 * @return this method never returns any value
	 * @throws T
	 *             the given throwable in an unchecked way
	 */
	// https://stackoverflow.com/questions/4554230/rethrowing-checked-exceptions/4555351#4555351
	@SuppressWarnings("unchecked")
	public static <T extends Throwable> Error rethrow(Throwable throwable) throws T {
		// This cast is erased at runtime and therefore never actually performed / checked:
		throw (T) throwable;
	}

	/**
	 * Calls the given {@link Callable} and rethrows any thrown exception as an unchecked one.
	 * 
	 * @param <T>
	 *            the return type
	 * @param callable
	 *            the callable
	 * @return the callable's result
	 */
	public static <T> T callUnchecked(Callable<? extends T> callable) {
		try {
			return callable.call();
		} catch (Throwable e) {
			throw rethrow(e);
		}
	}

	private ThrowableUtils() {
	}
}
