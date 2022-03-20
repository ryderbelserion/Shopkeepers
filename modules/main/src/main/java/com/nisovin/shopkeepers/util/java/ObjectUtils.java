package com.nisovin.shopkeepers.util.java;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

public final class ObjectUtils {

	@SuppressWarnings("unchecked")
	public static <T> @Nullable T castOrNull(
			@Nullable Object object,
			Class<? extends @NonNull T> clazz
	) {
		if (clazz.isInstance(object)) {
			return (T) object;
		} else {
			return null;
		}
	}

	private ObjectUtils() {
	}
}
