package com.nisovin.shopkeepers.config.lib.value.types;

import org.bukkit.Material;

import com.nisovin.shopkeepers.config.lib.value.UnknownMaterialException;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;

public class MaterialValue extends MinecraftEnumValue<Material> {

	public static final MaterialValue INSTANCE = new MaterialValue();

	public MaterialValue() {
		super(Material.class);
	}

	@Override
	protected ValueLoadException newUnknownEnumValueException(String valueName, ValueParseException parseException) {
		return new UnknownMaterialException(parseException.getMessage(), parseException);
	}
}
