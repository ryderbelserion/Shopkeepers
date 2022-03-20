package com.nisovin.shopkeepers.compat;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemMigration;
import com.nisovin.shopkeepers.util.java.TimeUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

/**
 * Tests some of this plugin's assumptions about the server's API implementation.
 * <p>
 * Any assumptions that turn out to be incorrect will cause the plugin to shut down to avoid any
 * damage that could be caused by the plugin not behaving as expected.
 * <p>
 * These tests should ideally be lightweight to not delay the server startup for too long.
 */
public class ServerAssumptionsTest {

	private static @Nullable Boolean result = null; // Null if the tests were not yet run

	/**
	 * Runs the server assumption tests.
	 * <p>
	 * The tests are only run once, during the initial plugin startup. Any subsequent invocations
	 * (e.g. during subsequent soft plugin reloads) return the result of the previous test
	 * execution.
	 * 
	 * @return <code>true</code> if the tests passed, <code>false</code> if one of the tests failed
	 */
	public static boolean run() {
		if (result != null) {
			Log.debug("Skipping already run server assumption tests.");
			return Unsafe.assertNonNull(result);
		}

		long startTime = System.nanoTime();
		Log.debug("Testing server assumptions ...");

		try {
			ServerAssumptionsTest test = new ServerAssumptionsTest();
			test.runAll();

			// Tests passed:
			result = true;
			double durationMillis = TimeUtils.convert(
					System.nanoTime() - startTime,
					TimeUnit.NANOSECONDS,
					TimeUnit.MILLISECONDS
			);
			Log.debug(() -> "Server assumption tests passed (" + TextUtils.format(durationMillis)
					+ " ms).");
		} catch (Exception e) {
			result = false;
			Log.severe("Server assumption test failed: " + e.getMessage());
		}
		return Unsafe.assertNonNull(result);
	}

	/**
	 * This exception is thrown when one of the tested server assumptions is detected to be
	 * incorrect.
	 */
	private static class ServerAssumptionFailedException extends Exception {

		private static final long serialVersionUID = 8174065546288633858L;

		/**
		 * Creates a new {@link ServerAssumptionFailedException}.
		 * 
		 * @param message
		 *            the exception message, not <code>null</code> or empty
		 */
		public ServerAssumptionFailedException(String message) {
			super(Validate.notEmpty(message, "message is null or empty"));
		}
	}

	private static void assumption(
			boolean expression,
			String errorMessage
	) throws ServerAssumptionFailedException {
		if (!expression) {
			throw new ServerAssumptionFailedException(errorMessage);
		}
	}

	private final ItemStack bukkitItemStack;
	private final ItemStack bukkitItemStack2;
	private final ItemStack craftItemStack;
	private final ItemStack craftItemStack2;

	private final ItemStack deserializedItemStack;
	private final ItemStack deserializedCraftItemStack;

	private ServerAssumptionsTest() {
		// Setup:
		this.bukkitItemStack = createComplexItemStack();
		this.bukkitItemStack2 = createComplexItemStack();

		this.craftItemStack = toCraftItemStackCopy(bukkitItemStack);
		this.craftItemStack2 = toCraftItemStackCopy(bukkitItemStack2);

		this.deserializedItemStack = deserialize(bukkitItemStack);
		this.deserializedCraftItemStack = toCraftItemStackCopy(deserializedItemStack);
	}

	private static ItemStack createComplexItemStack() {
		ItemStack itemStack = new ItemStack(Material.BOW);
		ItemMeta itemMeta = Unsafe.assertNonNull(itemStack.getItemMeta());
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemMeta.setLore(Arrays.asList(ChatColor.GREEN + "lore1", "lore2"));
		itemMeta.addEnchant(Unsafe.assertNonNull(Enchantment.PIERCING), 1, true);
		itemMeta.addEnchant(Unsafe.assertNonNull(Enchantment.ARROW_DAMAGE), 1, true);
		itemMeta.addEnchant(Unsafe.assertNonNull(Enchantment.DAMAGE_ARTHROPODS), 1, true);
		itemMeta.setCustomModelData(1);
		itemMeta.setUnbreakable(true);
		itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		itemMeta.addAttributeModifier(
				Attribute.GENERIC_ATTACK_SPEED,
				new AttributeModifier(
						new UUID(1L, 1L),
						"attack speed bonus",
						2,
						Operation.ADD_NUMBER,
						EquipmentSlot.HAND
				)
		);
		itemMeta.addAttributeModifier(
				Attribute.GENERIC_ATTACK_SPEED,
				new AttributeModifier(
						new UUID(2L, 2L),
						"attack speed bonus 2",
						0.5,
						Operation.MULTIPLY_SCALAR_1,
						EquipmentSlot.OFF_HAND
				)
		);
		itemMeta.addAttributeModifier(
				Attribute.GENERIC_MAX_HEALTH,
				new AttributeModifier(new UUID(3L, 3L),
						"attack speed bonus",
						2,
						Operation.ADD_NUMBER,
						EquipmentSlot.HAND
				)
		);
		((Damageable) itemMeta).setDamage(2);
		((Repairable) itemMeta).setRepairCost(3);
		PersistentDataContainer customTags = itemMeta.getPersistentDataContainer();
		customTags.set(
				NamespacedKeyUtils.create("some_plugin", "some-key"),
				Unsafe.assertNonNull(PersistentDataType.STRING),
				"some value"
		);
		PersistentDataContainer customContainer = customTags.getAdapterContext().newPersistentDataContainer();
		customContainer.set(
				NamespacedKeyUtils.create("inner_plugin", "inner-key"),
				Unsafe.assertNonNull(PersistentDataType.FLOAT),
				0.3F
		);
		customTags.set(
				NamespacedKeyUtils.create("some_plugin", "some-other-key"),
				Unsafe.assertNonNull(PersistentDataType.TAG_CONTAINER),
				customContainer
		);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	private static ItemStack toCraftItemStackCopy(ItemStack itemStack) {
		return ItemMigration.migrateNonNullItemStack(itemStack);
	}

	private static ItemStack deserialize(ItemStack itemStack) {
		String serialized = ConfigUtils.toConfigYaml("item", itemStack);
		ItemStack deserialized = ConfigUtils.fromConfigYaml(serialized, "item");
		return Validate.State.notNull(
				deserialized,
				() -> "Deserialized ItemStack is null! Original: " + itemStack
		);
	}

	// TESTS

	/**
	 * Runs all tests.
	 * 
	 * @throws ServerAssumptionFailedException
	 *             if one of the tests failed
	 */
	private void runAll() throws ServerAssumptionFailedException {
		this.testItemStackComparisons();
		this.testDeserializedItemStackComparisons();
		this.testInventoryItems();
	}

	private void testItemStackComparisons() throws ServerAssumptionFailedException {
		// Item stacks are similar to themselves:
		assumption(
				bukkitItemStack.isSimilar(bukkitItemStack),
				"Bukkit ItemStack#isSimilar(self)"
		);
		assumption(
				craftItemStack.isSimilar(craftItemStack),
				"CraftItemStack#isSimilar(self)"
		);

		// Item stacks are equal to themselves:
		assumption(
				bukkitItemStack.equals(bukkitItemStack),
				"Bukkit ItemStack#equals(self)"
		);
		assumption(
				craftItemStack.equals(craftItemStack),
				"CraftItemStack#equals(self)"
		);

		// Item stacks are similar to each other:
		assumption(
				bukkitItemStack.isSimilar(bukkitItemStack2),
				"Bukkit ItemStack#isSimilar(other Bukkit ItemStack)"
		);
		assumption(
				bukkitItemStack.isSimilar(craftItemStack),
				"Bukkit ItemStack#isSimilar(CraftItemStack)"
		);
		assumption(
				craftItemStack.isSimilar(craftItemStack2),
				"CraftItemStack#isSimilar(other CraftItemStack)"
		);
		assumption(
				craftItemStack.isSimilar(bukkitItemStack),
				"CraftItemStack#isSimilar(Bukkit ItemStack)"
		);

		// Item stacks are equal to each other:
		assumption(
				bukkitItemStack.equals(bukkitItemStack2),
				"Bukkit ItemStack#equals(other Bukkit ItemStack)"
		);
		assumption(
				bukkitItemStack.equals(craftItemStack),
				"Bukkit ItemStack#equals(CraftItemStack)"
		);
		assumption(
				craftItemStack.equals(craftItemStack2),
				"CraftItemStack#equals(other CraftItemStack)"
		);
		assumption(
				craftItemStack.equals(bukkitItemStack),
				"CraftItemStack#equals(Bukkit ItemStack)"
		);
	}

	private void testDeserializedItemStackComparisons() throws ServerAssumptionFailedException {
		// Item stacks are similar to deserialized item stack:
		assumption(
				bukkitItemStack.isSimilar(deserializedItemStack),
				"Bukkit ItemStack#isSimilar(Deserialized ItemStack)"
		);
		assumption(
				craftItemStack.isSimilar(deserializedItemStack),
				"CraftItemStack#isSimilar(Deserialized ItemStack)"
		);

		// Item stacks are similar to deserialized CraftItemStack:
		assumption(
				bukkitItemStack.isSimilar(deserializedCraftItemStack),
				"Bukkit ItemStack#isSimilar(Deserialized CraftItemStack)"
		);
		assumption(
				craftItemStack.isSimilar(deserializedCraftItemStack),
				"CraftItemStack#isSimilar(Deserialized CraftItemStack)"
		);

		// Deserialized item stack is similar to item stacks:
		assumption(
				deserializedItemStack.isSimilar(bukkitItemStack),
				"Deserialized ItemStack#isSimilar(Bukkit ItemStack)"
		);
		assumption(
				deserializedItemStack.isSimilar(craftItemStack),
				"Deserialized ItemStack#isSimilar(CraftItemStack)"
		);

		// Deserialized CraftItemStack is similar to item stacks:
		assumption(
				deserializedCraftItemStack.isSimilar(bukkitItemStack),
				"Deserialized CraftItemStack#isSimilar(Bukkit ItemStack)"
		);
		assumption(
				deserializedCraftItemStack.isSimilar(craftItemStack),
				"Deserialized CraftItemStack#isSimilar(CraftItemStack)"
		);
	}

	private void testInventoryItems() throws ServerAssumptionFailedException {
		Inventory inventory = Bukkit.createInventory(null, 9);
		ItemStack bukkitItemStack = new ItemStack(Material.STONE, 1);

		// Inventory#setItem copies the item stack:
		inventory.setItem(0, bukkitItemStack);
		bukkitItemStack.setAmount(2);
		ItemStack fromInventory = Unsafe.assertNonNull(inventory.getItem(0));
		assumption(
				fromInventory.getAmount() == 1,
				"Inventory#setItem did not copy the ItemStack!"
		);

		// Inventory#getItem returns a live wrapper of the underlying item stack:
		fromInventory.setAmount(3);
		ItemStack fromInventory2 = Unsafe.assertNonNull(inventory.getItem(0));
		assumption(
				fromInventory2.getAmount() == 3,
				"Inventory#getItem did not return a live wrapper of the inventory's ItemStack!"
		);
	}
}
