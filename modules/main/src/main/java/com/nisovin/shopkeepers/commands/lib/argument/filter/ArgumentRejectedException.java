package com.nisovin.shopkeepers.commands.lib.argument.filter;

import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.InvalidArgumentException;
import com.nisovin.shopkeepers.text.Text;

/**
 * An {@link InvalidArgumentException} that indicates that a parsed argument got rejected, for example by an
 * {@link ArgumentFilter}.
 */
public class ArgumentRejectedException extends InvalidArgumentException {

	private static final long serialVersionUID = 7271352558586958559L;

	public ArgumentRejectedException(CommandArgument<?> argument, Text message) {
		this(argument, message, null);
	}

	public ArgumentRejectedException(CommandArgument<?> argument, Text message, Throwable cause) {
		super(argument, message, cause);
	}
}
