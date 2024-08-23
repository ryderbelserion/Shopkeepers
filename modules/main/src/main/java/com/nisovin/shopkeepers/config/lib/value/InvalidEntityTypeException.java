package com.nisovin.shopkeepers.config.lib.value;

import java.util.Collections;
import java.util.List;

import org.bukkit.entity.EntityType;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * This exception is thrown by {@link ValueType#load(Object)} when an unknown {@link EntityType} is
 * encountered.
 */
public class InvalidEntityTypeException extends ValueLoadException {

	private static final long serialVersionUID = 4248498809095698671L;
	private static final List<? extends String> EXTRA_MESSAGES = Collections.singletonList(
			"All valid entity type names can be found here: "
					+ "https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/entity/EntityType.html"
	);

	public InvalidEntityTypeException(@Nullable String message) {
		super(message, EXTRA_MESSAGES);
	}

	public InvalidEntityTypeException(@Nullable String message, @Nullable Throwable cause) {
		super(message, EXTRA_MESSAGES, cause);
	}
}
