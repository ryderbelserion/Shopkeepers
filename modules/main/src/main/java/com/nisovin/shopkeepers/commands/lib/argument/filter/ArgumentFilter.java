package com.nisovin.shopkeepers.commands.lib.argument.filter;

import java.util.function.Predicate;

import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.text.Text;

public interface ArgumentFilter<T> extends Predicate<T> {

	public static final ArgumentFilter<Object> ACCEPT_ANY = (object) -> true;

	@SuppressWarnings("unchecked")
	public static <T> ArgumentFilter<T> acceptAny() {
		return (ArgumentFilter<T>) ACCEPT_ANY;
	}

	/**
	 * Gets the 'invalid argument' error message for the given parsed but declined value.
	 * <p>
	 * When overriding this method, consider using {@link CommandArgument#getDefaultErrorMsgArgs()} for the common
	 * message arguments.
	 * <p>
	 * Consider using an {@link ArgumentRejectedException} when using this for an exception.
	 * 
	 * @param argument
	 *            the argument using this filter
	 * @param argumentInput
	 *            the argument input
	 * @param value
	 *            the corresponding parsed but declined value
	 * @return the error message
	 */
	public default Text getInvalidArgumentErrorMsg(CommandArgument<T> argument, String argumentInput, T value) {
		return argument.getInvalidArgumentErrorMsg(argumentInput);
	}
}
