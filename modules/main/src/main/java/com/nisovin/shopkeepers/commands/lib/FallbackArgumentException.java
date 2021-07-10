package com.nisovin.shopkeepers.commands.lib;

import com.nisovin.shopkeepers.commands.lib.arguments.FallbackArgument;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Used by {@link FallbackArgument} to indicate that it wasn't able to parse the current argument, but that it may be
 * able to provide a fallback in case that none of the following command arguments are able to parse the remaining
 * command input either.
 */
public class FallbackArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = -2141058556443273342L;

	private final ArgumentParseException originalException; // not null

	public FallbackArgumentException(FallbackArgument<?> argument, ArgumentParseException originalException) {
		super(argument, Validate.notNull(originalException, "Original exception is null!").getMessageText(), originalException.getCause());
		this.originalException = originalException;
	}

	@Override
	public FallbackArgument<?> getArgument() {
		return (FallbackArgument<?>) super.getArgument();
	}

	/**
	 * Gets the original exception that got replaced by this {@link FallbackArgumentException}.
	 * <p>
	 * If there are multiple fallbacks recursively chained, this may itself be a {@link FallbackArgumentException}. Use
	 * {@link #getRootException()} to get the original exception at the root.
	 * 
	 * @return the original exception
	 */
	public ArgumentParseException getOriginalException() {
		return originalException;
	}

	/**
	 * Gets root parsing exception that triggered the fallback(s).
	 * <p>
	 * This follows the chain of {@link #getOriginalException() original exceptions} until it find the first one that is
	 * not a {@link FallbackArgumentException} itself.
	 */
	public ArgumentParseException getRootException() {
		if (originalException instanceof FallbackArgumentException) {
			return ((FallbackArgumentException) originalException).getRootException();
		} else {
			return originalException;
		}
	}
}
