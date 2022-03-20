package com.nisovin.shopkeepers.util.yaml;

import java.util.Arrays;
import java.util.Collections;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.AbstractItemStackSerializationTest;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public class YamlSerializationTest extends AbstractItemStackSerializationTest {

	@Override
	protected @Nullable String serialize(@Nullable ItemStack itemStack) {
		return YamlUtils.toYaml(itemStack);
	}

	@Override
	protected @Nullable ItemStack deserialize(@Nullable Object data) {
		if (data == null) return null;
		return YamlUtils.fromYaml((String) data);
	}

	// Compact Yaml tests

	private String serializeCompact(@Nullable ItemStack itemStack) {
		return YamlUtils.toCompactYaml(itemStack);
	}

	private void testCompactSerialization(@Nullable ItemStack itemStack) {
		String compactYaml = this.serializeCompact(itemStack);
		ItemStack deserialized = this.deserialize(compactYaml);
		Assert.assertEquals(
				Unsafe.nullableAsNonNull(itemStack),
				Unsafe.nullableAsNonNull(deserialized)
		);
		Assert.assertTrue(
				"Compact Yaml contains line breaks! <" + compactYaml + ">",
				!StringUtils.containsNewline(compactYaml)
		);
	}

	@Test
	public void testCompactItemStackSerialization() {
		this.createTestItemStacks().forEach(this::testCompactSerialization);
	}

	@Test
	public void testCompactString() {
		String multiLineString = "Multiline\nText\n\nWith empty lines and trailing\n";
		// Expected output: In double quotes and newlines quoted.
		Assert.assertEquals(
				"\"Multiline\\nText\\n\\nWith empty lines and trailing\\n\"",
				YamlUtils.toCompactYaml(multiLineString)
		);
		Assert.assertEquals(
				"[\"Multiline\\nText\\n\\nWith empty lines and trailing\\n\"]",
				YamlUtils.toCompactYaml(Arrays.asList(multiLineString))
		);
	}

	// Our Yaml instance should be configured to produce the same output as Bukkit's
	// YamlConfiguration
	@Test
	public void testYamlMirrorsBukkit() {
		this.createTestItemStacks().forEach(this::testYamlMirrorsBukkit);
	}

	private void testYamlMirrorsBukkit(@Nullable ItemStack itemStack) {
		String yaml = this.serialize(itemStack);
		String bukkitYaml;
		if (itemStack == null) {
			bukkitYaml = ConfigUtils.toFlatConfigYaml(Collections.emptyMap());
		} else {
			bukkitYaml = ConfigUtils.toFlatConfigYaml(ConfigUtils.serialize(itemStack));
		}
		Assert.assertEquals(bukkitYaml, Unsafe.nullableAsNonNull(yaml));
	}
}
