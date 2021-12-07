package com.nisovin.shopkeepers.util.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class JsonUtils {

	private static final ThreadLocal<Gson> GSON = ThreadLocal.withInitial(() -> {
		return newGsonBuilder().create();
	});
	private static final ThreadLocal<Gson> GSON_PRETTY = ThreadLocal.withInitial(() -> {
		return newGsonBuilder().setPrettyPrinting().create();
	});

	// A GsonBuilder with the common default configuration applied:
	private static GsonBuilder newGsonBuilder() {
		return new GsonBuilder()
				.disableHtmlEscaping()
				.serializeSpecialFloatingPointValues()
				.setLenient()
				.registerTypeAdapterFactory(BukkitAwareObjectTypeAdapter.FACTORY);
	}

	// The object may be one of the primitives supported by Gson by default (primitive, Map, List, null, ..), or one of
	// our custom supported types such as ConfigurationSerializable. However, see the limitations mentioned in
	// BukkitAwareObjectTypeAdapter.
	public static String toJson(Object object) {
		return toJson(GSON.get(), object);
	}

	public static String toPrettyJson(Object object) {
		return toJson(GSON_PRETTY.get(), object);
	}

	private static String toJson(Gson gson, Object object) {
		return gson.toJson(object);
	}

	// Does not deserialize ConfigurationSerializables.
	@SuppressWarnings("unchecked")
	public static <T> T fromPlainJson(String json) throws IllegalArgumentException {
		Gson gson = GSON.get();
		try {
			// This is expected to return Gson's default Object TypeAdapter instead of our custom Bukkit-aware one.
			return (T) gson.fromJson(json, Object.class);
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not deserialize object from Json!", e);
		}
	}

	public static <T> T fromJson(String json) throws IllegalArgumentException {
		Gson gson = GSON.get();
		return BukkitAwareObjectTypeAdapter.fromJson(gson, json);
	}

	private JsonUtils() {
	}
}
