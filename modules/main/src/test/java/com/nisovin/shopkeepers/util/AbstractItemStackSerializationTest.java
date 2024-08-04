package com.nisovin.shopkeepers.util;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;

public abstract class AbstractItemStackSerializationTest<@Nullable S> extends AbstractBukkitTest {

	protected List<? extends @Nullable ItemStack> createTestItemStacks() {
		return TestItemStacks.createAllItemStacks();
	}

	protected abstract @Nullable S serialize(@Nullable ItemStack itemStack);

	protected abstract @Nullable ItemStack deserialize(@Nullable S serialized);

	private void testDeserialization(@Nullable ItemStack itemStack) {
		S serialized = this.serialize(itemStack);
		ItemStack deserialized = this.deserialize(serialized);
		this.testDeserialization(itemStack, serialized, deserialized);
	}

	protected void testDeserialization(
			@Nullable ItemStack itemStack,
			@Nullable S serialized,
			@Nullable ItemStack deserialized
	) {
		Assert.assertEquals(
				Unsafe.nullableAsNonNull(itemStack),
				Unsafe.nullableAsNonNull(deserialized)
		);
	}

	@Test
	public void testItemStackDeserialization() {
		this.createTestItemStacks().forEach(this::testDeserialization);
	}
}
