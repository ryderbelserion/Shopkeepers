package com.nisovin.shopkeepers;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_14_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;

import com.nisovin.shopkeepers.testutil.AbstractTestBase;
import com.nisovin.shopkeepers.util.ItemData;
import com.nisovin.shopkeepers.util.ItemDataTest;
import com.nisovin.shopkeepers.util.ItemUtils;

import net.minecraft.server.v1_14_R1.GameProfileSerializer;
import net.minecraft.server.v1_14_R1.NBTTagCompound;

public class PerformanceTests extends AbstractTestBase {

	public static void testPerformance(String outputPrefix, String testName, int warmupCount, int testCount, Runnable function) {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		boolean cpuTimeSupported = threadMXBean.isCurrentThreadCpuTimeSupported();
		if (!cpuTimeSupported) {
			System.out.println(outputPrefix + "Note: Thread cpu time not supported!");
		}

		// warm up:
		for (int i = 0; i < warmupCount; ++i) {
			function.run();
		}

		long start = System.nanoTime();
		long cpuTimestart = (cpuTimeSupported ? threadMXBean.getCurrentThreadCpuTime() : 0);
		for (int i = 0; i < testCount; ++i) {
			function.run();
		}
		long cpuTimeDuration = (cpuTimeSupported ? threadMXBean.getCurrentThreadCpuTime() : 0) - cpuTimestart;
		long duration = System.nanoTime() - start;
		System.out.println(outputPrefix + "Duration of '" + testName + "' (" + testCount + " runs): "
				+ (duration / 1000000.0D) + " ms (CPU time: " + (cpuTimeDuration / 1000000.0D) + " ms)");
	}

	@Test
	public void testCreateItemStackPerformance() {
		System.out.println("Testing ItemStack creation performance:");
		int warmupCount = 10000;
		int testCount = 1000000;
		ItemStack itemStack = ItemDataTest.createItemStackFull();
		ItemData itemData = new ItemData(itemStack);

		testPerformance("  ", "ItemData#createItemStack", warmupCount, testCount, () -> {
			itemData.createItemStack();
		});

		testPerformance("  ", "ItemStack#clone()", warmupCount, testCount, () -> {
			itemStack.clone();
		});

		CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(CraftItemStack.asNMSCopy(itemStack));
		testPerformance("  ", "CraftItemStack#clone()", warmupCount, testCount, () -> {
			craftItemStack.clone();
		});
	}

	@Test
	public void testIsSimilarPerformance() {
		System.out.println("Testing ItemStack isSimilar performance:");
		int warmupCount = 10000;
		int testCount = 1000000;
		ItemStack itemStack = ItemDataTest.createItemStackFull();
		ItemStack itemStackCopy = itemStack.clone();
		CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(CraftItemStack.asNMSCopy(itemStack));
		CraftItemStack craftItemStackCopy = craftItemStack.clone();

		testPerformance("  ", "ItemStack#isSimilar(ItemStack)", warmupCount, testCount, () -> {
			itemStack.isSimilar(itemStackCopy);
		});

		testPerformance("  ", "ItemStack#isSimilar(CraftItemStack)", warmupCount, testCount, () -> {
			itemStack.isSimilar(craftItemStack);
		});

		testPerformance("  ", "CraftItemStack#isSimilar(ItemStack)", warmupCount, testCount, () -> {
			craftItemStack.isSimilar(itemStack);
		});

		testPerformance("  ", "CraftItemStack#isSimilar(CraftItemStack)", warmupCount, testCount, () -> {
			craftItemStack.isSimilar(craftItemStackCopy);
		});
	}

	@Test
	public void testMatchesPerformance() {
		System.out.println("Testing ItemStack matching performance:");
		int warmupCount = 10000;
		int testCount = 1000000;
		ItemStack itemStack = ItemDataTest.createItemStackFull();
		ItemData itemData = new ItemData(itemStack);
		Material type = itemStack.getType();
		String displayName = itemStack.getItemMeta().getDisplayName();
		List<String> lore = itemStack.getItemMeta().getLore();
		CraftItemStack craftItemStack = CraftItemStack.asCraftMirror(CraftItemStack.asNMSCopy(itemStack));
		NBTTagCompound tag = CraftItemStack.asNMSCopy(itemStack).getTag();
		NBTTagCompound tagCopy = tag.clone();

		testPerformance("  ", "comparing name and lore", warmupCount, testCount, () -> {
			ItemUtils.isSimilar(itemStack, type, displayName, lore);
		});

		testPerformance("  ", "ItemData#matches(ItemStack)", warmupCount, testCount, () -> {
			itemData.matches(itemStack);
		});

		testPerformance("  ", "ItemData#matches(CraftItemStack)", warmupCount, testCount, () -> {
			itemData.matches(itemStack);
		});

		testPerformance("  ", "matching NBT tags", warmupCount, testCount, () -> {
			GameProfileSerializer.a(tag, tagCopy, false);
		});

		testPerformance("  ", "matching CraftItemStack with NBT tag", warmupCount, testCount, () -> {
			GameProfileSerializer.a(tag, CraftItemStack.asNMSCopy(craftItemStack).getTag(), false);
		});

		testPerformance("  ", "matching ItemStack with NBT tag", warmupCount, testCount, () -> {
			GameProfileSerializer.a(tag, CraftItemStack.asNMSCopy(itemStack).getTag(), false);
		});

		testPerformance("  ", "matching ItemStack tags", warmupCount, testCount, () -> {
			GameProfileSerializer.a(CraftItemStack.asNMSCopy(itemStack).getTag(), CraftItemStack.asNMSCopy(itemStack).getTag(), false);
		});

		testPerformance("  ", "matching CraftItemStack tags", warmupCount, testCount, () -> {
			GameProfileSerializer.a(CraftItemStack.asNMSCopy(craftItemStack).getTag(), CraftItemStack.asNMSCopy(craftItemStack).getTag(), false);
		});
	}
}
