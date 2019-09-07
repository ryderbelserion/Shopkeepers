package com.nisovin.shopkeepers.util;

public interface ObjectMatcher<T> {
	/**
	 * Gets an object which matches the given input.
	 * <p>
	 * An example use could be to match objects identified by name.
	 * 
	 * @param input
	 *            the input
	 * @return the matching object, or <code>null</code>
	 */
	public T match(String input);
}
