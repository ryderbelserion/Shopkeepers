package com.nisovin.shopkeepers.util;

import java.util.Arrays;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.testutil.AbstractTestBase;

public class ItemDataTest extends AbstractTestBase {

	private static void testDeserialization(ItemData originalItemData) {
		YamlConfiguration config = new YamlConfiguration();
		Object serialized = originalItemData.serialize();
		config.set("key", serialized);
		String configString = config.saveToString();

		YamlConfiguration newConfig = new YamlConfiguration();
		try {
			newConfig.loadFromString(configString);
		} catch (InvalidConfigurationException e) {
		}
		Object data = newConfig.get("key");
		ItemData deserialized = ItemData.deserialize(data);

		Assert.assertEquals(originalItemData, deserialized);
	}

	// COMPACT

	private static ItemStack createItemStackSimple() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		return itemStack;
	}

	@Test
	public void testSerializationCompact() {
		ItemStack itemStack = createItemStackSimple();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		Assert.assertEquals("DIAMOND_SWORD", serialized.toString());
	}

	@Test
	public void testYAMLSerializationCompact() {
		ItemStack itemStack = createItemStackSimple();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		YamlConfiguration config = new YamlConfiguration();
		config.set("item", serialized);
		String yamlString = config.saveToString();
		Assert.assertEquals("item: DIAMOND_SWORD" + ConfigUtils.yamlLineBreak(), yamlString);
	}

	@Test
	public void testDeserializationSimple() {
		ItemStack itemStack = createItemStackSimple();
		ItemData itemData = new ItemData(itemStack);
		testDeserialization(itemData);
	}

	// MINIMAL

	private static ItemStack createItemStackMinimal() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	@Test
	public void testSerializationMinimal() {
		ItemStack itemStack = createItemStackMinimal();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		Assert.assertEquals("{type=DIAMOND_SWORD, display-name=&cCustom Name}", serialized.toString());
	}

	@Test
	public void testYAMLSerializationMinimal() {
		ItemStack itemStack = createItemStackMinimal();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		YamlConfiguration config = new YamlConfiguration();
		config.set("item", serialized);
		String yamlString = config.saveToString();
		Assert.assertEquals("item:" + ConfigUtils.yamlLineBreak() +
				"  type: DIAMOND_SWORD" + ConfigUtils.yamlLineBreak() +
				"  display-name: '&cCustom Name'" + ConfigUtils.yamlLineBreak(), yamlString);
	}

	@Test
	public void testDeserializationMinimal() {
		ItemStack itemStack = createItemStackMinimal();
		ItemData itemData = new ItemData(itemStack);
		testDeserialization(itemData);
	}

	// FULL

	public static ItemStack createItemStackFull() {
		ItemStack itemStack = new ItemStack(Material.DIAMOND_SWORD);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemMeta.setLore(Arrays.asList(ChatColor.GREEN + "lore1", "lore2"));
		itemMeta.addEnchant(Enchantment.DURABILITY, 1, true);
		itemMeta.addEnchant(Enchantment.DAMAGE_ALL, 2, true);
		itemMeta.setCustomModelData(1);
		itemMeta.setLocalizedName("loc name");
		itemMeta.setUnbreakable(true);
		itemMeta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(new UUID(1L, 1L), "attack speed bonus", 2, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		itemMeta.addAttributeModifier(Attribute.GENERIC_ATTACK_SPEED, new AttributeModifier(new UUID(2L, 2L), "attack speed bonus 2", 0.5, Operation.MULTIPLY_SCALAR_1, EquipmentSlot.OFF_HAND));
		itemMeta.addAttributeModifier(Attribute.GENERIC_MAX_HEALTH, new AttributeModifier(new UUID(3L, 3L), "attack speed bonus", 2, Operation.ADD_NUMBER, EquipmentSlot.HAND));
		((Damageable) itemMeta).setDamage(2);
		// note: this data ends up getting stored in an arbitrary order internally
		PersistentDataContainer customTags = itemMeta.getPersistentDataContainer();
		customTags.set(new NamespacedKey("some_plugin", "some-key"), PersistentDataType.STRING, "some value");
		PersistentDataContainer customContainer = customTags.getAdapterContext().newPersistentDataContainer();
		customContainer.set(new NamespacedKey("inner_plugin", "inner-key"), PersistentDataType.FLOAT, 0.3F);
		customTags.set(new NamespacedKey("some_plugin", "some-other-key"), PersistentDataType.TAG_CONTAINER, customContainer);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	@Test
	public void testSerializationFull() {
		ItemStack itemStack = createItemStackFull();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		Assert.assertEquals("{type=DIAMOND_SWORD, display-name=&cCustom Name, loc-name=loc name, lore=[&alore1, lore2],"
				+ " custom-model-data=1, enchants={DURABILITY=1, DAMAGE_ALL=2}, attribute-modifiers="
				+ "{GENERIC_ATTACK_SPEED=[AttributeModifier{uuid=00000000-0000-0001-0000-000000000001, name=attack speed bonus, operation=ADD_NUMBER, amount=2.0, slot=HAND},"
				+ " AttributeModifier{uuid=00000000-0000-0002-0000-000000000002, name=attack speed bonus 2, operation=MULTIPLY_SCALAR_1, amount=0.5, slot=OFF_HAND}],"
				+ " GENERIC_MAX_HEALTH=[AttributeModifier{uuid=00000000-0000-0003-0000-000000000003, name=attack speed bonus, operation=ADD_NUMBER, amount=2.0, slot=HAND}]},"
				+ " ItemFlags=[HIDE_ENCHANTS], Unbreakable=true, Damage=2,"
				+ " PublicBukkitValues={some_plugin:some-other-key={inner_plugin:inner-key=0.3f}, some_plugin:some-key=some value}}",
				serialized.toString());
	}

	@Test
	public void testYAMLSerializationFull() {
		ItemStack itemStack = createItemStackFull();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		YamlConfiguration config = new YamlConfiguration();
		config.set("item", serialized);
		String yamlString = config.saveToString();
		Assert.assertEquals("item:" + ConfigUtils.yamlLineBreak() +
				"  type: DIAMOND_SWORD" + ConfigUtils.yamlLineBreak() +
				"  display-name: '&cCustom Name'" + ConfigUtils.yamlLineBreak() +
				"  loc-name: loc name" + ConfigUtils.yamlLineBreak() +
				"  lore:" + ConfigUtils.yamlLineBreak() +
				"  - '&alore1'" + ConfigUtils.yamlLineBreak() +
				"  - lore2" + ConfigUtils.yamlLineBreak() +
				"  custom-model-data: 1" + ConfigUtils.yamlLineBreak() +
				"  enchants:" + ConfigUtils.yamlLineBreak() +
				"    DURABILITY: 1" + ConfigUtils.yamlLineBreak() +
				"    DAMAGE_ALL: 2" + ConfigUtils.yamlLineBreak() +
				"  attribute-modifiers:" + ConfigUtils.yamlLineBreak() +
				"    GENERIC_ATTACK_SPEED:" + ConfigUtils.yamlLineBreak() +
				"    - ==: org.bukkit.attribute.AttributeModifier" + ConfigUtils.yamlLineBreak() +
				"      amount: 2.0" + ConfigUtils.yamlLineBreak() +
				"      name: attack speed bonus" + ConfigUtils.yamlLineBreak() +
				"      slot: HAND" + ConfigUtils.yamlLineBreak() +
				"      uuid: 00000000-0000-0001-0000-000000000001" + ConfigUtils.yamlLineBreak() +
				"      operation: 0" + ConfigUtils.yamlLineBreak() +
				"    - ==: org.bukkit.attribute.AttributeModifier" + ConfigUtils.yamlLineBreak() +
				"      amount: 0.5" + ConfigUtils.yamlLineBreak() +
				"      name: attack speed bonus 2" + ConfigUtils.yamlLineBreak() +
				"      slot: OFF_HAND" + ConfigUtils.yamlLineBreak() +
				"      uuid: 00000000-0000-0002-0000-000000000002" + ConfigUtils.yamlLineBreak() +
				"      operation: 2" + ConfigUtils.yamlLineBreak() +
				"    GENERIC_MAX_HEALTH:" + ConfigUtils.yamlLineBreak() +
				"    - ==: org.bukkit.attribute.AttributeModifier" + ConfigUtils.yamlLineBreak() +
				"      amount: 2.0" + ConfigUtils.yamlLineBreak() +
				"      name: attack speed bonus" + ConfigUtils.yamlLineBreak() +
				"      slot: HAND" + ConfigUtils.yamlLineBreak() +
				"      uuid: 00000000-0000-0003-0000-000000000003" + ConfigUtils.yamlLineBreak() +
				"      operation: 0" + ConfigUtils.yamlLineBreak() +
				"  ItemFlags:" + ConfigUtils.yamlLineBreak() +
				"  - HIDE_ENCHANTS" + ConfigUtils.yamlLineBreak() +
				"  Unbreakable: true" + ConfigUtils.yamlLineBreak() +
				"  Damage: 2" + ConfigUtils.yamlLineBreak() +
				"  PublicBukkitValues:" + ConfigUtils.yamlLineBreak() +
				"    some_plugin:some-other-key:" + ConfigUtils.yamlLineBreak() +
				"      inner_plugin:inner-key: 0.3f" + ConfigUtils.yamlLineBreak() +
				"    some_plugin:some-key: some value" + ConfigUtils.yamlLineBreak(), yamlString);
	}

	@Test
	public void testDeserializationFull() {
		ItemStack itemStack = createItemStackFull();
		ItemData itemData = new ItemData(itemStack);
		testDeserialization(itemData);
	}

	// UNCOMMON

	private static ItemStack createItemStackUncommon() {
		ItemStack itemStack = new ItemStack(Material.LEATHER_CHESTPLATE);
		LeatherArmorMeta itemMeta = (LeatherArmorMeta) itemStack.getItemMeta();
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemMeta.setColor(Color.BLUE);
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	@Test
	public void testSerializationUncommon() {
		ItemStack itemStack = createItemStackUncommon();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		Assert.assertEquals("{type=LEATHER_CHESTPLATE, display-name=&cCustom Name, color=Color:[rgb0x00FF]}", serialized.toString());
	}

	@Test
	public void testYAMLSerializationUncommon() {
		ItemStack itemStack = createItemStackUncommon();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		YamlConfiguration config = new YamlConfiguration();
		config.set("item", serialized);
		String yamlString = config.saveToString();
		Assert.assertEquals("item:" + ConfigUtils.yamlLineBreak() +
				"  type: LEATHER_CHESTPLATE" + ConfigUtils.yamlLineBreak() +
				"  display-name: '&cCustom Name'" + ConfigUtils.yamlLineBreak() +
				"  color:" + ConfigUtils.yamlLineBreak() +
				"    ==: Color" + ConfigUtils.yamlLineBreak() +
				"    RED: 0" + ConfigUtils.yamlLineBreak() +
				"    BLUE: 255" + ConfigUtils.yamlLineBreak() +
				"    GREEN: 0" + ConfigUtils.yamlLineBreak(), yamlString);
	}

	@Test
	public void testDeserializationUncommon() {
		ItemStack itemStack = createItemStackUncommon();
		ItemData itemData = new ItemData(itemStack);
		testDeserialization(itemData);
	}

	// TILE ENTITY SIMPLE

	private static ItemStack createItemStackTileEntitySimple() {
		ItemStack itemStack = new ItemStack(Material.CHEST);
		return itemStack;
	}

	@Test
	public void testSerializationTileEntitySimple() {
		ItemStack itemStack = createItemStackTileEntitySimple();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		Assert.assertEquals("CHEST", serialized.toString());
	}

	@Test
	public void testYAMLSerializationTileEntitySimple() {
		ItemStack itemStack = createItemStackTileEntitySimple();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		YamlConfiguration config = new YamlConfiguration();
		config.set("item", serialized);
		String yamlString = config.saveToString();
		Assert.assertEquals("item: CHEST" + ConfigUtils.yamlLineBreak(), yamlString);
	}

	@Test
	public void testDeserializationTileEntitySimple() {
		ItemStack itemStack = createItemStackTileEntitySimple();
		ItemData itemData = new ItemData(itemStack);
		testDeserialization(itemData);
	}

	// TILE ENTITY MINIMAL

	private static ItemStack createItemStackTileEntityMinimal() {
		ItemStack itemStack = new ItemStack(Material.CHEST);
		ItemMeta itemMeta = itemStack.getItemMeta();
		itemMeta.setDisplayName(ChatColor.RED + "Custom Name");
		itemStack.setItemMeta(itemMeta);
		return itemStack;
	}

	@Test
	public void testSerializationTileEntityMinimal() {
		ItemStack itemStack = createItemStackTileEntityMinimal();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		Assert.assertEquals("{type=CHEST, display-name=&cCustom Name}", serialized.toString());
	}

	@Test
	public void testYAMLSerializationTileEntityMinimal() {
		ItemStack itemStack = createItemStackTileEntityMinimal();
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		YamlConfiguration config = new YamlConfiguration();
		config.set("item", serialized);
		String yamlString = config.saveToString();
		Assert.assertEquals("item:" + ConfigUtils.yamlLineBreak() +
				"  type: CHEST" + ConfigUtils.yamlLineBreak() +
				"  display-name: '&cCustom Name'" + ConfigUtils.yamlLineBreak(), yamlString);
	}

	@Test
	public void testDeserializationTileEntityMinimal() {
		ItemStack itemStack = createItemStackTileEntityMinimal();
		ItemData itemData = new ItemData(itemStack);
		testDeserialization(itemData);
	}

	// ITEMDATA MATCHES

	@Test
	public void testItemDataMatches() {
		ItemStack itemStack = createItemStackFull();
		ItemData itemData = new ItemData(itemStack);
		ItemStack differentItemType = itemStack.clone();
		differentItemType.setType(Material.IRON_SWORD);
		ItemStack differentItemData = ItemUtils.setItemStackName(itemStack.clone(), "different name");

		Assert.assertTrue("ItemData#matches(ItemStack)", itemData.matches(itemStack));
		Assert.assertTrue("ItemData#matches(ItemData)", itemData.matches(new ItemData(itemStack)));
		Assert.assertFalse("!ItemData#matches(different item type)", itemData.matches(new ItemData(differentItemType)));
		Assert.assertFalse("!ItemData#matches(different item data)", itemData.matches(new ItemData(differentItemData)));
	}
}
