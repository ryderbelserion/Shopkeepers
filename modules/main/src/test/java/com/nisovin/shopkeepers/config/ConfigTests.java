package com.nisovin.shopkeepers.config;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;

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
		Set<String> expectedKeysSet = expectedDefaultConfig.getKeys(false);
		List<String> expectedKeys = new ArrayList<>(expectedKeysSet);
		Map<String, Object> expectedValues = expectedDefaultConfig.getValues(false);

		// Actual default config:
		Configuration defaultConfig = this.loadConfigFromResource("config.yml");
		Set<String> actualKeysSet = defaultConfig.getKeys(false);
		List<String> actualKeys = new ArrayList<>(actualKeysSet);
		Map<String, Object> actualValues = defaultConfig.getValues(false);
		ConfigUtils.convertSectionsToMaps(actualValues);

		// Check for missing keys:
		for (String expectedKey : expectedKeys) {
			Assert.assertTrue("The default config is missing the key '" + expectedKey + "'!",
					actualKeysSet.contains(expectedKey));

			// Compare values:
			Object expectedValue = expectedValues.get(expectedKey);
			Object actualValue = actualValues.get(expectedKey);
			Assert.assertEquals("The value for key '" + expectedKey + "' of the default config does not match the expected value!",
					expectedValue, actualValue);
		}

		// Check for unexpected keys:
		for (String actualKey : actualKeys) {
			Assert.assertTrue("The default config contains the unexpected key '" + actualKey + "'!",
					expectedKeysSet.contains(actualKey));
		}

		// Check the order of keys and for duplicated keys:
		Assert.assertEquals("The default config keys do not match the expected keys!", expectedKeys, actualKeys);
	}

	@Test
	public void testDefaultLanguageFilesConsistency() {
		// Expected default language file:
		MemoryConfiguration expectedDefaultLanguageFile = new MemoryConfiguration();
		Messages.getInstance().save(expectedDefaultLanguageFile);
		Set<String> expectedKeysSet = expectedDefaultLanguageFile.getKeys(false);
		List<String> expectedKeys = new ArrayList<>(expectedKeysSet);
		Map<String, Object> expectedValues = expectedDefaultLanguageFile.getValues(false);

		// Actual default language files:
		String[] languageFilePaths = new String[] {
			"lang/language-en-default.yml",
			"lang/language-de.yml"
		};
		for (String languageFilePath : languageFilePaths) {
			Configuration languageFile = this.loadConfigFromResource(languageFilePath);
			Set<String> actualKeysSet = languageFile.getKeys(false);
			List<String> actualKeys = new ArrayList<>(actualKeysSet);

			// Check for missing keys:
			for (String expectedKey : expectedKeys) {
				Assert.assertTrue("The default language file '" + languageFilePath + "' is missing the key '" + expectedKey + "'!",
						actualKeysSet.contains(expectedKey));
			}

			// Check for unexpected keys:
			for (String actualKey : actualKeys) {
				Assert.assertTrue("The default language file '" + languageFilePath + "' contains the unexpected key '" + actualKey + "'!",
						expectedKeysSet.contains(actualKey));
			}

			// Check the order of keys and for duplicated keys:
			Assert.assertEquals("The keys of the default language file '" + languageFilePath + "' do not match the expected keys!",
					expectedKeys, actualKeys);

			if (languageFilePath.equals("lang/language-en-default.yml")) {
				// Compare values:
				Map<String, Object> actualValues = languageFile.getValues(false);
				ConfigUtils.convertSectionsToMaps(actualValues);

				// Compare values:
				for (String expectedKey : expectedKeys) {
					Object expectedValue = expectedValues.get(expectedKey);
					Object actualValue = actualValues.get(expectedKey);
					Assert.assertEquals("The value for key '" + expectedKey + "' of the default language file '" + languageFilePath + "' does not match the expected value!",
							expectedValue, actualValue);
				}
			}
		}
	}
}
