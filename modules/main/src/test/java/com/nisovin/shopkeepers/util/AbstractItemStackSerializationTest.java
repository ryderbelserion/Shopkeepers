package com.nisovin.shopkeepers.util;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;

public abstract class AbstractItemStackSerializationTest extends AbstractBukkitTest {

	protected List<ItemStack> createTestItemStacks() {
		return TestItemStacks.createAllItemStacks();
	}

	protected abstract Object serialize(ItemStack itemStack);

	protected abstract ItemStack deserialize(Object data);

	protected void testDeserialization(ItemStack itemStack) {
		Object data = this.serialize(itemStack);
		ItemStack deserialized = this.deserialize(data);
		Assert.assertEquals(itemStack, deserialized);
	}

	@Test
	public void testItemStackDeserialization() {
		this.createTestItemStacks().forEach(this::testDeserialization);
	}
}
