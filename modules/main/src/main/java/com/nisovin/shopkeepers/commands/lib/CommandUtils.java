package com.nisovin.shopkeepers.commands.lib;

import java.util.Locale;

import com.nisovin.shopkeepers.util.java.Validate;

public final class CommandUtils {

	public static String normalize(String label) {
		Validate.notNull(label, "label is null");
		return label.toLowerCase(Locale.ROOT);
	}

	private CommandUtils() {
	}
}
