package com.nisovin.shopkeepers.commands.lib;

import com.nisovin.shopkeepers.commands.lib.arguments.FallbackArgument;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Used together with {@link FallbackArgument} to indicate to the processing command that the current command argument
 * wasn't able to parse the current argument, but that it may be able to provide a fallback in case none of the
 * following command arguments are able to parse it either.
 */
public class FallbackArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = -2141058556443273342L;

	private final FallbackArgument<?> argument; // not null
	private final ArgumentParseException originalException; // not null

	public FallbackArgumentException(FallbackArgument<?> argument, ArgumentParseException originalException) {
		super(Validate.notNull(originalException).getMessage(), originalException.getCause());
		Validate.notNull(argument);
		this.originalException = originalException;
		this.argument = argument;
	}

	/**
	 * Gets the {@link FallbackArgument} that may be able to provide a fallback.
	 * 
	 * @return the fallback argument
	 */
	public FallbackArgument<?> getArgument() {
		return argument;
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
	 * Gets root parsing exception that caused the fallback(s) to jump in.
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
