package com.nisovin.shopkeepers.util.data.serialization.bukkit;

import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link Keyed} values.
 */
public final class KeyedSerializers {

	/**
	 * Resolves {@link Keyed} instances of a specific type by {@link NamespacedKey}.
	 *
	 * @param <E>
	 *            the {@link Keyed} type that is being resolved
	 */
	public interface KeyedResolver<E extends Keyed> {

		/**
		 * Resolves the an object by its namespaced key.
		 * 
		 * @param key
		 *            the namespaced key, not <code>null</code>
		 * @return the resolved object, or <code>null</code> if it cannot be resolved
		 */
		public @Nullable E resolve(NamespacedKey key);
	}

	private static class KeyedSerializer<E extends Keyed> implements DataSerializer<@NonNull E> {

		private final Class<@NonNull E> keyedType;
		private final KeyedResolver<@NonNull E> resolver;

		private KeyedSerializer(Class<@NonNull E> keyedType, KeyedResolver<@NonNull E> resolver) {
			Validate.notNull(keyedType, "keyedType is null");
			Validate.notNull(resolver, "resolver is null");
			this.keyedType = keyedType;
			this.resolver = resolver;
		}

		@Override
		public @Nullable Object serialize(@NonNull E value) {
			Validate.notNull(value, "value is null");
			return value.getKey().toString();
		}

		@Override
		public @NonNull E deserialize(Object data) throws InvalidDataException {
			NamespacedKey key = NamespacedKeySerializers.DEFAULT.deserialize(data);
			@Nullable E value = resolver.resolve(key);
			if (value == null) {
				throw new InvalidDataException("Unknown " + keyedType.getSimpleName() + ": "
						+ key.toString());
			}

			return value;
		}
	}

	/**
	 * Gets a {@link DataSerializer} for values of the specified {@link Keyed} type that uses the
	 * given {@code resolver} to lookup the objects by their {@link NamespacedKey} during
	 * deserialization.
	 * 
	 * @param <E>
	 *            the {@link Keyed} type
	 * @param keyedType
	 *            the class of the serialized {@link Keyed} type that is being de-/serialized, not
	 *            <code>null</code>
	 * @param resolver
	 *            the {@link KeyedResolver} to use during deserialization
	 * @return the data serializer, not <code>null</code>
	 */
	public static <E extends Keyed> DataSerializer<@NonNull E> forResolver(
			Class<@NonNull E> keyedType,
			KeyedResolver<@NonNull E> resolver
	) {
		return new KeyedSerializer<>(keyedType, resolver);
	}

	/**
	 * Gets a {@link DataSerializer} for values of the specified {@link Keyed} type that looks up
	 * the objects from the given {@link Registry} during deserialization.
	 * 
	 * @param <E>
	 *            the {@link Keyed} type
	 * @param keyedType
	 *            the class of the serialized {@link Keyed} type that is being de-/serialized, not
	 *            <code>null</code>
	 * @param registry
	 *            the {@link Registry} to use during deserialization
	 * @return the data serializer, not <code>null</code>
	 */
	public static <E extends Keyed> DataSerializer<@NonNull E> forRegistry(
			Class<@NonNull E> keyedType,
			Registry<@NonNull E> registry
	) {
		Validate.notNull(registry, "registry is null");
		return forResolver(keyedType, (key) -> registry.get(key));
	}

	private KeyedSerializers() {
	}
}
