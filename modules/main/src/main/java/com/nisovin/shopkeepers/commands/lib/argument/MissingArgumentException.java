package com.nisovin.shopkeepers.commands.lib.argument;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.text.Text;

/**
 * An {@link ArgumentParseException} that indicates that parsing failed due to there being no
 * argument to parse.
 */
public class MissingArgumentException extends ArgumentParseException {

	private static final long serialVersionUID = -3269722516077651284L;

	public MissingArgumentException(CommandArgument<?> argument, Text message) {
		this(argument, message, null);
	}

	public MissingArgumentException(
			CommandArgument<?> argument,
			Text message,
			@Nullable Throwable cause
	) {
		super(argument, message, cause);
	}
}
