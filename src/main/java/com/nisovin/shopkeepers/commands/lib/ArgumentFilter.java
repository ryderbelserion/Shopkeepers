package com.nisovin.shopkeepers.commands.lib;

import java.util.function.Predicate;

public interface ArgumentFilter<T> extends Predicate<T> {

	public static final ArgumentFilter<Object> ACCEPT_ANY = new ArgumentFilter<Object>() {
		@Override
		public boolean test(Object object) {
			return true;
		}
	};

	@SuppressWarnings("unchecked")
	public static <T> ArgumentFilter<T> acceptAny() {
		return (ArgumentFilter<T>) ACCEPT_ANY;
	}

	/**
	 * Gets an 'invalid argument' error message for the given parsed but declined value.
	 * <p>
	 * Consider using an {@link ArgumentRejectedException} when using this for an exception.
	 * 
	 * @param argument
	 *            the argument using this filter
	 * @param input
	 *            the argument input
	 * @param value
	 *            the corresponding parsed but declined value
	 * @return the error message
	 */
	public default String getInvalidArgumentErrorMsg(CommandArgument<T> argument, String input, T value) {
		return argument.getInvalidArgumentErrorMsg(input);
	}
}
