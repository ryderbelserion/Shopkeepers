package com.nisovin.shopkeepers.config;

import static com.nisovin.shopkeepers.config.lib.ConfigHelper.toConfigKey;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.Messages;

public class ConfigTests {

	private List<String> getConfigClassKeys(Class<?> configClass) {
		Field[] fields = configClass.getDeclaredFields();
		List<String> keys = new ArrayList<>(fields.length);
		for (Field field : fields) {
			if (field.isSynthetic()) continue;
			if (!Modifier.isPublic(field.getModifiers())) {
				continue;
			}
			String configKey = toConfigKey(field.getName());
			keys.add(configKey);
		}
		return keys;
	}

	private Configuration loadConfigFromResource(String resourcePath) {
		InputStream configResource = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
		return YamlConfiguration.loadConfiguration(new InputStreamReader(configResource));
	}

	@Test
	public void testDefaultConfigConsistency() {
		// Expected default config:
		List<String> expectedKeys = this.getConfigClassKeys(Settings.class);

		// Actual default config:
		Configuration defaultConfig = this.loadConfigFromResource("config.yml");
		List<String> actualKeys = new ArrayList<>(defaultConfig.getKeys(false));

		// This checks: Missing keys, unexpected keys, order of keys, duplication of keys.
		Assert.assertEquals("Default config keys do not match the expected keys!", expectedKeys, actualKeys);
	}

	@Test
	public void testDefaultLanguageFilesConsistency() {
		// Expected messages:
		List<String> expectedKeys = this.getConfigClassKeys(Messages.class);

		// Actual default language files:
		String[] languageFilePaths = new String[] {
			"lang/language-en-default.yml",
			"lang/language-de.yml"
		};
		for (String languageFilePath : languageFilePaths) {
			Configuration languageFile = this.loadConfigFromResource(languageFilePath);
			List<String> actualKeys = new ArrayList<>(languageFile.getKeys(false));
			// This checks: Missing keys, unexpected keys, order of keys, duplication of keys.
			Assert.assertEquals("Keys of default language file '" + languageFilePath + "' do not match the expected keys!",
					expectedKeys, actualKeys);
		}
	}
}
