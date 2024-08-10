package com.nisovin.shopkeepers.util;

import java.util.logging.Logger;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.yaml.YamlUtils;

// Note: We test the ItemStack deserialization through ItemData. Since ItemData is defined by its
// stored ItemStack, this is sufficient to also test the deserialization of the ItemData itself.
public class ItemDataTest extends AbstractItemStackSerializationTest<@Nullable String> {

	private static final Logger LOGGER = Logger.getLogger(ItemDataTest.class.getCanonicalName());
	private static final boolean DEBUG = false;

	@BeforeClass
	public static void setup() {
	}

	@AfterClass
	public static void cleanup() {
		ItemData.resetSerializerPrefersPlainTextFormat();
	}

	private static String yamlNewline() {
		return YamlUtils.yamlNewline();
	}

	private String serializeToYamlConfig(@Nullable ItemData itemData) {
		YamlConfiguration yamlConfig = ConfigUtils.newYamlConfig();
		Object serialized = (itemData != null) ? itemData.serialize() : null;
		yamlConfig.set("item", serialized);
		return yamlConfig.saveToString();
	}

	private @Nullable ItemData deserializeFromYamlConfig(String yamlConfigString) {
		YamlConfiguration yamlConfig = ConfigUtils.newYamlConfig();
		try {
			yamlConfig.loadFromString(yamlConfigString);
		} catch (InvalidConfigurationException e) {
		}
		Object serialized = yamlConfig.get("item"); // Can be null
		if (serialized == null) return null;
		try {
			return ItemData.SERIALIZER.deserialize(serialized);
		} catch (InvalidDataException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected @Nullable String serialize(@Nullable ItemStack itemStack) {
		ItemData itemData = null;
		if (itemStack != null) {
			itemStack.setAmount(1); // We don't serialize the stack size
			itemData = new ItemData(itemStack);
		}
		return this.serializeToYamlConfig(itemData);
	}

	@Override
	protected @Nullable ItemStack deserialize(@Nullable String serialized) {
		if (serialized == null) return null;
		ItemData deserialized = this.deserializeFromYamlConfig(serialized);
		return (deserialized != null) ? deserialized.createItemStack() : null;
	}

	// Additional tests

	private void testYamlSerialization(ItemStack itemStack, String expected) {
		ItemData itemData = new ItemData(itemStack);
		String yamlString = this.serializeToYamlConfig(itemData);
		if (DEBUG) {
			LOGGER.info("expected: " + expected);
			LOGGER.info("actual: " + yamlString);
		}
		Assert.assertEquals(expected, yamlString);
	}

	// Compact representation (basic tool)

	@Test
	public void testYamlSerializationCompact() {
		ItemStack itemStack = TestItemStacks.createItemStackBasicTool();
		this.testYamlSerialization(itemStack, "item: DIAMOND_SWORD" + yamlNewline());
	}

	// Display name

	@Test
	public void testYAMLSerializationDisplayName() {
		ItemStack itemStack = TestItemStacks.createItemStackDisplayName();
		this.testYamlSerialization(
				itemStack,
				"item:" + yamlNewline()
						+ "  type: DIAMOND_SWORD" + yamlNewline()
						+ "  display-name: '{\"text\":\"Custom Name\",\"color\":\"red\"}'" + yamlNewline()
		);
	}

	// Complete meta

	@Test
	public void testYAMLSerializationComplete() {
		ItemStack itemStack = TestItemStacks.createItemStackComplete();
		this.testYamlSerialization(
				itemStack,
				"item:" + yamlNewline()
						+ "  type: DIAMOND_SWORD" + yamlNewline()
						+ "  display-name: '{\"text\":\"Custom Name\",\"color\":\"red\"}'" + yamlNewline()
						+ "  item-name: '{\"text\":\"Custom item name\",\"color\":\"red\"}'" + yamlNewline()
						+ "  lore:" + yamlNewline()
						+ "  - '{\"text\":\"lore1\",\"color\":\"green\"}'" + yamlNewline()
						+ "  - '\"lore2\"'" + yamlNewline()
						+ "  custom-model-data: 1" + yamlNewline()
						+ "  enchants:" + yamlNewline()
						+ "    minecraft:unbreaking: 1" + yamlNewline()
						+ "    minecraft:sharpness: 2" + yamlNewline()
						+ "  attribute-modifiers:" + yamlNewline()
						+ "    minecraft:generic.attack_speed:" + yamlNewline()
						+ "    - ==: org.bukkit.attribute.AttributeModifier" + yamlNewline()
						+ "      amount: 2.0" + yamlNewline()
						+ "      name: attack speed bonus" + yamlNewline()
						+ "      slot: hand" + yamlNewline()
						+ "      uuid: 00000000-0000-0001-0000-000000000001" + yamlNewline()
						+ "      operation: 0" + yamlNewline()
						+ "    - ==: org.bukkit.attribute.AttributeModifier" + yamlNewline()
						+ "      amount: 0.5" + yamlNewline()
						+ "      name: attack speed bonus 2" + yamlNewline()
						+ "      slot: offhand" + yamlNewline()
						+ "      uuid: 00000000-0000-0002-0000-000000000002" + yamlNewline()
						+ "      operation: 2" + yamlNewline()
						+ "    minecraft:generic.max_health:" + yamlNewline()
						+ "    - ==: org.bukkit.attribute.AttributeModifier" + yamlNewline()
						+ "      amount: 2.0" + yamlNewline()
						+ "      name: max health bonus" + yamlNewline()
						+ "      slot: hand" + yamlNewline()
						+ "      uuid: 00000000-0000-0003-0000-000000000003" + yamlNewline()
						+ "      operation: 0" + yamlNewline()
						+ "  repair-cost: 3" + yamlNewline()
						+ "  ItemFlags:" + yamlNewline()
						+ "  - HIDE_ENCHANTS" + yamlNewline()
						+ "  hide-tool-tip: true" + yamlNewline()
						+ "  Unbreakable: true" + yamlNewline()
						+ "  enchantment-glint-override: true" + yamlNewline()
						+ "  fire-resistant: true" + yamlNewline()
						+ "  max-stack-size: 65" + yamlNewline()
						+ "  rarity: EPIC" + yamlNewline()
						+ "  food:" + yamlNewline()
						+ "    ==: Food" + yamlNewline()
						+ "    nutrition: 2" + yamlNewline()
						+ "    saturation: 2.5" + yamlNewline()
						+ "    can-always-eat: true" + yamlNewline()
						+ "    eat-seconds: 5.5" + yamlNewline()
						+ "    effects:" + yamlNewline()
						+ "    - ==: FoodEffect" + yamlNewline()
						+ "      effect:" + yamlNewline()
						+ "        ==: PotionEffect" + yamlNewline()
						+ "        effect: minecraft:blindness" + yamlNewline()
						+ "        duration: 5" + yamlNewline()
						+ "        amplifier: 1" + yamlNewline()
						+ "        ambient: true" + yamlNewline()
						+ "        has-particles: true" + yamlNewline()
						+ "        has-icon: true" + yamlNewline()
						+ "      probability: 0.5" + yamlNewline()
						+ "  tool:" + yamlNewline()
						+ "    ==: Tool" + yamlNewline()
						+ "    default-mining-speed: 1.5" + yamlNewline()
						+ "    damage-per-block: 2" + yamlNewline()
						+ "    rules:" + yamlNewline()
						+ "    - ==: ToolRule" + yamlNewline()
						+ "      blocks:" + yamlNewline()
						+ "      - minecraft:stone" + yamlNewline()
						+ "      speed: 0.5" + yamlNewline()
						+ "      correct-for-drops: true" + yamlNewline()
						+ "  Damage: 2" + yamlNewline()
						+ "  max-damage: 10" + yamlNewline()
						+ "  PublicBukkitValues: |-" + yamlNewline()
						+ "    {" + yamlNewline()
						+ "        \"some_plugin:some-key\": \"some value\"," + yamlNewline()
						+ "        \"some_plugin:some-other-key\": {" + yamlNewline()
						+ "            \"inner_plugin:inner-key\": 0.3f" + yamlNewline()
						+ "        }" + yamlNewline()
						+ "    }" + yamlNewline()
		);
	}

	// Block data

	@Test
	public void testYAMLSerializationBlockData() {
		ItemStack itemStack = TestItemStacks.createItemStackBlockData();
		this.testYamlSerialization(
				itemStack,
				"item:" + yamlNewline()
						+ "  type: CAMPFIRE" + yamlNewline()
						+ "  BlockStateTag:" + yamlNewline()
						+ "    waterlogged: 'false'" + yamlNewline()
						+ "    signal_fire: 'false'" + yamlNewline()
						+ "    lit: 'false'" + yamlNewline()
						+ "    facing: north" + yamlNewline()
		);
	}

	// Uncommon ItemMeta

	@Test
	public void testYAMLSerializationUncommonMeta() {
		ItemStack itemStack = TestItemStacks.createItemStackUncommonMeta();
		this.testYamlSerialization(itemStack, "item:" + yamlNewline()
				+ "  type: LEATHER_CHESTPLATE" + yamlNewline()
				+ "  color:" + yamlNewline()
				+ "    ==: Color" + yamlNewline()
				+ "    ALPHA: 255" + yamlNewline()
				+ "    RED: 0" + yamlNewline()
				+ "    BLUE: 255" + yamlNewline()
				+ "    GREEN: 0" + yamlNewline());
	}

	// Basic TileEntity

	@Test
	public void testYAMLSerializationBasicTileEntity() {
		ItemStack itemStack = TestItemStacks.createItemStackBasicTileEntity();
		this.testYamlSerialization(itemStack, "item: CHEST" + yamlNewline());
	}

	// TileEntity with display name

	@Test
	public void testYAMLSerializationTileEntityDisplayName() {
		ItemStack itemStack = TestItemStacks.createItemStackTileEntityDisplayName();
		this.testYamlSerialization(
				itemStack,
				"item:" + yamlNewline()
						+ "  type: CHEST" + yamlNewline()
						+ "  display-name: '{\"text\":\"Custom Name\",\"color\":\"red\"}'" + yamlNewline()
		);
	}

	// ITEMDATA MATCHES

	@Test
	public void testItemDataMatches() {
		ItemStack itemStack = TestItemStacks.createItemStackComplete();
		ItemData itemData = new ItemData(itemStack);
		ItemStack differentItemType = itemStack.clone();
		differentItemType.setType(Material.IRON_SWORD);
		ItemStack differentItemData = ItemUtils.setDisplayName(itemStack.clone(), "different name");

		Assert.assertTrue(
				"ItemData#matches(ItemStack)",
				itemData.matches(itemStack)
		);
		Assert.assertTrue(
				"ItemData#matches(ItemData)",
				itemData.matches(new ItemData(itemStack))
		);
		Assert.assertFalse(
				"!ItemData#matches(different item type)",
				itemData.matches(new ItemData(differentItemType))
		);
		Assert.assertFalse(
				"!ItemData#matches(different item data)",
				itemData.matches(new ItemData(differentItemData))
		);
	}
}
