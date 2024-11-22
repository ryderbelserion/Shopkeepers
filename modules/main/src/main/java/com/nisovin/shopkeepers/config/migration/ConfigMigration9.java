package com.nisovin.shopkeepers.config.migration;

import org.bukkit.Sound;

import com.nisovin.shopkeepers.util.bukkit.SoundEffect;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Migrates the config from version 8 to version 9.
 */
public class ConfigMigration9 implements ConfigMigration {

	@Override
	public void apply(DataContainer configData) {
		// Migrate sound effects inside the config from enum names to namespaced keys:
		migrateSoundEffect(configData, "trade-succeeded-sound");
		migrateSoundEffect(configData, "trade-failed-sound");
		migrateSoundEffect(configData, "trade-notification-sound");
		migrateSoundEffect(configData, "shop-owner-trade-notification-sound");
	}

	private void migrateSoundEffect(DataContainer configData, String key) {
		var data = configData.get(key);
		if (data == null) {
			// Value not found: Nothing to migrate.
			Log.info("  Setting '" + key + "': Not found. Skipping migration.");
			return;
		}

		try {
			var soundEffect = SoundEffect.SERIALIZER.deserialize(data);
			if (soundEffect.getSound() != null) {
				Log.info("  Setting '" + key + "': Sound value found. Nothing to migrate.");
				return;
			}

			var soundName = soundEffect.getSoundName();
			assert soundName != null;
			if (soundName.isEmpty()) {
				// Empty sound name: Nothing to migrate.
				Log.info("  Setting '" + key + "': Empty sound name. Nothing to migrate.");
				return;
			}

			// Try to get the sound by the old enum name:
			Sound sound;
			try {
				sound = Sound.valueOf(soundName);
			} catch (IllegalArgumentException e) {
				// Sound not found: Skipping migration.
				Log.info("  Setting '" + key + "': Sound enum value not found. Skipping migration.");
				return;
			}

			// Migrate to sound namespaced key:
			var newSoundEffect = new SoundEffect(sound)
					.withCategory(soundEffect.getCategory())
					.withPitch(soundEffect.getPitch())
					.withVolume(soundEffect.getVolume());

			var newData = SoundEffect.SERIALIZER.serialize(newSoundEffect);
			assert newData != null;

			Log.info("  Migrating setting '" + key + "' from sound name '" + soundName
					+ "' to sound key '" + sound.getKey() + "'.");
			configData.set(key, newData);
		} catch (InvalidDataException e) {
			Log.warning("  Setting '" + key + "': Failed to load. Skipping migration.");
			return;
		}
	}
}
