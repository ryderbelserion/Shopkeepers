package com.nisovin.shopkeepers.util;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.block.data.type.Campfire;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.Repairable;
import org.bukkit.inventory.meta.WritableBookMeta;
import org.bukkit.inventory.meta.components.FoodComponent;
import org.bukkit.inventory.meta.components.ToolComponent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * ItemStack definitions for test cases.
 */
public class TestItemStacks {

	// Note: SPIGOT-7571: Use Json-based display names and lore (e.g. via
	// ItemUtils#setDisplayNameAndLore) instead of ItemMeta#setDisplayName / #setLore.

	public static List<? extends @Nullable ItemStack> createAllItemStacks() {
		return Arrays.asList(
				createItemStackNull(),
				createItemStackAir(),
				createItemStackBasic(),
				createItemStackBasicWithSize(),
				createItemStackBasicTool(),
				createItemStackDisplayName(),
				createItemStackComplete(),
				createItemStackBlockData(),
				createItemStackUncommonMeta(),
				createItemStackWritableBook(),
				createItemStackWrittenBook(),
				createItemStackTileEntityDisplayName(),
				createItemStackBasicTileEntity()
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
		ItemUtils.setDisplayNameAndLore(itemStack, "{\"text\":\"Custom Name\",\"color\":\"red\"}", null);
		return itemStack;
	}

	public static ItemStack createItemStackComplete() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		ItemUtils.setDisplayNameAndLore(
				itemStack,
				"{\"text\":\"Custom Name\",\"color\":\"red\"}",
				Arrays.asList("{\"text\":\"lore1\",\"color\":\"green\"}", "lore2")
		);
		ItemUtils.setItemName(itemStack, "{\"text\":\"Custom item name\",\"color\":\"red\"}");

		ItemMeta itemMeta = Unsafe.assertNonNull(itemStack.getItemMeta());
		itemMeta.setMaxStackSize(65);
		itemMeta.setRarity(ItemRarity.EPIC);
		itemMeta.setHideTooltip(true);
		itemMeta.setCustomModelData(1);
		itemMeta.setFireResistant(true);
		itemMeta.setUnbreakable(true);
		((Damageable) itemMeta).setDamage(2);
		((Damageable) itemMeta).setMaxDamage(10);
		((Repairable) itemMeta).setRepairCost(3);

		ToolComponent tool = itemMeta.getTool();
		tool.setDefaultMiningSpeed(1.5f);
		tool.setDamagePerBlock(2);
		tool.addRule(Material.STONE, 0.5f, true);
		itemMeta.setTool(tool);

		itemMeta.setEnchantmentGlintOverride(true);
		itemMeta.addEnchant(Enchantment.UNBREAKING, 1, true);
		itemMeta.addEnchant(Enchantment.SHARPNESS, 2, true);
		itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
				new AttributeModifier(
						new UUID(1L, 1L),
						"attack speed bonus",
						2,
						Operation.ADD_NUMBER,
						EquipmentSlotGroup.HAND
				)
		);
		itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED,
				new AttributeModifier(
						new UUID(2L, 2L),
						"attack speed bonus 2",
						0.5,
						Operation.MULTIPLY_SCALAR_1,
						EquipmentSlotGroup.OFFHAND
				)
		);
		itemMeta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH,
				new AttributeModifier(
						new UUID(3L, 3L),
						"max health bonus",
						2,
						Operation.ADD_NUMBER,
						EquipmentSlotGroup.HAND
				)
		);
		itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

		FoodComponent food = itemMeta.getFood();
		food.setNutrition(2);
		food.setSaturation(2.5f);
		food.setCanAlwaysEat(true);
		food.setEatSeconds(5.5f);
		food.addEffect(new PotionEffect(PotionEffectType.BLINDNESS, 5, 1), 0.5f);
		itemMeta.setFood(food);

		// Note: This data ends up getting stored in an arbitrary order internally.
		PersistentDataContainer customTags = itemMeta.getPersistentDataContainer();
		customTags.set(
				NamespacedKeyUtils.create("some_plugin", "some-key"),
				PersistentDataType.STRING,
				"some value"
		);
		PersistentDataContainer customContainer = customTags.getAdapterContext().newPersistentDataContainer();
		customContainer.set(
				NamespacedKeyUtils.create("inner_plugin", "inner-key"),
				PersistentDataType.FLOAT,
				0.3F
		);
		customTags.set(
				NamespacedKeyUtils.create("some_plugin", "some-other-key"),
				PersistentDataType.TAG_CONTAINER,
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
		itemMeta.setColor(Color.BLUE);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	public static ItemStack createItemStackWritableBook() {
		ItemStack itemStack = new ItemStack(Material.WRITABLE_BOOK);
		WritableBookMeta itemMeta = Unsafe.castNonNull(itemStack.getItemMeta());
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
		ItemUtils.setDisplayNameAndLore(itemStack, "{\"text\":\"Custom Name\",\"color\":\"red\"}", null);
		return itemStack;
	}

	private TestItemStacks() {
	}
}
