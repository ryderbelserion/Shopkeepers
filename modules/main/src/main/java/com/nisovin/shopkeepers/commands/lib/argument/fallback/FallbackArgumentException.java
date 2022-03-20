package com.nisovin.shopkeepers.commands.lib.argument.fallback;

import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Used by {@link FallbackArgument} to indicate that it wasn't able to parse the current argument,
 * but that it may be able to provide a fallback in case that none of the following command
 * arguments are able to parse the remaining command input either.
 */
public class FallbackArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = -2141058556443273342L;

	private final ArgumentParseException originalException; // Not null

	public FallbackArgumentException(
			FallbackArgument<?> argument,
			ArgumentParseException originalException
	) {
		super(
				Validate.notNull(argument, "argument is null"),
				Validate.notNull(originalException, "originalException is null").getMessageText(),
				originalException.getCause()
		);
		this.originalException = originalException;
	}

	@Override
	public @NonNull FallbackArgument<?> getArgument() {
		return Unsafe.castNonNull(super.getArgument());
	}

	/**
	 * Gets the original exception that got replaced by this {@link FallbackArgumentException}.
	 * <p>
	 * If there are multiple fallbacks recursively chained, this may itself be a
	 * {@link FallbackArgumentException}. Use {@link #getRootException()} to get the original
	 * exception at the root.
	 * 
	 * @return the original exception
	 */
	public ArgumentParseException getOriginalException() {
		return originalException;
	}

	/**
	 * Gets root parsing exception that triggered the fallback(s).
	 * <p>
	 * This follows the chain of {@link #getOriginalException() original exceptions} until it find
	 * the first one that is not a {@link FallbackArgumentException} itself.
	 * 
	 * @return the root exception
	 */
	public ArgumentParseException getRootException() {
		if (originalException instanceof FallbackArgumentException) {
			return ((FallbackArgumentException) originalException).getRootException();
		} else {
			return originalException;
		}
	}
}
