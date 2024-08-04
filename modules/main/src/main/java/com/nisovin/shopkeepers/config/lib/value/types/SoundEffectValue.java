package com.nisovin.shopkeepers.config.lib.value.types;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;

public class SoundEffectValue extends ValueType<SoundEffect> {

	public static final SoundEffectValue INSTANCE = new SoundEffectValue();

	public SoundEffectValue() {
	}

	@Override
	public @Nullable SoundEffect load(@Nullable Object configValue) throws ValueLoadException {
		if (configValue == null) return null;
		try {
			return SoundEffect.SERIALIZER.deserialize(configValue);
		} catch (InvalidDataException e) {
			throw new ValueLoadException(e.getMessage(), e);
		}
	}

	@Override
	public @Nullable Object save(@Nullable SoundEffect value) {
		if (value == null) return null;
		return value.serialize();
	}

	@Override
	public SoundEffect parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		try {
			// Note: This only supports the parsing from the compact representation currently (sound
			// name only).
			// TODO Print a warning if it does not match a known Sound?
			return SoundEffect.SERIALIZER.deserialize(input);
		} catch (InvalidDataException e) {
			throw new ValueParseException(e.getMessage(), e);
		}
	}
}
