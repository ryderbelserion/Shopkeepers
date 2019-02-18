package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.function.Predicate;

import com.nisovin.shopkeepers.commands.lib.CommandArgument;

public interface ArgumentFilter<T> extends Predicate<T> {

	/**
	 * Gets an 'invalid argument' error message for the given parsed but declined value.
	 * 
	 * @param argument
	 *            the argument using this filter
	 * @param input
	 *            the argument input
	 * @param value
	 *            the corresponding parsed but declined value
	 * @return the error message
	 */
	public default String getInvalidArgumentErrorMsg(CommandArgument argument, String input, T value) {
		return argument.getInvalidArgumentErrorMsg(input);
	}
}
