package com.nisovin.shopkeepers.util.data.serialization.bukkit;

import org.bukkit.inventory.ItemStack;

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
		public Object serialize(ItemStack value) {
			Validate.notNull(value, "value is null");
			return value;
		}

		@Override
		public ItemStack deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			if (!(data instanceof ItemStack)) {
				throw new InvalidDataException("Data is not of type ItemStack, but "
						+ data.getClass().getName() + "!");
			}
			ItemStack itemStack = (ItemStack) data;
			itemStack = DataUtils.deserializeItemStack(itemStack);
			return itemStack;
		}
	};

	/**
	 * A {@link DataSerializer} for {@link UnmodifiableItemStack} values.
	 */
	public static final DataSerializer<UnmodifiableItemStack> UNMODIFIABLE = new DataSerializer<UnmodifiableItemStack>() {
		@Override
		public Object serialize(UnmodifiableItemStack value) {
			Validate.notNull(value, "value is null");
			return DataUtils.serializeItemStack(value);
		}

		@Override
		public UnmodifiableItemStack deserialize(Object data) throws InvalidDataException {
			ItemStack itemStack = DEFAULT.deserialize(data);
			return UnmodifiableItemStack.of(itemStack);
		}
	};

	private ItemStackSerializers() {
	}
}
