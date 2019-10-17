package com.nisovin.shopkeepers.commands.lib;

import com.nisovin.shopkeepers.util.Validate;

/**
 * An {@link ArgumentParseException} that wraps another argument parse exception and gets thrown only if there are no
 * other exceptions thrown during the command parsing.
 * <p>
 * This can for example be used if an argument couldn't be parsed, but it expects that the remaining command arguments
 * may be able to throw a more accurate exception. This exception gets only thrown if there are no other parsing
 * exceptions.
 */
public class PostponedArgumentParseException extends ArgumentParseException {

	private static final long serialVersionUID = -2965276003159498004L;

	private final ArgumentParseException originalException;

	public PostponedArgumentParseException(ArgumentParseException originalException) {
		super(Validate.notNull(originalException, "Original exception is null!").getMessage(), originalException.getCause());
		Validate.isTrue(!(originalException instanceof FallbackArgumentException), "Cannot postpone fallback exception!");
		this.originalException = originalException;
	}

	public final ArgumentParseException getOriginalException() {
		return originalException;
	}

	public final ArgumentParseException getRootException() {
		if (originalException instanceof PostponedArgumentParseException) {
			return ((PostponedArgumentParseException) originalException).getRootException();
		} else {
			return originalException;
		}
	}
}
