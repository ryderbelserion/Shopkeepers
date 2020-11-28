package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.ValueLoadException;
import com.nisovin.shopkeepers.config.value.UnknownMaterialException;
import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.util.ItemData;
import com.nisovin.shopkeepers.util.ItemData.ItemDataDeserializeException;
import com.nisovin.shopkeepers.util.ItemData.UnknownItemTypeException;

public class ItemDataValue extends ValueType<ItemData> {

	public static final ItemDataValue INSTANCE = new ItemDataValue();

	public ItemDataValue() {
	}

	@Override
	public ItemData load(Object configValue) throws ValueLoadException {
		try {
			// Returns null if the config value is null.
			return ItemData.deserialize(configValue);
		} catch (UnknownItemTypeException e) {
			throw new UnknownMaterialException(e.getMessage(), e);
		} catch (ItemDataDeserializeException e) {
			throw new ValueLoadException(e.getMessage(), e);
		}
	}

	@Override
	public Object save(ItemData value) {
		if (value == null) return null;
		return value.serialize();
	}
}
