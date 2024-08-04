package com.nisovin.shopkeepers.util.bukkit;

import java.util.List;
import java.util.function.Predicate;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.checkerframework.checker.nullness.qual.NonNull;

import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.PredicateUtils;

public class RegistryUtils {

	public static <T extends Keyed> List<@NonNull T> getValues(Registry<@NonNull T> registry) {
		return registry.stream().toList();
	}

	public static <T extends Keyed> List<NamespacedKey> getKeys(Registry<@NonNull T> registry) {
		return registry.stream().map(Keyed::getKey).toList();
	}

	public static <T extends Keyed> @NonNull T cycleKeyed(
			Registry<@NonNull T> registry,
			@NonNull T current,
			boolean backwards
	) {
		return cycleKeyed(registry, current, backwards, PredicateUtils.alwaysTrue());
	}

	// Cycled through all values but none got accepted: Returns current value.
	public static <T extends Keyed> @NonNull T cycleKeyed(
			Registry<@NonNull T> registry,
			@NonNull T current,
			boolean backwards,
			Predicate<? super @NonNull T> predicate
	) {
		// TODO Cache the registry values? Or use the iterator/stream directly to get the next
		// value.
		List<@NonNull T> values = getValues(registry);
		return CollectionUtils.cycleValue(values, current, backwards, predicate);
	}

	private RegistryUtils() {
	}
}
