package com.nisovin.shopkeepers.util;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Set;
import java.util.logging.Logger;

import org.bukkit.ChatColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.ClassUtils;

public class TextTest extends AbstractBukkitTest {

	private static final Logger LOGGER = Logger.getLogger(TextTest.class.getCanonicalName());

	@Test
	public void testIsBukkitHexCode() {
		assertTrue("Bukkit hex code with & is not considered valid",
				TextUtils.isBukkitHexCode("&x&a&a&b&b&c&c"));
		assertTrue("Bukkit hex code with § is not considered valid",
				TextUtils.isBukkitHexCode("§x§a§a§b§b§c§c"));

		assertFalse("Too long Bukkit hex code is considered valid",
				TextUtils.isBukkitHexCode("&x&a&a&b&b&c&c&d"));
		assertFalse("Too short Bukkit hex code is considered valid",
				TextUtils.isBukkitHexCode("&x&a&a&b&b&c"));
		assertFalse("Incomplete Bukkit hex code is considered valid",
				TextUtils.isBukkitHexCode("&x&a&a&b&b&c&"));
		assertFalse("Bukkit hex code with invalid format character is considered valid",
				TextUtils.isBukkitHexCode("&#&a&a&b&b&c&c"));
		assertFalse("Bukkit hex code with invalid hex character is considered valid",
				TextUtils.isBukkitHexCode("&x&a&a&b&b&c&g"));
		assertFalse("Non-Bukkit hex code is considered valid Bukkit hex code",
				TextUtils.isBukkitHexCode("#aabbcc"));
		assertFalse("Non-Bukkit hex code with ampersand is considered valid Bukkit hex code",
				TextUtils.isBukkitHexCode("&#aabbcc"));
	}

	@Test
	public void testIsHexCode() {
		assertTrue("Hex code is not considered valid",
				TextUtils.isHexCode("#aabbcc"));

		assertFalse("Bukkit hex code is considered valid hex code",
				TextUtils.isHexCode("§x§a§a§b§b§c§c"));
		assertFalse("Too long hex code is considered valid",
				TextUtils.isHexCode("#aabbccd"));
		assertFalse("Too short hex code is considered valid",
				TextUtils.isHexCode("#aabbc"));
		assertFalse("Hex code without hex char is considered valid",
				TextUtils.isHexCode("aabbcc"));
		assertFalse("Hex code without hex char but correct length is considered valid",
				TextUtils.isHexCode("xaabbcc"));
		assertFalse("Hex code with invalid hex character is considered valid",
				TextUtils.isHexCode("#aabbcg"));
	}

	@Test
	public void testFromBukkitHexCode() {
		String expected = "#aabbcc";
		assertEquals("fromBukkitHexCode with & failed",
				expected, TextUtils.fromBukkitHexCode("&x&a&a&b&b&c&c"));
		assertEquals("fromBukkitHexCode with § failed",
				expected, TextUtils.fromBukkitHexCode("§x§a§a§b§b§c§c"));
	}

	@Test
	public void testToBukkitHexCode() {
		assertEquals("toBukkitHexCode with & failed",
				"&x&a&a&b&b&c&c", TextUtils.toBukkitHexCode("#aabbcc", TextUtils.COLOR_CHAR_ALTERNATIVE));
		assertEquals("toBukkitHexCode with § failed",
				"§x§a§a§b§b§c§c", TextUtils.toBukkitHexCode("#aabbcc", ChatColor.COLOR_CHAR));
	}

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

		Set<String> configKeys = config.getKeys(false);
		int tested = 0;
		for (String key : configKeys) {
			Object value = config.get(key);
			if (!(value instanceof String)) continue;

			tested++;
			String formatText = (String) value;
			String colorizedText = TextUtils.colorize(formatText);
			Text text = Text.parse(formatText);
			assertEquals(
					"Format text does not match parsing input for: " + key,
					formatText, text.toFormat()
			);

			if (text.isPlainText()) {
				// Note: toPlainText is allowed to apply certain conversions (e.g. convert hex color
				// codes to Bukkit's format). But our default config is not expected to currently
				// use any features that are affected by such conversions.
				assertEquals(
						"Plain text does not match parsing input for: " + key,
						colorizedText, text.toPlainText()
				);
			}
		}
		LOGGER.info("Tested config messages: " + tested + " / " + configKeys.size()
				+ " (total config entries)");
		assertTrue("The test didn't actually test anything!", tested > 0);
	}
}
