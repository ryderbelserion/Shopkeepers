package com.nisovin.shopkeepers.util.bukkit;

import java.util.List;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.util.TestItemStacks;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class UnmodifiableItemStackTest extends AbstractBukkitTest {

	// Also contains a null ItemStack!
	private List<? extends @Nullable ItemStack> createTestItemStacks() {
		return TestItemStacks.createAllItemStacks();
	}

	@Test
	public void testUnmodifiableEqualsBukkit() {
		this.createTestItemStacks().forEach(itemStack -> {
			@Nullable UnmodifiableItemStack unmodifiableItemStack = UnmodifiableItemStack.of(itemStack);
			Assert.assertTrue(ItemUtils.equals(unmodifiableItemStack, itemStack));
		});
	}

	@Test
	public void testUnmodifiableEqualsUnmodifiable() {
		this.createTestItemStacks().forEach(itemStack -> {
			@Nullable UnmodifiableItemStack unmodifiableItemStack = UnmodifiableItemStack.of(itemStack);
			@Nullable UnmodifiableItemStack unmodifiableItemStack2 = UnmodifiableItemStack.of(itemStack);
			Assert.assertEquals(
					Unsafe.nullableAsNonNull(unmodifiableItemStack),
					Unsafe.nullableAsNonNull(unmodifiableItemStack2)
			);
		});
	}

	@Test
	public void testBukkitIsSimilarUnmodifiable() {
		this.createTestItemStacks().forEach(itemStack -> {
			@Nullable UnmodifiableItemStack unmodifiableItemStack = UnmodifiableItemStack.of(itemStack);
			Assert.assertTrue(ItemUtils.isSimilar(
					itemStack,
					ItemUtils.asItemStackOrNull(unmodifiableItemStack)
			));
		});
	}

	@Test
	public void testUnmodifiableIsSimilarBukkit() {
		this.createTestItemStacks().forEach(itemStack -> {
			@Nullable UnmodifiableItemStack unmodifiableItemStack = UnmodifiableItemStack.of(itemStack);
			Assert.assertTrue(ItemUtils.isSimilar(unmodifiableItemStack, itemStack));
		});
	}

	@Test
	public void testUnmodifiableIsSimilarUnmodifiable() {
		this.createTestItemStacks().forEach(itemStack -> {
			@Nullable UnmodifiableItemStack unmodifiableItemStack = UnmodifiableItemStack.of(itemStack);
			@Nullable UnmodifiableItemStack unmodifiableItemStack2 = UnmodifiableItemStack.of(itemStack);
			Assert.assertTrue(ItemUtils.isSimilar(unmodifiableItemStack, unmodifiableItemStack2));
		});
	}
}
