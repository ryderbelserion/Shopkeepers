package com.nisovin.shopkeepers.util;

import org.bukkit.Material;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.ItemData;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.yaml.YamlUtils;

// Note: We test the ItemStack deserialization through ItemData. Since ItemData is defined by its
// stored ItemStack, this is sufficient to also test the deserialization of the ItemData itself.
public class ItemDataTest extends AbstractItemStackSerializationTest<@Nullable String> {

	@BeforeClass
	public static void setup() {
		// Our test cases use the plain text format:
		ItemData.serializerPrefersPlainTextFormat(true);
	}

	@AfterClass
	public static void cleanup() {
		ItemData.resetSerializerPrefersPlainTextFormat();
	}

	private static String yamlNewline() {
		return YamlUtils.yamlNewline();
	}

	private String serializeToYamlConfig(@Nullable ItemData itemData) {
		YamlConfiguration yamlConfig = new YamlConfiguration();
		Object serialized = (itemData != null) ? itemData.serialize() : null;
		yamlConfig.set("item", serialized);
		return yamlConfig.saveToString();
	}

	private @Nullable ItemData deserializeFromYamlConfig(String yamlConfigString) {
		YamlConfiguration yamlConfig = new YamlConfiguration();
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

	private void testSerialization(ItemStack itemStack, String expected) {
		ItemData itemData = new ItemData(itemStack);
		Object serialized = itemData.serialize();
		Assert.assertEquals(expected, serialized.toString());
	}

	private void testYamlSerialization(ItemStack itemStack, String expected) {
		ItemData itemData = new ItemData(itemStack);
		String yamlString = this.serializeToYamlConfig(itemData);
		Assert.assertEquals(expected, yamlString);
	}

	// Compact representation (basic tool)

	@Test
	public void testSerializationCompact() {
		ItemStack itemStack = TestItemStacks.createItemStackBasicTool();
		this.testSerialization(itemStack, "DIAMOND_SWORD");
	}

	@Test
	public void testYamlSerializationCompact() {
		ItemStack itemStack = TestItemStacks.createItemStackBasicTool();
		this.testYamlSerialization(itemStack, "item: DIAMOND_SWORD" + yamlNewline());
	}

	// Display name

	@Test
	public void testSerializationDisplayName() {
		ItemStack itemStack = TestItemStacks.createItemStackDisplayName();
		this.testSerialization(
				itemStack,
				"{type=DIAMOND_SWORD, display-name=&cCustom Name}"
		);
	}

	@Test
	public void testYAMLSerializationDisplayName() {
		ItemStack itemStack = TestItemStacks.createItemStackDisplayName();
		this.testYamlSerialization(
				itemStack,
				"item:" + yamlNewline()
						+ "  type: DIAMOND_SWORD" + yamlNewline()
						+ "  display-name: '&cCustom Name'" + yamlNewline()
		);
	}

	// Complete meta

	@Test
	public void testSerializationComplete() {
		ItemStack itemStack = TestItemStacks.createItemStackComplete();
		this.testSerialization(
				itemStack,
				"{type=DIAMOND_SWORD, display-name=&cCustom Name,"
						+ " loc-name=loc name, lore=[&alore1, lore2], custom-model-data=1,"
						+ " enchants={DURABILITY=1, DAMAGE_ALL=2}, attribute-modifiers="
						+ "{GENERIC_ATTACK_SPEED=[AttributeModifier{"
						+ "uuid=00000000-0000-0001-0000-000000000001, name=attack speed bonus,"
						+ " operation=ADD_NUMBER, amount=2.0, slot=HAND}, AttributeModifier{"
						+ "uuid=00000000-0000-0002-0000-000000000002, name=attack speed bonus 2,"
						+ " operation=MULTIPLY_SCALAR_1, amount=0.5, slot=OFF_HAND}],"
						+ " GENERIC_MAX_HEALTH=[AttributeModifier{"
						+ "uuid=00000000-0000-0003-0000-000000000003, name=attack speed bonus,"
						+ " operation=ADD_NUMBER, amount=2.0, slot=HAND}]},"
						+ " repair-cost=3, ItemFlags=[HIDE_ENCHANTS], Unbreakable=true, Damage=2,"
						+ " PublicBukkitValues={"
						+ "some_plugin:some-other-key={inner_plugin:inner-key=0.3f},"
						+ " some_plugin:some-key=some value}}"
		);
	}

	@Test
	public void testYAMLSerializationComplete() {
		ItemStack itemStack = TestItemStacks.createItemStackComplete();
		this.testYamlSerialization(
				itemStack,
				"item:" + yamlNewline()
						+ "  type: DIAMOND_SWORD" + yamlNewline()
						+ "  display-name: '&cCustom Name'" + yamlNewline()
						+ "  loc-name: loc name" + yamlNewline()
						+ "  lore:" + yamlNewline()
						+ "  - '&alore1'" + yamlNewline()
						+ "  - lore2" + yamlNewline()
						+ "  custom-model-data: 1" + yamlNewline()
						+ "  enchants:" + yamlNewline()
						+ "    DURABILITY: 1" + yamlNewline()
						+ "    DAMAGE_ALL: 2" + yamlNewline()
						+ "  attribute-modifiers:" + yamlNewline()
						+ "    GENERIC_ATTACK_SPEED:" + yamlNewline()
						+ "    - ==: org.bukkit.attribute.AttributeModifier" + yamlNewline()
						+ "      amount: 2.0" + yamlNewline()
						+ "      name: attack speed bonus" + yamlNewline()
						+ "      slot: HAND" + yamlNewline()
						+ "      uuid: 00000000-0000-0001-0000-000000000001" + yamlNewline()
						+ "      operation: 0" + yamlNewline()
						+ "    - ==: org.bukkit.attribute.AttributeModifier" + yamlNewline()
						+ "      amount: 0.5" + yamlNewline()
						+ "      name: attack speed bonus 2" + yamlNewline()
						+ "      slot: OFF_HAND" + yamlNewline()
						+ "      uuid: 00000000-0000-0002-0000-000000000002" + yamlNewline()
						+ "      operation: 2" + yamlNewline()
						+ "    GENERIC_MAX_HEALTH:" + yamlNewline()
						+ "    - ==: org.bukkit.attribute.AttributeModifier" + yamlNewline()
						+ "      amount: 2.0" + yamlNewline()
						+ "      name: attack speed bonus" + yamlNewline()
						+ "      slot: HAND" + yamlNewline()
						+ "      uuid: 00000000-0000-0003-0000-000000000003" + yamlNewline()
						+ "      operation: 0" + yamlNewline()
						+ "  repair-cost: 3" + yamlNewline()
						+ "  ItemFlags:" + yamlNewline()
						+ "  - HIDE_ENCHANTS" + yamlNewline()
						+ "  Unbreakable: true" + yamlNewline()
						+ "  Damage: 2" + yamlNewline()
						+ "  PublicBukkitValues:" + yamlNewline()
						+ "    some_plugin:some-other-key:" + yamlNewline()
						+ "      inner_plugin:inner-key: 0.3f" + yamlNewline()
						+ "    some_plugin:some-key: some value" + yamlNewline()
		);
	}

	// Block data

	// TODO This might be broken in Bukkit: https://hub.spigotmc.org/jira/browse/SPIGOT-6257
	// Even though this specific test case passes, it might break once Spigot releases a fix for the
	// linked issue.
	// @Test
	public void testSerializationBlockData() {
		ItemStack itemStack = TestItemStacks.createItemStackBlockData();
		this.testSerialization(
				itemStack,
				"{type=CAMPFIRE, BlockStateTag={waterlogged=false, signal_fire=false, lit=false,"
						+ " facing=north}}"
		);
	}

	// TODO This might be broken in Bukkit: https://hub.spigotmc.org/jira/browse/SPIGOT-6257
	// Even though this specific test case passes, it might break once Spigot releases a fix for the
	// linked issue.
	// @Test
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
	public void testSerializationUncommonMeta() {
		ItemStack itemStack = TestItemStacks.createItemStackUncommonMeta();
		this.testSerialization(
				itemStack,
				"{type=LEATHER_CHESTPLATE, display-name=&cCustom Name, color=Color:[rgb0x00FF]}"
		);
	}

	@Test
	public void testYAMLSerializationUncommonMeta() {
		ItemStack itemStack = TestItemStacks.createItemStackUncommonMeta();
		this.testYamlSerialization(itemStack, "item:" + yamlNewline()
				+ "  type: LEATHER_CHESTPLATE" + yamlNewline()
				+ "  display-name: '&cCustom Name'" + yamlNewline()
				+ "  color:" + yamlNewline()
				+ "    ==: Color" + yamlNewline()
				+ "    RED: 0" + yamlNewline()
				+ "    BLUE: 255" + yamlNewline()
				+ "    GREEN: 0" + yamlNewline());
	}

	// Basic TileEntity

	@Test
	public void testSerializationBasicTileEntity() {
		ItemStack itemStack = TestItemStacks.createItemStackBasicTileEntity();
		this.testSerialization(itemStack, "CHEST");
	}

	@Test
	public void testYAMLSerializationBasicTileEntity() {
		ItemStack itemStack = TestItemStacks.createItemStackBasicTileEntity();
		this.testYamlSerialization(itemStack, "item: CHEST" + yamlNewline());
	}

	// TileEntity with display name

	@Test
	public void testSerializationTileEntityDisplayName() {
		ItemStack itemStack = TestItemStacks.createItemStackTileEntityDisplayName();
		this.testSerialization(itemStack, "{type=CHEST, display-name=&cCustom Name}");
	}

	@Test
	public void testYAMLSerializationTileEntityDisplayName() {
		ItemStack itemStack = TestItemStacks.createItemStackTileEntityDisplayName();
		this.testYamlSerialization(
				itemStack,
				"item:" + yamlNewline()
						+ "  type: CHEST" + yamlNewline()
						+ "  display-name: '&cCustom Name'" + yamlNewline()
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
