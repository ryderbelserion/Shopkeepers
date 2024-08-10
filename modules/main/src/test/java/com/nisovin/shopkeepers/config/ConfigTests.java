package com.nisovin.shopkeepers.config;

import static com.nisovin.shopkeepers.testutil.matchers.IsDataFuzzyEqual.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.java.ClassUtils;

public class ConfigTests extends AbstractBukkitTest {

	@BeforeClass
	public static void setup() {
	}

	@AfterClass
	public static void cleanup() {
		ItemData.resetSerializerPrefersPlainTextFormat();
	}

	private DataContainer loadConfigFromResource(String resourcePath, boolean useBukkitSettings) {
		InputStream configResource = ClassUtils.getResource(this.getClass(), resourcePath);

		YamlConfiguration config = useBukkitSettings ? new YamlConfiguration() : ConfigUtils.newYamlConfig();
		try {
			config.load(new InputStreamReader(configResource));
		} catch (IOException | InvalidConfigurationException e) {
			throw new RuntimeException(e);
		}

		// In order to be able to compare the contents of this loaded config with the data of an
		// in-memory serialized config, we need to convert all config sections to Maps.
		// Modifiable shallow copy:
		Map<String, Object> configData = ConfigUtils.getValues(config);
		ConfigUtils.convertSectionsToMaps(configData);
		return DataContainer.ofNonNull(configData); // Map-based data container
	}

	@Test
	public void testDefaultConfigConsistency() {
		// Expected default config:
		DataContainer expectedDefaultConfig = DataContainer.create();
		Settings.getInstance().save(expectedDefaultConfig);
		Set<? extends String> expectedKeysSet = expectedDefaultConfig.getKeys();
		List<? extends String> expectedKeys = new ArrayList<>(expectedKeysSet);
		Map<? extends String, @NonNull ?> expectedValues = expectedDefaultConfig.getValues();

		// Actual default config:
		// Load the config just like Bukkit does it (i.e. with the default '.' section path
		// separator).
		DataContainer defaultConfig = this.loadConfigFromResource("config.yml", true);
		Set<? extends String> actualKeysSet = defaultConfig.getKeys();
		List<? extends String> actualKeys = new ArrayList<>(actualKeysSet);
		Map<? extends String, @NonNull ?> actualValues = defaultConfig.getValues();

		// Check for missing keys:
		for (String expectedKey : expectedKeys) {
			Assert.assertTrue("The default config is missing the key '" + expectedKey + "'!",
					actualKeysSet.contains(expectedKey));

			// Compare values:
			Object expectedValue = expectedValues.get(expectedKey);
			Object actualValue = actualValues.get(expectedKey);
			Assert.assertThat("The value for key '" + expectedKey
					+ "' of the default config does not match the expected value!",
					actualValue, dataFuzzyEqualTo(expectedValue));
		}

		// Check for unexpected keys:
		for (String actualKey : actualKeys) {
			Assert.assertTrue("The default config contains the unexpected key '" + actualKey + "'!",
					expectedKeysSet.contains(actualKey));
		}

		// Check the order of keys and for duplicated keys:
		Assert.assertEquals("The default config keys do not match the expected keys!",
				expectedKeys, actualKeys);
	}

	@Test
	public void testDefaultLanguageFilesConsistency() {
		// Expected default language file:
		DataContainer expectedDefaultLanguageFile = DataContainer.create();
		Messages.getInstance().save(expectedDefaultLanguageFile);
		Set<? extends String> expectedKeysSet = expectedDefaultLanguageFile.getKeys();
		List<? extends String> expectedKeys = new ArrayList<>(expectedKeysSet);
		Map<? extends String, @NonNull ?> expectedValues = expectedDefaultLanguageFile.getValues();

		// Actual default language files:
		@NonNull String[] languageFilePaths = new @NonNull String[] {
				"lang/language-en-default.yml",
				"lang/language-de.yml"
		};
		for (String languageFilePath : languageFilePaths) {
			DataContainer languageFile = this.loadConfigFromResource(languageFilePath, false);
			Set<? extends String> actualKeysSet = languageFile.getKeys();
			List<? extends String> actualKeys = new ArrayList<>(actualKeysSet);

			// Check for missing keys:
			for (String expectedKey : expectedKeys) {
				Assert.assertTrue("The default language file '" + languageFilePath
						+ "' is missing the key '" + expectedKey + "'!",
						actualKeysSet.contains(expectedKey));
			}

			// Check for unexpected keys:
			for (String actualKey : actualKeys) {
				Assert.assertTrue("The default language file '" + languageFilePath
						+ "' contains the unexpected key '" + actualKey + "'!",
						expectedKeysSet.contains(actualKey));
			}

			// Check the order of keys and for duplicated keys:
			Assert.assertEquals("The keys of the default language file '" + languageFilePath
					+ "' do not match the expected keys!",
					expectedKeys, actualKeys);

			if (languageFilePath.equals("lang/language-en-default.yml")) {
				// Compare values:
				Map<? extends String, @NonNull ?> actualValues = languageFile.getValues();

				// Compare values:
				for (String expectedKey : expectedKeys) {
					Object expectedValue = expectedValues.get(expectedKey);
					Object actualValue = actualValues.get(expectedKey);
					Assert.assertEquals("The value for key '" + expectedKey
							+ "' of the default language file '" + languageFilePath
							+ "' does not match the expected value!",
							Unsafe.nullableAsNonNull(expectedValue),
							Unsafe.nullableAsNonNull(actualValue)
					);
				}
			}
		}
	}
}
