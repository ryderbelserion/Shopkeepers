package com.nisovin.shopkeepers.config.lib.value;

import java.util.Collections;
import java.util.List;

import org.bukkit.Material;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This exception is thrown by {@link ValueType#load(Object)} when an unknown {@link Material} is
 * encountered, as well as for unsupported {@link Material#isLegacy() legacy} Materials.
 */
public class InvalidMaterialException extends ValueLoadException {

	private static final long serialVersionUID = 1653518607452366268L;
	private static final List<? extends String> EXTRA_MESSAGES = Collections.singletonList(
			"All valid material names can be found here: "
					+ "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html"
	);

	public InvalidMaterialException(@Nullable String message) {
		super(message, EXTRA_MESSAGES);
	}

	public InvalidMaterialException(@Nullable String message, @Nullable Throwable cause) {
		super(message, EXTRA_MESSAGES, cause);
	}
}
