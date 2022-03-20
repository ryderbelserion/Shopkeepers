package com.nisovin.shopkeepers.util;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.ClassUtils;

public class TextTest extends AbstractBukkitTest {

	private static final Logger LOGGER = Logger.getLogger(TextTest.class.getCanonicalName());

	// Tests the Text parsing and backwards conversion with the actual messages of the default
	// language file
	@Test
	public void testRealMessageConversions() throws IOException {
		// Load default language file:
		Configuration config;
		String languageFilePath = Messages.getDefaultLanguageFilePath();
		InputStream languageFileResource = ClassUtils.getResource(this.getClass(), languageFilePath);
		try (Reader reader = new BufferedReader(new InputStreamReader(languageFileResource))) {
			config = YamlConfiguration.loadConfiguration(reader);
		}

		Set<@NonNull String> configKeys = Unsafe.castNonNull(config.getKeys(false));
		int tested = 0;
		for (String key : configKeys) {
			Object value = config.get(key);
			if (!(value instanceof String)) continue;

			tested++;
			String stringText = TextUtils.colorize((String) value);
			Text text = Text.parse(stringText);
			assertEquals(
					"Plain format text does not match parsing input for: " + key,
					stringText, text.toPlainFormatText()
			);
			assertEquals(
					"Plain text does not match parsing input for: " + key,
					stringText, text.toPlainText()
			);
		}
		LOGGER.info("Tested config messages: " + tested + " / " + configKeys.size()
				+ " (total config entries)");
		assertTrue("The test didn't actually test anything!", tested > 0);
	}
}
