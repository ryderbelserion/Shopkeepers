package com.nisovin.shopkeepers.util.yaml;

import java.util.Arrays;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.AbstractItemStackSerializationTest;
import com.nisovin.shopkeepers.util.java.StringUtils;

public class CompactYamlSerializationTest extends AbstractItemStackSerializationTest<@Nullable String> {

	@Override
	protected @Nullable String serialize(@Nullable ItemStack itemStack) {
		return YamlUtils.toCompactYaml(itemStack);
	}

	@Override
	protected @Nullable ItemStack deserialize(@Nullable String serialized) {
		if (serialized == null) return null;
		return YamlUtils.fromYaml(serialized);
	}

	@Override
	protected void testDeserialization(
			@Nullable ItemStack itemStack,
			@Nullable String serialized,
			@Nullable ItemStack deserialized
	) {
		super.testDeserialization(itemStack, serialized, deserialized);
		Assert.assertTrue(
				"Compact Yaml contains line breaks! <" + serialized + ">",
				!StringUtils.containsNewline(serialized)
		);
	}

	private void testCompactSerialization(@Nullable ItemStack itemStack) {
		String compactYaml = this.serialize(itemStack);
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
}
