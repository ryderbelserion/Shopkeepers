package com.nisovin.shopkeepers.util.yaml;

import java.util.Arrays;

import org.bukkit.inventory.ItemStack;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.util.AbstractItemStackSerializationTest;
import com.nisovin.shopkeepers.util.ConfigUtils;
import com.nisovin.shopkeepers.util.StringUtils;

public class YamlSerializationTest extends AbstractItemStackSerializationTest {

	@Override
	protected String serialize(ItemStack itemStack) {
		return YamlUtils.toYaml(itemStack);
	}

	@Override
	protected ItemStack deserialize(Object data) {
		return YamlUtils.fromYaml((String) data);
	}

	// Compact Yaml tests

	private String serializeCompact(ItemStack itemStack) {
		return YamlUtils.toCompactYaml(itemStack);
	}

	private void testCompactSerialization(ItemStack itemStack) {
		String compactYaml = this.serializeCompact(itemStack);
		ItemStack deserialized = this.deserialize(compactYaml);
		Assert.assertEquals(itemStack, deserialized);
		Assert.assertTrue("Compact Yaml contains line breaks! <" + compactYaml + ">", !StringUtils.containsNewline(compactYaml));
	}

	@Test
	public void testCompactItemStackSerialization() {
		this.createTestItemStacks().forEach(itemStack -> {
			this.testCompactSerialization(itemStack);
		});
	}

	@Test
	public void testCompactString() {
		String multiLineString = "Multiline\nText\n\nWith empty lines and trailing\n";
		// Expected output: In double quotes and newlines quoted.
		Assert.assertEquals("\"Multiline\\nText\\n\\nWith empty lines and trailing\\n\"", YamlUtils.toCompactYaml(multiLineString));
		Assert.assertEquals("[\"Multiline\\nText\\n\\nWith empty lines and trailing\\n\"]", YamlUtils.toCompactYaml(Arrays.asList(multiLineString)));
	}

	// Our Yaml instance should be configured to produce the same output as Bukkit's YamlConfiguration
	@Test
	public void testYamlMirrorsBukkit() {
		this.createTestItemStacks().forEach(itemStack -> {
			this.testYamlMirrorsBukkit(itemStack);
		});
	}

	private void testYamlMirrorsBukkit(ItemStack itemStack) {
		String yaml = this.serialize(itemStack);
		String bukkitYaml = ConfigUtils.toFlatConfigYaml(ConfigUtils.serialize(itemStack));
		Assert.assertEquals(bukkitYaml, yaml);
	}
}
