package com.nisovin.shopkeepers;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_20_R4.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.util.TestItemStacks;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MutableLong;
import com.nisovin.shopkeepers.util.java.TimeUtils;

import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.component.PatchedDataComponentMap;

public class PerformanceTests extends AbstractBukkitTest {

	private static final Logger LOGGER = Logger.getLogger(PerformanceTests.class.getCanonicalName());

	public static void testPerformance(
			String outputPrefix,
			String testName,
			int warmupCount,
			int testCount,
			Runnable function
	) {
		ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
		boolean cpuTimeSupported = threadMXBean.isCurrentThreadCpuTimeSupported();
		if (!cpuTimeSupported) {
			LOGGER.info(outputPrefix + "Note: Thread CPU time not supported!");
		}

		// warm up:
		for (int i = 0; i < warmupCount; ++i) {
			function.run();
		}

		long startNanos = System.nanoTime();
		long cpuStartNanos = 0;
		if (cpuTimeSupported) {
			cpuStartNanos = threadMXBean.getCurrentThreadCpuTime();
		}
		for (int i = 0; i < testCount; ++i) {
			function.run();
		}
		long cpuDurationNanos = 0;
		if (cpuTimeSupported) {
			cpuDurationNanos = threadMXBean.getCurrentThreadCpuTime() - cpuStartNanos;
		}
		double cpuDurationMillis = TimeUtils.convert(
				cpuDurationNanos,
				TimeUnit.NANOSECONDS,
				TimeUnit.MILLISECONDS
		);
		double durationMillis = TimeUtils.convert(
				System.nanoTime() - startNanos,
				TimeUnit.NANOSECONDS,
				TimeUnit.MILLISECONDS
		);
		LOGGER.info(outputPrefix + "Duration of '" + testName + "' (" + testCount + " runs): "
				+ durationMillis + " ms (CPU time: " + cpuDurationMillis + " ms)");
	}

	@Test
	public void testCreateItemStackPerformance() {
		LOGGER.info("Testing ItemStack creation performance:");
		int warmupCount = 10000;
		int testCount = 1000000;
		ItemStack itemStack = TestItemStacks.createItemStackComplete();
		ItemData itemData = new ItemData(itemStack);

		testPerformance(
				"  ",
				"ItemData#createItemStack",
				warmupCount,
				testCount,
				itemData::createItemStack
		);

		testPerformance(
				"  ",
				"ItemStack#clone()",
				warmupCount,
				testCount,
				itemStack::clone
		);

		CraftItemStack craftItemStack = CraftItemStack.asCraftCopy(itemStack);
		testPerformance(
				"  ",
				"CraftItemStack#clone()",
				warmupCount,
				testCount,
				craftItemStack::clone
		);
	}

	@Test
	public void testIsSimilarPerformance() {
		LOGGER.info("Testing ItemStack isSimilar performance:");
		int warmupCount = 10000;
		int testCount = 1000000;
		ItemStack itemStack = TestItemStacks.createItemStackComplete();
		ItemStack itemStackCopy = itemStack.clone();
		CraftItemStack craftItemStack = CraftItemStack.asCraftCopy(itemStack);
		CraftItemStack craftItemStackCopy = craftItemStack.clone();

		testPerformance(
				"  ",
				"ItemStack#isSimilar(ItemStack)",
				warmupCount,
				testCount,
				() -> {
					itemStack.isSimilar(itemStackCopy);
				}
		);

		testPerformance(
				"  ",
				"ItemStack#isSimilar(CraftItemStack)",
				warmupCount,
				testCount,
				() -> {
					itemStack.isSimilar(craftItemStack);
				}
		);

		testPerformance(
				"  ",
				"CraftItemStack#isSimilar(ItemStack)",
				warmupCount,
				testCount,
				() -> {
					craftItemStack.isSimilar(itemStack);
				}
		);

		testPerformance(
				"  ",
				"CraftItemStack#isSimilar(CraftItemStack)",
				warmupCount,
				testCount,
				() -> {
					craftItemStack.isSimilar(craftItemStackCopy);
				}
		);
	}

	@Test
	public void testMatchesPerformance() {
		LOGGER.info("Testing ItemStack matching performance:");
		int warmupCount = 10000;
		int testCount = 1000000;
		ItemStack itemStack = TestItemStacks.createItemStackComplete();
		ItemData itemData = new ItemData(itemStack);
		Material type = itemStack.getType();
		ItemMeta itemMeta = Unsafe.assertNonNull(itemStack.getItemMeta());
		String displayName = itemMeta.getDisplayName();
		List<? extends String> lore = Unsafe.castNonNull(itemMeta.getLore());
		CraftItemStack craftItemStack = CraftItemStack.asCraftCopy(itemStack);
		var componentsPatch = CraftItemStack.asNMSCopy(itemStack).getComponentsPatch();
		var componentsPatchCopy = CraftItemStack.asNMSCopy(itemStack).getComponentsPatch();
		var componentPredicate = DataComponentPredicate.allOf(PatchedDataComponentMap.fromPatch(
				DataComponentMap.EMPTY,
				CraftItemStack.asNMSCopy(itemStack).getComponentsPatch()
		));
		// Components: Also includes unspecified default components.
		var components = CraftItemStack.asNMSCopy(itemStack).getComponents();
		var componentsCopy = CraftItemStack.asNMSCopy(itemStack).getComponents();

		testPerformance(
				"  ",
				"comparing name and lore",
				warmupCount,
				testCount,
				() -> {
					ItemUtils.isSimilar(itemStack, type, displayName, lore);
				}
		);

		testPerformance(
				"  ",
				"ItemData#matches(ItemStack)",
				warmupCount,
				testCount,
				() -> {
					itemData.matches(itemStack);
				}
		);

		testPerformance(
				"  ",
				"ItemData#matches(CraftItemStack)",
				warmupCount,
				testCount,
				() -> {
					itemData.matches(craftItemStack);
				}
		);

		testPerformance(
				"  ",
				"matching component patches",
				warmupCount,
				testCount,
				() -> componentsPatch.equals(componentsPatchCopy)
		);

		testPerformance(
				"  ",
				"matching components",
				warmupCount,
				testCount,
				() -> components.equals(componentsCopy)
		);

		testPerformance(
				"  ",
				"matching components with component predicate",
				warmupCount,
				testCount,
				() -> componentPredicate.test(components)
		);

		testPerformance(
				"  ",
				"matching CraftItemStack with component predicate",
				warmupCount,
				testCount,
				() -> componentPredicate.test(CraftItemStack.asNMSCopy(craftItemStack))
		);

		testPerformance(
				"  ",
				"matching ItemStack with component predicate",
				warmupCount,
				testCount,
				() -> componentPredicate.test(CraftItemStack.asNMSCopy(itemStack))
		);

		testPerformance(
				"  ",
				"matching ItemStack component patches with component predicate",
				warmupCount,
				testCount,
				() -> {
					DataComponentPredicate.allOf(PatchedDataComponentMap.fromPatch(
							DataComponentMap.EMPTY,
							CraftItemStack.asNMSCopy(itemStack).getComponentsPatch()
					)).test(CraftItemStack.asNMSCopy(itemStack));
				}
		);

		testPerformance(
				"  ",
				"matching CraftItemStack component patches with component predicate",
				warmupCount,
				testCount,
				() -> {
					DataComponentPredicate.allOf(PatchedDataComponentMap.fromPatch(
							DataComponentMap.EMPTY,
							CraftItemStack.asNMSCopy(craftItemStack).getComponentsPatch()
					)).test(CraftItemStack.asNMSCopy(craftItemStack));
				}
		);
	}

	@Test
	public void testCraftItemStackReflectiveHandleVsCopyPerformance() throws Exception {
		LOGGER.info("Testing reflective CraftItemStack.handle access vs asNMSCopy performance:");
		int warmupCount = 10000;
		int testCount = 10000000;

		Field craftItemStackHandleField = CraftItemStack.class.getDeclaredField("handle");
		craftItemStackHandleField.setAccessible(true);

		ItemStack fullItemStack = TestItemStacks.createItemStackComplete();
		ItemStack basicItemStack = TestItemStacks.createItemStackBasic();
		CraftItemStack fullCraftItemStack = CraftItemStack.asCraftCopy(fullItemStack);
		CraftItemStack basicCraftItemStack = CraftItemStack.asCraftCopy(basicItemStack);

		// In order to avoid that the compiler optimizes these operations away, we increment this
		// value during the
		// tests:
		MutableLong value = new MutableLong();

		testPerformance(
				"  ",
				"full reflective CraftItemStack.handle access",
				warmupCount,
				testCount,
				() -> {
					try {
						net.minecraft.world.item.ItemStack nmsItem = Unsafe.cast(
								craftItemStackHandleField.get(fullCraftItemStack)
						);
						if (nmsItem != null) {
							value.increment(1);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
		);

		testPerformance(
				"  ",
				"full CraftItemStack asNMSCopy",
				warmupCount,
				testCount,
				() -> {
					net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(
							fullCraftItemStack
					);
					if (nmsItem != null) {
						value.increment(1);
					}
				}
		);

		testPerformance(
				"  ",
				"full ItemStack asNMSCopy",
				warmupCount,
				testCount,
				() -> {
					net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(
							fullItemStack
					);
					if (nmsItem != null) {
						value.increment(1);
					}
				}
		);

		testPerformance(
				"  ",
				"basic reflective CraftItemStack.handle access",
				warmupCount,
				testCount,
				() -> {
					try {
						net.minecraft.world.item.ItemStack nmsItem = Unsafe.cast(
								craftItemStackHandleField.get(basicCraftItemStack)
						);
						if (nmsItem != null) {
							value.increment(1);
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
		);

		testPerformance(
				"  ",
				"basic CraftItemStack asNMSCopy",
				warmupCount,
				testCount,
				() -> {
					net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(
							basicCraftItemStack
					);
					if (nmsItem != null) {
						value.increment(1);
					}
				}
		);

		testPerformance(
				"  ",
				"basic ItemStack asNMSCopy",
				warmupCount,
				testCount,
				() -> {
					net.minecraft.world.item.ItemStack nmsItem = CraftItemStack.asNMSCopy(
							basicItemStack
					);
					if (nmsItem != null) {
						value.increment(1);
					}
				}
		);

		if (value.getValue() == 0) {
			throw new IllegalStateException("Unexpected test outcome.");
		}
	}
}
