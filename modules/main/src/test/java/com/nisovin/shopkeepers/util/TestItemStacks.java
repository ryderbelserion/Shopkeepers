package com.nisovin.shopkeepers.util;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;

/**
 * ItemStack definitions for test cases.
 */
public class TestItemStacks {

	public static List<? extends @Nullable ItemStack> createAllItemStacks() {
		return Arrays.asList(
				createItemStackNull(),
				createItemStackAir(),
				createItemStackBasic(),
				createItemStackBasicWithSize(),
				createItemStackBasicTool(),
				createItemStackDisplayName(),
				createItemStackComplete(),
				// TODO This might be broken in Bukkit: Serializing and deserializing this item will
				// produce non-equal ItemStacks with different BlockData (false vs 0b). See
				// https://hub.spigotmc.org/jira/browse/SPIGOT-6257
				// createItemStackBlockData(),
				createItemStackUncommonMeta(),
				createItemStackWritableBook(),
				createItemStackWrittenBook(),
				createItemStackBasicTileEntity(),
				createItemStackTileEntityDisplayName()
		);
	}

	public static @Nullable ItemStack createItemStackNull() {
		return null;
	}

	public static ItemStack createItemStackAir() {
		return new ItemStack(Material.AIR);
	}

	public static ItemStack createItemStackBasic() {
		return new ItemStack(Material.STONE);
	}

	public static ItemStack createItemStackBasicWithSize() {
		return new ItemStack(Material.STONE, 10);
	}

	public static ItemStack createItemStackBasicTool() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		return itemStack;
	}

	public static ItemStack createItemStackDisplayName() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		ItemMeta itemMeta = Unsafe.assertNonNull(itemStack.getItemMeta());
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackComplete() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		ItemMeta itemMeta = Unsafe.assertNonNull(itemStack.getItemMeta());
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemMeta.setLore(Arrays.asList(ChatColor.GREEN + "lore1", "lore2"));
		itemMeta.addEnchant(Unsafe.assertNonNull(Enchantment.DURABILITY), 1, true);
		itemMeta.addEnchant(Unsafe.assertNonNull(Enchantment.DAMAGE_ALL), 2, true);
		itemMeta.setCustomModelData(1);
		itemMeta.setLocalizedName("loc name");
		itemMeta.setUnbreakable(true);
		itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
				new AttributeModifier(
						new UUID(1L, 1L),
						"attack speed bonus",
						2,
						Operation.ADD_NUMBER,
						EquipmentSlot.HAND
				)
		);
		itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
				new AttributeModifier(
						new UUID(2L, 2L),
						"attack speed bonus 2",
						0.5,
						Operation.MULTIPLY_SCALAR_1,
						EquipmentSlot.OFF_HAND
				)
		);
		itemMeta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH,
				new AttributeModifier(
						new UUID(3L, 3L),
						"attack speed bonus",
						2,
						Operation.ADD_NUMBER,
						EquipmentSlot.HAND
				)
		);
		((Damageable) itemMeta).setDamage(2);
		((Repairable) itemMeta).setRepairCost(3);
		// Note: This data ends up getting stored in an arbitrary order internally.
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

	public static ItemStack createItemStackBlockData() {
		ItemStack itemStack = new ItemStack(Material.CAMPFIRE);
		BlockDataMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		Campfire blockData = (Campfire) Material.CAMPFIRE.createBlockData();
		blockData.setLit(false);
		itemMeta.setBlockData(blockData);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackUncommonMeta() {
		ItemStack itemStack = new ItemStack(Material.LEATHER_CHESTPLATE);
		LeatherArmorMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemMeta.setColor(Color.BLUE);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackWritableBook() {
		ItemStack itemStack = new ItemStack(Material.WRITABLE_BOOK);
		// TODO MC 1.20.5: Returns WritableBookMeta without the option to set title, author,
		// generation.
		BookMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemMeta.setTitle("Finding Diamonds");
		itemMeta.setAuthor("D. Whining Rod");
		itemMeta.setGeneration(Generation.COPY_OF_ORIGINAL);
		itemMeta.setPages(
				"Page 1\nWith empty lines\n\nAnd literal newline \\n and different kinds of quotes like ' and \"!",
				"Page2\n  With multiple lines and whitespace\nAnd §ccolors &a!"
		);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackWrittenBook() {
		ItemStack itemStack = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemMeta.setTitle("Finding Diamonds");
		itemMeta.setAuthor("D. Whining Rod");
		itemMeta.setGeneration(Generation.COPY_OF_ORIGINAL);
		itemMeta.setPages(
				"Page 1\nWith empty lines\n\nAnd literal newline \\n and different kinds of quotes like ' and \"!",
				"Page2\n  With multiple lines and whitespace\nAnd §ccolors &a!"
		);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackBasicTileEntity() {
		ItemStack itemStack = new ItemStack(Material.CHEST);
		return itemStack;
	}

	public static ItemStack createItemStackTileEntityDisplayName() {
		ItemStack itemStack = new ItemStack(Material.CHEST);
		ItemMeta itemMeta = Unsafe.assertNonNull(itemStack.getItemMeta());
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	private TestItemStacks() {
	}
}
