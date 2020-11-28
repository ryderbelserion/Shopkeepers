package com.nisovin.shopkeepers.config.value.types;

import org.bukkit.Material;

import com.nisovin.shopkeepers.config.value.SettingLoadException;
import com.nisovin.shopkeepers.config.value.UnknownMaterialException;
import com.nisovin.shopkeepers.config.value.ValueType;

public class MaterialValue extends ValueType<Material> {

	public static final MaterialValue INSTANCE = new MaterialValue();

	private static final StringValue stringValue = new StringValue();

	public MaterialValue() {
	}

	@Override
	public Material load(Object configValue) throws SettingLoadException {
		String materialName = stringValue.load(configValue);
		if (materialName == null) return null;
		// This assumes that legacy item conversion has already been performed:
		Material material = Material.matchMaterial(materialName); // Can be null
		if (material == null) {
			throw new UnknownMaterialException("Unknown material: " + materialName);
		}
		return material;
	}

	@Override
	public Object save(Material value) {
		if (value == null) return null;
		return value.name();
	}
}
