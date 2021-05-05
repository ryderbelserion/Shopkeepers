package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.util.EnchantmentUtils;
import com.nisovin.shopkeepers.util.PotionUtils;

public class PlaceholderItemTests extends AbstractBukkitTest {

	@Test
	public void testNonPlaceholderItems() {
		testParsing(null, null);
		testParsing(new ItemStack(Material.AIR), null);
		testParsing(new ItemStack(Material.STONE), null);
		testParsing(new ItemStack(Material.NAME_TAG), null);
		testParsing(placeholderItem(null), null);
		testParsing(placeholderItem(""), null);
		testParsing(placeholderItem("bla"), null);
		testParsing(placeholderItem(ChatColor.RED + "bla"), null);
	}

	@Test
	public void testNormalPlaceholderItems() {
		testParsing(placeholderItem(Material.EMERALD_ORE.name()), new ItemStack(Material.EMERALD_ORE));
		testParsing(placeholderItem("emerald_ore"), new ItemStack(Material.EMERALD_ORE));
		testParsing(placeholderItem("emerald-ore"), new ItemStack(Material.EMERALD_ORE));
		testParsing(placeholderItem("emerald ore"), new ItemStack(Material.EMERALD_ORE));
		testParsing(placeholderItem(" emerald_ore  "), new ItemStack(Material.EMERALD_ORE));
		testParsing(placeholderItem(ChatColor.RED + "emerald_ore"), new ItemStack(Material.EMERALD_ORE));
		testParsing(placeholderItem("minecraft:emerald_ore"), new ItemStack(Material.EMERALD_ORE));
		// The Material parsing does not use the lenient NamespacedKey parsing (yet):
		// testParsing(placeholderItem(":emerald_ore"), new ItemStack(Material.EMERALD_ORE));

		// Invalid materials:
		testParsing(placeholderItem(Material.AIR.name()), null);
		testParsing(placeholderItem(Material.FIRE.name()), null);
		testParsing(placeholderItem(Material.LEGACY_APPLE.name()), null);
	}

	@Test
	public void testEnchantedBooks() {
		testParsing(placeholderItem("fire_protection"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, 1));
		testParsing(placeholderItem("fire-protection"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, 1));
		testParsing(placeholderItem("fire protection"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, 1));
		testParsing(placeholderItem("FIRE PROTECTION"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, 1));
		testParsing(placeholderItem("curse of binding"), EnchantmentUtils.createEnchantedBook(Enchantment.BINDING_CURSE, 1));

		// With level:
		testParsing(placeholderItem("fire_protection 2"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, 2));
		testParsing(placeholderItem("fire_protection II"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, 2));
		testParsing(placeholderItem("fire_protection X"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, 10));
		testParsing(placeholderItem("fire_protection 0"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, 0));
		testParsing(placeholderItem("fire_protection -1"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, -1));
		testParsing(placeholderItem("fire_protection min"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, Enchantment.PROTECTION_FIRE.getStartLevel()));
		testParsing(placeholderItem("fire_protection max"), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, Enchantment.PROTECTION_FIRE.getMaxLevel()));
		testParsing(placeholderItem("fire_protection " + ((int) Short.MAX_VALUE + 10)), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, Short.MAX_VALUE));
		testParsing(placeholderItem("fire_protection " + ((int) Short.MIN_VALUE - 10)), EnchantmentUtils.createEnchantedBook(Enchantment.PROTECTION_FIRE, Short.MIN_VALUE));
	}

	@Test
	public void testPotions() {
		for (PotionType potionType : PotionType.values()) {
			testParsing(placeholderItem(potionType.name() + " potion"), potion(new PotionData(potionType, false, false)));

			// Long variant:
			if (potionType.isExtendable()) {
				testParsing(placeholderItem("long " + potionType.name() + " potion"), potion(new PotionData(potionType, true, false)));
			}

			// Strong variant:
			if (potionType.isUpgradeable()) {
				testParsing(placeholderItem("strong " + potionType.name() + " potion"), potion(new PotionData(potionType, false, true)));
			}
		}

		// Formatting:
		testParsing(placeholderItem("night_vision"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("night-vision"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("night vision"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("  night_vision "), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("NIGHT_VISION"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("minecraft:night_vision"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));

		// Aliases:
		testParsing(placeholderItem("empty"), potion(new PotionData(PotionType.UNCRAFTABLE, false, false)));
		testParsing(placeholderItem("leaping"), potion(new PotionData(PotionType.JUMP, false, false)));

		// Alternative formats:
		testParsing(placeholderItem("night_vision potion"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("night vision potion"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("night vision 2"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("night vision II"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("night vision potion 2"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("potion of night vision"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));
		testParsing(placeholderItem("potion night vision"), potion(new PotionData(PotionType.NIGHT_VISION, false, false)));

		// Invalid variants:
		// Not extendable, 'long' is ignored:
		testParsing(placeholderItem("long luck"), potion(new PotionData(PotionType.LUCK, false, false)));
		// Not upgradable, 'strong' is ignored:
		testParsing(placeholderItem("strong luck"), potion(new PotionData(PotionType.LUCK, false, false)));
		// Can't be both long and strong, 'strong' is ignored:
		testParsing(placeholderItem("long potion of speed 2"), potion(new PotionData(PotionType.SPEED, true, false)));

		// Not a potion type, but a parsed as a normal potion item:
		testParsing(placeholderItem("potion"), new ItemStack(Material.POTION));
	}

	@Test
	public void testSplashPotions() {
		testParsing(placeholderItem("speed splash potion"), splashPotion(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("splash speed potion"), splashPotion(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("splash potion of speed"), splashPotion(new PotionData(PotionType.SPEED, false, false)));

		// Long variants:
		testParsing(placeholderItem("long splash potion of speed"), splashPotion(new PotionData(PotionType.SPEED, true, false)));
		testParsing(placeholderItem("splash potion of long speed"), splashPotion(new PotionData(PotionType.SPEED, true, false)));

		// Strong variants:
		testParsing(placeholderItem("strong splash potion of speed"), splashPotion(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("splash potion of strong speed"), splashPotion(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("splash potion of speed 2"), splashPotion(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("speed 2 splash potion"), splashPotion(new PotionData(PotionType.SPEED, false, true)));
	}

	@Test
	public void testLingeringPotions() {
		testParsing(placeholderItem("speed lingering potion"), lingeringPotion(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("lingering speed potion"), lingeringPotion(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("lingering potion of speed"), lingeringPotion(new PotionData(PotionType.SPEED, false, false)));

		// Long variants:
		testParsing(placeholderItem("long lingering potion of speed"), lingeringPotion(new PotionData(PotionType.SPEED, true, false)));
		testParsing(placeholderItem("lingering potion of long speed"), lingeringPotion(new PotionData(PotionType.SPEED, true, false)));

		// Strong variants:
		testParsing(placeholderItem("strong lingering potion of speed"), lingeringPotion(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("lingering potion of strong speed"), lingeringPotion(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("lingering potion of speed 2"), lingeringPotion(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("speed 2 lingering potion"), lingeringPotion(new PotionData(PotionType.SPEED, false, true)));
	}

	@Test
	public void testTippedArrows() {
		testParsing(placeholderItem("speed arrow"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("speed potion arrow"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("speed tipped potion arrow"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("speed tipped arrow"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("arrow of speed"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("arrow of speed potion"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("potion arrow of speed"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("tipped arrow of speed"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("tipped potion arrow of speed"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("tipped arrow of speed potion"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));
		testParsing(placeholderItem("tipped speed potion arrow"), tippedArrow(new PotionData(PotionType.SPEED, false, false)));

		// Long variants:
		testParsing(placeholderItem("long tipped arrow of speed"), tippedArrow(new PotionData(PotionType.SPEED, true, false)));
		testParsing(placeholderItem("long arrow of speed"), tippedArrow(new PotionData(PotionType.SPEED, true, false)));
		testParsing(placeholderItem("tipped arrow of long speed"), tippedArrow(new PotionData(PotionType.SPEED, true, false)));
		testParsing(placeholderItem("arrow of long speed"), tippedArrow(new PotionData(PotionType.SPEED, true, false)));
		testParsing(placeholderItem("long speed arrow"), tippedArrow(new PotionData(PotionType.SPEED, true, false)));

		// Strong variants:
		testParsing(placeholderItem("strong tipped arrow of speed"), tippedArrow(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("strong arrow of speed"), tippedArrow(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("tipped arrow of strong speed"), tippedArrow(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("arrow of strong speed"), tippedArrow(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("tipped arrow of speed 2"), tippedArrow(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("speed 2 tipped arrow"), tippedArrow(new PotionData(PotionType.SPEED, false, true)));
		testParsing(placeholderItem("strong speed arrow"), tippedArrow(new PotionData(PotionType.SPEED, false, true)));
	}

	private ItemStack placeholderItem(String displayName) {
		return PlaceholderItems.createPlaceholderItem(displayName);
	}

	private ItemStack potion(PotionData potionData) {
		return PotionUtils.setPotionData(new ItemStack(Material.POTION), potionData);
	}

	private ItemStack splashPotion(PotionData potionData) {
		return PotionUtils.setPotionData(new ItemStack(Material.SPLASH_POTION), potionData);
	}

	private ItemStack lingeringPotion(PotionData potionData) {
		return PotionUtils.setPotionData(new ItemStack(Material.LINGERING_POTION), potionData);
	}

	private ItemStack tippedArrow(PotionData potionData) {
		return PotionUtils.setPotionData(new ItemStack(Material.TIPPED_ARROW), potionData);
	}

	private void testParsing(ItemStack placeholderItem, ItemStack expected) {
		ItemStack substitutedItem = PlaceholderItems.getSubstitutedItem(placeholderItem);
		Assert.assertEquals(expected, substitutedItem);
	}
}
