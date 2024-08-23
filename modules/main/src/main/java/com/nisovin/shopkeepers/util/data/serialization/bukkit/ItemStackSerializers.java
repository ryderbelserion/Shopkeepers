package com.nisovin.shopkeepers.util.data.serialization.bukkit;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.bukkit.DataUtils;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Default {@link DataSerializer}s for {@link ItemStack} values.
 */
public final class ItemStackSerializers {

	/**
	 * A {@link DataSerializer} for {@link ItemStack} values.
	 */
	public static final DataSerializer<ItemStack> DEFAULT = new DataSerializer<ItemStack>() {
		@Override
		public @Nullable Object serialize(ItemStack value) {
			Validate.notNull(value, "value is null");
			return value;
		}

		@Override
		public ItemStack deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			if (data instanceof ItemStack) {
				return DataUtils.deserializeNonNullItemStack((ItemStack) data);
			} else if (data instanceof UnmodifiableItemStack) {
				// We also support UnmodifiableItemStacks here, but return a copy of the item stack,
				// because we don't know how the returned ItemStack will be used by the caller, i.e.
				// whether it is expected to be modifiable.
				// Note: We don't expect the additional ItemStack processing of
				// DataUtils.deserializeItemStack to be required here, because the item stack is not
				// freshly deserialized.
				return ((UnmodifiableItemStack) data).copy();
			} else {
				throw new InvalidDataException("Data is not of type ItemStack, but "
						+ data.getClass().getName() + "!");
			}
		}
	};

	/**
	 * A {@link DataSerializer} for {@link UnmodifiableItemStack} values.
	 */
	public static final DataSerializer<UnmodifiableItemStack> UNMODIFIABLE = new DataSerializer<UnmodifiableItemStack>() {
		@Override
		public @Nullable Object serialize(UnmodifiableItemStack value) {
			Validate.notNull(value, "value is null");
			return DataUtils.serializeItemStack(value);
		}

		@Override
		public UnmodifiableItemStack deserialize(Object data) throws InvalidDataException {
			if (data instanceof UnmodifiableItemStack) {
				// If the data is already an UnmodifiableItemStack, return it without the copying
				// that would be done by DEFAULT.deserialize.
				// Note: We don't expect the additional ItemStack processing of
				// DataUtils.deserializeItemStack to be required here, because the item stack is not
				// freshly deserialized.
				return (UnmodifiableItemStack) data;
			}
			// Else: Try to load it as a normal ItemStack:
			return UnmodifiableItemStack.ofNonNull(DEFAULT.deserialize(data));
		}
	};

	private ItemStackSerializers() {
	}
}
