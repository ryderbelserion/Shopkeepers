package com.nisovin.shopkeepers.config.value.types;

import org.bukkit.Material;

import com.nisovin.shopkeepers.config.value.UnknownMaterialException;
import com.nisovin.shopkeepers.config.value.ValueLoadException;
import com.nisovin.shopkeepers.config.value.ValueParseException;
import com.nisovin.shopkeepers.config.value.ValueType;
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
