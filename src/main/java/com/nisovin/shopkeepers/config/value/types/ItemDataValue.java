package com.nisovin.shopkeepers.config.value.types;

import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.config.value.ValueType;
import com.nisovin.shopkeepers.util.ItemData;

public class ItemDataValue extends ValueType<ItemData> {

	public static final ItemDataValue INSTANCE = new ItemDataValue();

	public ItemDataValue() {
	}

	@Override
	public ItemData load(Object configValue) throws SettingLoadException {
		ItemData itemData = null;
		try {
			// Returns null if the config value is null. Otherwise triggers a warning, which we translate into an
			// exception.
			itemData = ItemData.deserialize(configValue, (warning) -> {
				String errorMsg = warning;
				if (warning.contains("Unknown item type")) { // TODO this is ugly
					errorMsg = warning + " (All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html)";
				}
				// We can only throw unchecked exceptions here, so we wrap the exception here and unwrap it again
				// outside:
				throw new RuntimeException(new SettingLoadException(errorMsg));
			});
		} catch (RuntimeException e) {
			if (e.getCause() instanceof SettingLoadException) {
				throw (SettingLoadException) e.getCause();
			} else {
				throw e;
			}
		}
		return itemData;
	}

	@Override
	public Object save(ItemData value) {
		if (value == null) return null;
		return value.serialize();
	}
}
