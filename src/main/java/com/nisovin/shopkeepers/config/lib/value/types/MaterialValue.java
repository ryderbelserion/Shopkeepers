package com.nisovin.shopkeepers.config.lib.value.types;

import org.bukkit.Material;

import com.nisovin.shopkeepers.config.lib.value.UnknownMaterialException;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.Validate;

public class MaterialValue extends ValueType<Material> {

	public static final MaterialValue INSTANCE = new MaterialValue();

	private static final StringValue stringValue = new StringValue();

	public MaterialValue() {
	}

	@Override
	public Material load(Object configValue) throws ValueLoadException {
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

	@Override
	public String format(Material value) {
		if (value == null) return "null";
		return value.name();
	}

	@Override
	public Material parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		Material material = Material.matchMaterial(input); // Can be null
		if (material == null) {
			throw new ValueParseException("Unknown material: " + input);
		}
		return material;
	}
}
