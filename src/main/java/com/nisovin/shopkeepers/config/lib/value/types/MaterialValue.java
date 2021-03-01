package com.nisovin.shopkeepers.config.lib.value.types;

import org.bukkit.Material;

import com.nisovin.shopkeepers.config.lib.value.InvalidMaterialException;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;

public class MaterialValue extends MinecraftEnumValue<Material> {

	public static final MaterialValue INSTANCE = new MaterialValue();

	public MaterialValue() {
		super(Material.class);
	}

	@Override
	protected ValueLoadException newInvalidEnumValueException(String valueName, ValueParseException parseException) {
		return new InvalidMaterialException(parseException.getMessage(), parseException);
	}

	@Override
	public Material parse(String input) throws ValueParseException {
		Material material = super.parse(input);
		assert material != null; // Parsing throws an exception instead
		// Filter legacy materials:
		if (material.isLegacy()) {
			throw new ValueParseException("Unsupported legacy Material: " + input);
		}
		return material;
	}
}
