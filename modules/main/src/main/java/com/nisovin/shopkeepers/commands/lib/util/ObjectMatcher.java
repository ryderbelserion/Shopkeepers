package com.nisovin.shopkeepers.commands.lib.util;

import java.util.stream.Stream;

public interface ObjectMatcher<T> {
	/**
	 * Gets the objects which match the given input
	 * <p>
	 * An example use could be to match objects identified by name.
	 * <p>
	 * The returned stream may lazily determine matching objects as they are required.
	 * 
	 * @param input
	 *            the input
	 * @return a stream over the matching objects, can be empty
	 */
	public Stream<? extends T> match(String input);
}
