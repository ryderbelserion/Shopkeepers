package com.nisovin.shopkeepers.config.value.types;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
				List<String> extraMessages = Collections.emptyList();
				if (warning.contains("Unknown item type")) { // TODO this is ugly
					extraMessages = Arrays.asList(
							"All valid material names can be found here: https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html"
					);
				}
				// We can only throw unchecked exceptions here, so we wrap the exception here and unwrap it again
				// outside:
				throw new RuntimeException(new SettingLoadException(warning, extraMessages));
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
