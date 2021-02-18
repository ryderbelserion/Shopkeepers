package com.nisovin.shopkeepers.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.util.ConfigUtils;

public class ConfigTests extends AbstractBukkitTest {

	private Configuration loadConfigFromResource(String resourcePath) {
		InputStream configResource = this.getClass().getClassLoader().getResourceAsStream(resourcePath);
		return YamlConfiguration.loadConfiguration(new InputStreamReader(configResource));
	}

	@Test
	public void testDefaultConfigConsistency() {
		// Expected default config:
		MemoryConfiguration expectedDefaultConfig = new MemoryConfiguration();
		Settings.getInstance().save(expectedDefaultConfig);
		List<String> expectedKeys = new ArrayList<>(expectedDefaultConfig.getKeys(false));
		Map<String, Object> expectedValues = expectedDefaultConfig.getValues(false);

		// Actual default config:
		Configuration defaultConfig = this.loadConfigFromResource("config.yml");
		List<String> actualKeys = new ArrayList<>(defaultConfig.getKeys(false));
		Map<String, Object> actualValues = defaultConfig.getValues(false);
		ConfigUtils.convertSectionsToMaps(actualValues);

		// Check for missing keys, unexpected keys, order of keys, duplication of keys:
		Assert.assertEquals("Default config keys do not match the expected keys!", expectedKeys, actualKeys);
		// Compare setting values:
		Assert.assertEquals("Default config does not match the expected default config!", expectedValues, actualValues);
	}

	@Test
	public void testDefaultLanguageFilesConsistency() {
		// Expected default language file:
		MemoryConfiguration expectedDefaultLanguageFile = new MemoryConfiguration();
		Messages.getInstance().save(expectedDefaultLanguageFile);
		List<String> expectedKeys = new ArrayList<>(expectedDefaultLanguageFile.getKeys(false));
		Map<String, Object> expectedValues = expectedDefaultLanguageFile.getValues(false);

		// Actual default language files:
		String[] languageFilePaths = new String[] {
			"lang/language-en-default.yml",
			"lang/language-de.yml"
		};
		for (String languageFilePath : languageFilePaths) {
			Configuration languageFile = this.loadConfigFromResource(languageFilePath);
			List<String> actualKeys = new ArrayList<>(languageFile.getKeys(false));

			// Check for missing keys, unexpected keys, order of keys, duplication of keys:
			Assert.assertEquals("Keys of default language file '" + languageFilePath + "' do not match the expected keys!",
					expectedKeys, actualKeys);
			if (languageFilePath.equals("lang/language-en-default.yml")) {
				// Compare values:
				Map<String, Object> actualValues = languageFile.getValues(false);
				ConfigUtils.convertSectionsToMaps(actualValues);
				Assert.assertEquals("Default language file '" + languageFilePath + "' does not match the expected default language file!",
						expectedValues, actualValues);
			}
		}
	}
}
