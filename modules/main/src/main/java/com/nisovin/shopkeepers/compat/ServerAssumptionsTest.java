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
 * Any assumptions that turn out to be incorrect will cause the plugin to shut down to avoid any damage that could be
 * caused by the plugin not behaving as expected.
 * <p>
 * These tests should ideally be lightweight to not delay the server startup for too long.
 */
public class ServerAssumptionsTest {

	private static Boolean result = null; // Null if the tests were not yet run

	/**
	 * Runs the server assumption tests.
	 * <p>
	 * The tests are only run once, during the initial plugin startup. Any subsequent invocations (e.g. during
	 * subsequent soft plugin reloads) return the result of the previous test execution.
	 * 
	 * @return <code>true</code> if the tests passed, <code>false</code> if one of the tests failed
	 */
	public static boolean run() {
		if (result != null) {
			Log.debug("Skipping already run server assumption tests.");
			return result;
		}

		long startTime = System.nanoTime();
		Log.debug("Testing server assumptions ...");

		try {
			ServerAssumptionsTest test = new ServerAssumptionsTest();
			test.runAll();

			// Tests passed:
			result = true;
			double durationMillis = TimeUtils.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS, TimeUnit.MILLISECONDS);
			Log.debug(() -> "Server assumption tests passed (" + TextUtils.DECIMAL_FORMAT.format(durationMillis) + " ms).");
		} catch (ServerAssumptionFailedException e) {
			result = false;
			Log.severe("Server assumption test failed: " + e.getMessage());
		}
		return result;
	}

	/**
	 * This exception is thrown when one of the tested server assumptions is detected to be incorrect.
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

	private static void assumption(boolean expression, String errorMessage) throws ServerAssumptionFailedException {
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
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemMeta.setLore(Arrays.asList(ChatColor.GREEN + "lore1", "lore2"));
		itemMeta.addEnchant(Enchantment.PIERCING, 1, true);
		itemMeta.addEnchant(Enchantment.ARROW_DAMAGE, 1, true);
		itemMeta.addEnchant(Enchantment.DAMAGE_ARTHROPODS, 1, true);
		itemMeta.setCustomModelData(1);
		itemMeta.setUnbreakable(true);
		itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(new UUID(1L, 1L), "attack speed bonus", 2, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(new UUID(2L, 2L), "attack speed bonus 2", 0.5, Operation.MULTIPLY_SCALAR_1, EquipmentSlot.OFF_HAND));
		itemMeta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(new UUID(3L, 3L), "attack speed bonus", 2, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		((Damageable) itemMeta).setDamage(2);
		((Repairable) itemMeta).setRepairCost(3);
		PersistentDataContainer customTags = itemMeta.getPersistentDataContainer();
		customTags.set(NamespacedKeyUtils.create("some_plugin", "some-key"), PersistentDataType.STRING, "some value");
		PersistentDataContainer customContainer = customTags.getAdapterContext().newPersistentDataContainer();
		customContainer.set(NamespacedKeyUtils.create("inner_plugin", "inner-key"), PersistentDataType.FLOAT, 0.3F);
		customTags.set(NamespacedKeyUtils.create("some_plugin", "some-other-key"), PersistentDataType.TAG_CONTAINER, customContainer);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	private static ItemStack toCraftItemStackCopy(ItemStack itemStack) {
		return ItemMigration.migrateItemStack(itemStack);
	}

	private static ItemStack deserialize(ItemStack itemStack) {
		String serialized = ConfigUtils.toConfigYaml("item", itemStack);
		return ConfigUtils.fromConfigYaml(serialized, "item");
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
		if (!bukkitItemStack.isSimilar(bukkitItemStack)) {
			throw new ServerAssumptionFailedException("Bukkit ItemStack#isSimilar(self)");
		}
		if (!craftItemStack.isSimilar(craftItemStack)) {
			throw new ServerAssumptionFailedException("CraftItemStack#isSimilar(self)");
		}

		// Item stacks are equal to themselves:
		if (!bukkitItemStack.equals(bukkitItemStack)) {
			throw new ServerAssumptionFailedException("Bukkit ItemStack#equals(self)");
		}
		if (!craftItemStack.equals(craftItemStack)) {
			throw new ServerAssumptionFailedException("CraftItemStack#equals(self)");
		}

		// Item stacks are similar to each other:
		if (!bukkitItemStack.isSimilar(bukkitItemStack2)) {
			throw new ServerAssumptionFailedException("Bukkit ItemStack#isSimilar(other Bukkit ItemStack)");
		}
		if (!bukkitItemStack.isSimilar(craftItemStack)) {
			throw new ServerAssumptionFailedException("Bukkit ItemStack#isSimilar(CraftItemStack)");
		}
		if (!craftItemStack.isSimilar(craftItemStack2)) {
			throw new ServerAssumptionFailedException("CraftItemStack#isSimilar(other CraftItemStack)");
		}
		if (!craftItemStack.isSimilar(bukkitItemStack)) {
			throw new ServerAssumptionFailedException("CraftItemStack#isSimilar(Bukkit ItemStack)");
		}

		// Item stacks are equal to each other:
		if (!bukkitItemStack.equals(bukkitItemStack2)) {
			throw new ServerAssumptionFailedException("Bukkit ItemStack#equals(other Bukkit ItemStack)");
		}
		if (!bukkitItemStack.equals(craftItemStack)) {
			throw new ServerAssumptionFailedException("Bukkit ItemStack#equals(CraftItemStack)");
		}
		if (!craftItemStack.equals(craftItemStack2)) {
			throw new ServerAssumptionFailedException("CraftItemStack#equals(other CraftItemStack)");
		}
		if (!craftItemStack.equals(bukkitItemStack)) {
			throw new ServerAssumptionFailedException("CraftItemStack#equals(Bukkit ItemStack)");
		}
	}

	private void testDeserializedItemStackComparisons() throws ServerAssumptionFailedException {
		// Item stacks are similar to deserialized item stack:
		if (!bukkitItemStack.isSimilar(deserializedItemStack)) {
			throw new ServerAssumptionFailedException("Bukkit ItemStack#isSimilar(Deserialized ItemStack)");
		}
		if (!craftItemStack.isSimilar(deserializedItemStack)) {
			throw new ServerAssumptionFailedException("CraftItemStack#isSimilar(Deserialized ItemStack)");
		}

		// Item stacks are similar to deserialized CraftItemStack:
		if (!bukkitItemStack.isSimilar(deserializedCraftItemStack)) {
			throw new ServerAssumptionFailedException("Bukkit ItemStack#isSimilar(Deserialized CraftItemStack)");
		}
		if (!craftItemStack.isSimilar(deserializedCraftItemStack)) {
			throw new ServerAssumptionFailedException("CraftItemStack#isSimilar(Deserialized CraftItemStack)");
		}

		// Deserialized item stack is similar to item stacks:
		if (!deserializedItemStack.isSimilar(bukkitItemStack)) {
			throw new ServerAssumptionFailedException("Deserialized ItemStack#isSimilar(Bukkit ItemStack)");
		}
		if (!deserializedItemStack.isSimilar(craftItemStack)) {
			throw new ServerAssumptionFailedException("Deserialized ItemStack#isSimilar(CraftItemStack)");
		}

		// Deserialized CraftItemStack is similar to item stacks:
		if (!deserializedCraftItemStack.isSimilar(bukkitItemStack)) {
			throw new ServerAssumptionFailedException("Deserialized CraftItemStack#isSimilar(Bukkit ItemStack)");
		}
		if (!deserializedCraftItemStack.isSimilar(craftItemStack)) {
			throw new ServerAssumptionFailedException("Deserialized CraftItemStack#isSimilar(CraftItemStack)");
		}
	}

	private void testInventoryItems() throws ServerAssumptionFailedException {
		Inventory inventory = Bukkit.createInventory(null, 9);
		ItemStack bukkitItemStack = new ItemStack(Material.STONE, 1);

		// Inventory#setItem copies the item stack:
		inventory.setItem(0, bukkitItemStack);
		bukkitItemStack.setAmount(2);
		ItemStack fromInventory = inventory.getItem(0);
		assumption(fromInventory.getAmount() == 1, "Inventory#setItem did not copy the ItemStack!");

		// Inventory#getItem returns a live wrapper of the underlying item stack:
		fromInventory.setAmount(3);
		ItemStack fromInventory2 = inventory.getItem(0);
		assumption(fromInventory2.getAmount() == 3, "Inventory#getItem did not return a live wrapper of the inventory's ItemStack!");
	}
}
