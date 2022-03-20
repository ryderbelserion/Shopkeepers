package com.nisovin.shopkeepers.util.java;

import java.util.concurrent.Callable;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link Callable} without return type.
 * <p>
 * Can be used to compactly specify a {@link Callable} lambda without return type like this:
 * 
 * <pre>
 * {@code
 * requiresCallable((VoidCallable) () -> doSomething());
 * }
 * </pre>
 * <p>
 * Unlike {@link Runnable#run()}, the {@link #voidCall()} method may throw an exception.
 */
@FunctionalInterface
public interface VoidCallable extends Callable<@Nullable Void> {

	/**
	 * Calls {@link #voidCall()} and returns <code>null</code>.
	 */
	@Override
	public default @Nullable Void call() throws Exception {
		this.voidCall();
		return null;
	}

	/**
	 * Performs some action and may throw an exception if it is unable to do so.
	 * 
	 * @throws Exception
	 *             if unable to perform the intended action
	 */
	public void voidCall() throws Exception;
}
