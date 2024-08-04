package com.nisovin.shopkeepers.util.data.serialization;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.container.DataValueContainer;
import com.nisovin.shopkeepers.util.data.property.Property;

/**
 * Converts a certain type of non-<code>null</code> value to and from a serializable representation
 * as described by {@link DataContainer}.
 * <p>
 * During {@link #deserialize(Object) deserialization}, {@link DataSerializer}s should only validate
 * the given data to check if they can reconstruct the original value of the target type. Any
 * validation of the deserialized value is better handled by higher level components, such as
 * {@link Property properties}.
 * <p>
 * Similarly, {@link #deserialize(Object)} shall not throw any {@link MissingDataException}s. The
 * decision whether a deserialized value should be regarded as empty and therefore treated as if it
 * is missing is also better made by higher level components.
 * 
 * @param <T>
 *            the type of values that are being serialized
 */
public interface DataSerializer<@NonNull T> {

	/**
	 * Converts the given value to a representation that can be serialized as described by
	 * {@link DataContainer}.
	 * <p>
	 * Usually, with the limitations mentioned below and in {@link DataContainer}, the value can be
	 * reconstructed from the serialized representation via {@link #deserialize(Object)}.
	 * <p>
	 * In some cases the given value might not be serializable, or its serialization might not be
	 * reconstructible via {@link #deserialize(Object)}: For example, {@link DataValueContainer}s
	 * have states in which their {@link DataContainer#serialize() serialization} can return
	 * arbitrary values, including <code>null</code>.
	 * 
	 * @param value
	 *            the value, not <code>null</code>
	 * @return the serialized representation, can be <code>null</code> in some cases
	 */
	public @Nullable Object serialize(T value);

	/**
	 * Reconstructs the value from the given serialized representation.
	 * <p>
	 * This is expected to not throw any {@link MissingDataException}.
	 * 
	 * @param data
	 *            the serialized representation, not <code>null</code>
	 * @return the reconstructed value, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the value can not be reconstructed
	 */
	public @NonNull T deserialize(Object data) throws InvalidDataException;
}
