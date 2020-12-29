package com.nisovin.shopkeepers.util;

import java.util.concurrent.Callable;

/**
 * A {@link Callable} without return type.
 * <p>
 * Can be used to compactly specify a {@link Callable} lambda without return type like this:
 * 
 * <pre>
 * {@code
 * methodRequiringCallable((VoidCallable) () -> doSomething());
 * }
 * </pre>
 */
@FunctionalInterface
public interface VoidCallable extends Callable<Void> {

	/**
	 * Calls {@link #voidCall()} and returns <code>null</code>.
	 */
	@Override
	public default Void call() throws Exception {
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
