package com.nisovin.shopkeepers.commands.lib;

import com.nisovin.shopkeepers.text.Text;

/**
 * This exception is thrown when a command cannot be executed because of missing permissions.
 */
public class NoPermissionException extends CommandException {

	private static final long serialVersionUID = -4918361876059852723L;

	public NoPermissionException(Text message) {
		super(message);
	}

	public NoPermissionException(Text message, Throwable cause) {
		super(message, cause);
	}
}
