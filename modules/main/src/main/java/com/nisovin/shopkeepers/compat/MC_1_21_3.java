package com.nisovin.shopkeepers.compat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.nisovin.shopkeepers.util.java.ClassUtils;
import com.nisovin.shopkeepers.util.logging.Log;

// TODO This can be removed once we only support Bukkit 1.21.3 upwards.
public final class MC_1_21_3 {

	private static Optional<Boolean> IS_AVAILABLE = Optional.empty();

	public static final List<String> SALMON_VARIANTS = Collections.unmodifiableList(Arrays.asList(
			"SMALL", "MEDIUM", "LARGE"
	));

	public static void init() {
		if (isAvailable()) {
			Log.debug("MC 1.21.3 exclusive features are enabled.");
		} else {
			Log.debug("MC 1.21.3 exclusive features are disabled.");
		}
	}

	public static boolean isAvailable() {
		if (!IS_AVAILABLE.isPresent()) {
			boolean isAvailable = ClassUtils.getClassOrNull("org.bukkit.entity.Salmon$Variant") != null;
			IS_AVAILABLE = Optional.of(isAvailable);
		}
		return IS_AVAILABLE.get();
	}

	private MC_1_21_3() {
	}
}
