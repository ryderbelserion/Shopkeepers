package com.nisovin.shopkeepers.commands.lib;

import java.util.Locale;

public class CommandUtils {

	private CommandUtils() {
	}

	public static String normalize(String label) {
		assert label != null;
		return label.toLowerCase(Locale.ROOT);
	}
}
