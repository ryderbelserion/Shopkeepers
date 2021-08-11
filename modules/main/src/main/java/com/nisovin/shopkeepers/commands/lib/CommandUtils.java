package com.nisovin.shopkeepers.commands.lib;

import java.util.Locale;

import com.nisovin.shopkeepers.util.java.Validate;

public class CommandUtils {

	private CommandUtils() {
	}

	public static String normalize(String label) {
		Validate.notNull(label, "label");
		return label.toLowerCase(Locale.ROOT);
	}
}
