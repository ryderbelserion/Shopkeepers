package com.nisovin.shopkeepers.util.bukkit;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.checkerframework.checker.nullness.qual.NonNull;

public class RegistryUtils {

	public static <T extends @NonNull Keyed> List<@NonNull T> getValues(Registry<@NonNull T> registry) {
		List<@NonNull T> list = new ArrayList<>();
		registry.forEach(list::add);
		return list;
	}

	public static <T extends @NonNull Keyed> List<@NonNull NamespacedKey> getKeys(Registry<@NonNull T> registry) {
		List<@NonNull NamespacedKey> list = new ArrayList<>();
		registry.forEach(value -> list.add(value.getKey()));
		return list;
	}

	private RegistryUtils() {
	}
}
