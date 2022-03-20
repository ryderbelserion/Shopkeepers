package com.nisovin.shopkeepers.util;

import org.bukkit.NamespacedKey;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;

public class NamespacedKeyUtilsTests {

	@Test
	public void testParsing() {
		// No namespace specified (implicitly uses the Minecraft namespace):
		testParsing("", null); // Empty
		testParsing("bla", NamespacedKey.minecraft("bla"));
		// Different word separators:
		testParsing("bla_blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing("bla-blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing("bla blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing("bla.blubb", NamespacedKey.minecraft("bla.blubb"));
		testParsing("bla/blubb", NamespacedKey.minecraft("bla/blubb"));
		// Leading and trailing spaces:
		testParsing(" bla_blubb  ", NamespacedKey.minecraft("bla_blubb"));
		// Upper case characters:
		testParsing("bla_BluBB", NamespacedKey.minecraft("bla_blubb"));

		testParsing(":", null);
		testParsing(":bla", NamespacedKey.minecraft("bla"));
		testParsing(":bla_blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing(":bla-blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing(":bla blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing(":bla.blubb", NamespacedKey.minecraft("bla.blubb"));
		testParsing(":bla/blubb", NamespacedKey.minecraft("bla/blubb"));
		testParsing(" :bla_blubb  ", NamespacedKey.minecraft("bla_blubb"));
		testParsing(":bla_BluBB", NamespacedKey.minecraft("bla_blubb"));

		// Explicit Minecraft namespace:
		testParsing("minecraft:", null);
		testParsing("minecraft:bla", NamespacedKey.minecraft("bla"));
		testParsing("minecraft:bla_blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing("minecraft:bla-blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing("minecraft:bla blubb", NamespacedKey.minecraft("bla_blubb"));
		testParsing("minecraft:bla.blubb", NamespacedKey.minecraft("bla.blubb"));
		testParsing("minecraft:bla/blubb", NamespacedKey.minecraft("bla/blubb"));
		testParsing(" minecraft:bla_blubb  ", NamespacedKey.minecraft("bla_blubb"));
		testParsing("minecraft:bla_BluBB", NamespacedKey.minecraft("bla_blubb"));

		// Custom namespace:
		testParsing("custom:", null);
		testParsing("custom:bla", NamespacedKeyUtils.create("custom", "bla"));
		testParsing("custom:bla_blubb", NamespacedKeyUtils.create("custom", "bla_blubb"));
		// Dash might be valid:
		testParsing("custom:bla-blubb", NamespacedKeyUtils.create("custom", "bla-blubb"));
		testParsing("custom:bla blubb", NamespacedKeyUtils.create("custom", "bla_blubb"));
		testParsing("custom:bla.blubb", NamespacedKeyUtils.create("custom", "bla.blubb"));
		testParsing("custom:bla/blubb", NamespacedKeyUtils.create("custom", "bla/blubb"));
		testParsing(" custom:bla_blubb  ", NamespacedKeyUtils.create("custom", "bla_blubb"));
		testParsing("custom:bla_BluBB", NamespacedKeyUtils.create("custom", "bla_blubb"));
	}

	private void testParsing(String input, @Nullable NamespacedKey expected) {
		NamespacedKey parsed = NamespacedKeyUtils.parse(input);
		Assert.assertEquals(
				Unsafe.nullableAsNonNull(expected),
				Unsafe.nullableAsNonNull(parsed)
		);
	}
}
