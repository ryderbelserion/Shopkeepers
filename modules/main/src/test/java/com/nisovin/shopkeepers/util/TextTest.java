package com.nisovin.shopkeepers.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;

import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;

public class TextTest extends AbstractBukkitTest {

	// Tests the Text parsing and backwards conversion with the actual messages of the default language file
	@Test
	public void testRealMessageConversions() throws IOException {
		// Load default language file:
		Configuration config;
		ClassLoader classLoader = this.getClass().getClassLoader();
		try (Reader reader = new BufferedReader(new InputStreamReader(classLoader.getResourceAsStream("lang/language-en-default.yml")))) {
			config = YamlConfiguration.loadConfiguration(reader);
		}

		Set<String> configKeys = config.getKeys(false);
		int tested = 0;
		for (String key : configKeys) {
			Object value = config.get(key);
			if (!(value instanceof String)) continue;

			tested++;
			String stringText = TextUtils.colorize((String) value);
			Text text = Text.parse(stringText);
			assertEquals("Plain format text does not match parsing input for: " + key, stringText, text.toPlainFormatText());
			assertEquals("Plain text does not match parsing input for: " + key, stringText, text.toPlainText());
		}
		System.out.println("Tested config messages: " + tested + " / " + configKeys.size() + " (total config entries)");
		assertTrue("The test didn't actually test anything!", tested > 0);
	}
}
