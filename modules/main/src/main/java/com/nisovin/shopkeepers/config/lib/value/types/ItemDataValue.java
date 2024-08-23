package com.nisovin.shopkeepers.config.lib.value.types;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.InvalidMaterialException;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.bukkit.UnknownMaterialException;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.java.ThrowableUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ItemDataValue extends ValueType<ItemData> {

	public static final ItemDataValue INSTANCE = new ItemDataValue();

	public ItemDataValue() {
	}

	@Override
	public @Nullable ItemData load(@Nullable Object configValue) throws ValueLoadException {
		if (configValue == null) return null;
		try {
			return ItemData.SERIALIZER.deserialize(configValue);
		} catch (InvalidDataException e) {
			if (ThrowableUtils.getRootCause(e) instanceof UnknownMaterialException) {
				throw new InvalidMaterialException(e.getMessage(), e);
			}
			throw new ValueLoadException(e.getMessage(), e);
		}
	}

	@Override
	public @Nullable Object save(@Nullable ItemData value) {
		if (value == null) return null;
		return value.serialize();
	}

	@Override
	public String format(@Nullable ItemData value) {
		if (value == null) return "null";
		StringBuilder builder = new StringBuilder(value.getType().name());
		if (value.hasItemMeta()) {
			builder.append(" (+NBT)");
		}
		return builder.toString();
	}

	@Override
	public ItemData parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		try {
			// Note: This only supports the parsing from the compact representation currently (item
			// type only).
			return ItemData.SERIALIZER.deserialize(input);
		} catch (InvalidDataException e) {
			throw new ValueParseException(e.getMessage(), e);
		}
	}
}
