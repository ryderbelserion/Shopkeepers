package com.nisovin.shopkeepers.config.lib.value.types;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.config.lib.value.InvalidEntityTypeException;
import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;

public class EntityTypeValue extends MinecraftEnumValue<EntityType> {

	public static final EntityTypeValue INSTANCE = new EntityTypeValue();

	public EntityTypeValue() {
		super(EntityType.class);
	}

	@Override
	protected ValueLoadException newInvalidEnumValueException(
			String valueName,
			ValueParseException parseException
	) {
		return new InvalidEntityTypeException(parseException.getMessage(), parseException);
	}
}
