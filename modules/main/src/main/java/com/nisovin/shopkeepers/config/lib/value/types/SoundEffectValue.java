package com.nisovin.shopkeepers.config.lib.value.types;

import com.nisovin.shopkeepers.config.lib.value.ValueLoadException;
import com.nisovin.shopkeepers.config.lib.value.ValueParseException;
import com.nisovin.shopkeepers.config.lib.value.ValueType;
import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.bukkit.SoundEffect.SoundEffectDeserializeException;
import com.nisovin.shopkeepers.util.java.Validate;

public class SoundEffectValue extends ValueType<SoundEffect> {

	public static final SoundEffectValue INSTANCE = new SoundEffectValue();

	public SoundEffectValue() {
	}

	@Override
	public SoundEffect load(Object configValue) throws ValueLoadException {
		try {
			// Returns null if the config value is null.
			return SoundEffect.deserialize(configValue);
		} catch (SoundEffectDeserializeException e) {
			throw new ValueLoadException(e.getMessage(), e);
		}
	}

	@Override
	public Object save(SoundEffect value) {
		if (value == null) return null;
		return value.serialize();
	}

	@Override
	public SoundEffect parse(String input) throws ValueParseException {
		Validate.notNull(input, "input is null");
		try {
			// Note: This only supports the parsing from the compact representation currently (sound name only).
			// TODO Print a warning if it does not match a known Sound?
			return SoundEffect.deserialize(input);
		} catch (SoundEffectDeserializeException e) {
			throw new ValueParseException(e.getMessage(), e);
		}
	}
}
