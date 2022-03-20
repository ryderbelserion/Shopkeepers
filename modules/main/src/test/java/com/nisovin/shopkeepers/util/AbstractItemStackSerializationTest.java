package com.nisovin.shopkeepers.util;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;

public abstract class AbstractItemStackSerializationTest extends AbstractBukkitTest {

	protected List<? extends @Nullable ItemStack> createTestItemStacks() {
		return TestItemStacks.createAllItemStacks();
	}

	protected abstract @Nullable Object serialize(@Nullable ItemStack itemStack);

	protected abstract @Nullable ItemStack deserialize(@Nullable Object data);

	protected void testDeserialization(@Nullable ItemStack itemStack) {
		Object data = this.serialize(itemStack);
		ItemStack deserialized = this.deserialize(data);
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
