package com.nisovin.shopkeepers.util.json;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.AbstractItemStackSerializationTest;

public class JsonSerializationTest extends AbstractItemStackSerializationTest<@Nullable String> {

	@Override
	protected @Nullable String serialize(@Nullable ItemStack itemStack) {
		return JsonUtils.toJson(itemStack);
	}

	@Override
	protected @Nullable ItemStack deserialize(@Nullable String serialized) {
		return JsonUtils.fromJson(serialized);
	}

	@Test
	public void testSerializeSpecialFloatingPointNumbers() {
		testSerializeSpecialFloatingPointNumber(Double.NaN);
		testSerializeSpecialFloatingPointNumber(Double.POSITIVE_INFINITY);
		testSerializeSpecialFloatingPointNumber(Double.NEGATIVE_INFINITY);
		testSerializeSpecialFloatingPointNumber(Double.MAX_VALUE);
		testSerializeSpecialFloatingPointNumber(Double.MIN_VALUE);
		// Note: Floats are not preserved during deserialization, because we read all floating point
		// numbers as doubles.
	}

	private void testSerializeSpecialFloatingPointNumber(Number number) {
		String json = JsonUtils.toJson(number);
		Object deserialized = JsonUtils.fromJson(json);
		Assert.assertEquals(number, Unsafe.nullableAsNonNull(deserialized));
	}
}
