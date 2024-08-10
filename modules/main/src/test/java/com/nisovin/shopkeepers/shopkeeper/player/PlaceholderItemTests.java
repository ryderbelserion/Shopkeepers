package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionType;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.testutil.AbstractBukkitTest;
import com.nisovin.shopkeepers.util.inventory.EnchantmentUtils;
import com.nisovin.shopkeepers.util.inventory.PotionUtils;

public class PlaceholderItemTests extends AbstractBukkitTest {

	@Test
	public void testNonPlaceholderItems() {
		testParsing(null, null);
		testParsing(new ItemStack(Material.AIR), null);
		testParsing(new ItemStack(Material.STONE), null);
		testParsing(placeholderItem(null), null);
		testParsing(placeholderItem(""), null);
		testParsing(placeholderItem("bla"), null);
		testParsing(placeholderItem(ChatColor.RED + "bla"), null);
	}

	@Test
	public void testNormalPlaceholderItems() {
		testParsing(
				placeholderItem(Material.EMERALD_ORE.name()),
				new ItemStack(Material.EMERALD_ORE)
		);
		testParsing(
				placeholderItem("emerald_ore"),
				new ItemStack(Material.EMERALD_ORE)
		);
		testParsing(
				placeholderItem("emerald-ore"),
				new ItemStack(Material.EMERALD_ORE)
		);
		testParsing(
				placeholderItem("emerald ore"),
				new ItemStack(Material.EMERALD_ORE)
		);
		testParsing(
				placeholderItem(" emerald_ore  "),
				new ItemStack(Material.EMERALD_ORE)
		);
		testParsing(
				placeholderItem(ChatColor.RED + "emerald_ore"),
				new ItemStack(Material.EMERALD_ORE)
		);
		testParsing(
				placeholderItem("minecraft:emerald_ore"),
				new ItemStack(Material.EMERALD_ORE)
		);
		// The Material parsing does not use the lenient NamespacedKey parsing (yet):
		// testParsing(placeholderItem(":emerald_ore"), new ItemStack(Material.EMERALD_ORE));

		// Invalid materials:
		testParsing(placeholderItem(Material.AIR.name()), null);
		testParsing(placeholderItem(Material.FIRE.name()), null);
		testParsing(placeholderItem(Material.LEGACY_APPLE.name()), null);
	}

	@Test
	public void testEnchantedBooks() {
		Enchantment fireProtectionEnchantment = Enchantment.FIRE_PROTECTION;
		Enchantment bindingCurseEnchantment = Enchantment.BINDING_CURSE;

		testParsing(
				placeholderItem("fire_protection"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, 1)
		);
		testParsing(
				placeholderItem("fire-protection"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, 1)
		);
		testParsing(
				placeholderItem("fire protection"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, 1)
		);
		testParsing(
				placeholderItem("FIRE PROTECTION"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, 1)
		);
		testParsing(
				placeholderItem("curse of binding"),
				EnchantmentUtils.createEnchantedBook(bindingCurseEnchantment, 1)
		);

		// With level:
		testParsing(
				placeholderItem("fire_protection 2"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, 2)
		);
		testParsing(
				placeholderItem("fire_protection II"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, 2)
		);
		testParsing(
				placeholderItem("fire_protection X"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, 10)
		);
		testParsing(
				placeholderItem("fire_protection 0"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, 0)
		);
		testParsing(
				placeholderItem("fire_protection -1"),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, -1)
		);
		testParsing(
				placeholderItem("fire_protection min"),
				EnchantmentUtils.createEnchantedBook(
						fireProtectionEnchantment,
						fireProtectionEnchantment.getStartLevel()
				)
		);
		testParsing(
				placeholderItem("fire_protection max"),
				EnchantmentUtils.createEnchantedBook(
						fireProtectionEnchantment,
						fireProtectionEnchantment.getMaxLevel()
				)
		);
		testParsing(
				placeholderItem("fire_protection " + ((int) Short.MAX_VALUE + 10)),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, Short.MAX_VALUE)
		);
		testParsing(
				placeholderItem("fire_protection " + ((int) Short.MIN_VALUE - 10)),
				EnchantmentUtils.createEnchantedBook(fireProtectionEnchantment, Short.MIN_VALUE)
		);
	}

	@Test
	public void testPotions() {
		for (PotionType potionType : Registry.POTION) {
			// Note: Long and strong potions are their own potion types. Their keys start with
			// either "long" or "strong".
			testParsing(
					placeholderItem(potionType.getKey().getKey() + " potion"),
					potion(potionType)
			);
		}

		// Aliases:
		for (var aliasEntry : PotionUtils.POTION_TYPE_ALIASES_VIEW.entrySet()) {
			testParsing(
					placeholderItem(aliasEntry.getKey()),
					potion(aliasEntry.getValue())
			);
		}

		// Formatting:
		testParsing(
				placeholderItem("night_vision"),
				potion(PotionType.NIGHT_VISION)
		);
		testParsing(
				placeholderItem("night-vision"),
				potion(PotionType.NIGHT_VISION)
		);
		testParsing(
				placeholderItem("night vision"),
				potion(PotionType.NIGHT_VISION)
		);
		testParsing(
				placeholderItem("  night_vision "),
				potion(PotionType.NIGHT_VISION)
		);
		testParsing(
				placeholderItem("NIGHT_VISION"),
				potion(PotionType.NIGHT_VISION)
		);
		testParsing(
				placeholderItem("minecraft:night_vision"),
				potion(PotionType.NIGHT_VISION)
		);

		// Alternative formats:
		testParsing(
				placeholderItem("night_vision potion"),
				potion(PotionType.NIGHT_VISION)
		);
		testParsing(
				placeholderItem("night vision potion"),
				potion(PotionType.NIGHT_VISION)
		);
		testParsing(
				placeholderItem("potion of night vision"),
				potion(PotionType.NIGHT_VISION)
		);
		testParsing(
				placeholderItem("potion night vision"),
				potion(PotionType.NIGHT_VISION)
		);

		// Variants:
		testParsing(
				placeholderItem("long regeneration"),
				potion(PotionType.LONG_REGENERATION)
		);
		testParsing(
				placeholderItem("regeneration II"),
				potion(PotionType.STRONG_REGENERATION)
		);
		testParsing(
				placeholderItem("regeneration 2"),
				potion(PotionType.STRONG_REGENERATION)
		);
		testParsing(
				placeholderItem("strong regeneration"),
				potion(PotionType.STRONG_REGENERATION)
		);

		// Invalid variants:
		// Not extendable, 'long' is ignored:
		testParsing(
				placeholderItem("long luck"),
				potion(PotionType.LUCK)
		);
		// Not upgradable, 'strong' is ignored:
		testParsing(
				placeholderItem("strong luck"),
				potion(PotionType.LUCK)
		);
		// Can't be both long and strong, 'strong' is ignored:
		testParsing(
				placeholderItem("long potion of swiftness 2"),
				potion(PotionType.LONG_SWIFTNESS)
		);

		// Not a potion type, but a parsed as a normal potion item:
		testParsing(
				placeholderItem("potion"),
				new ItemStack(Material.POTION)
		);
	}

	@Test
	public void testSplashPotions() {
		testParsing(
				placeholderItem("swiftness splash potion"),
				splashPotion(PotionType.SWIFTNESS)
		);
		testParsing(placeholderItem("splash swiftness potion"),
				splashPotion(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("splash potion of swiftness"),
				splashPotion(PotionType.SWIFTNESS)
		);

		// Long variants:
		testParsing(
				placeholderItem("long splash potion of swiftness"),
				splashPotion(PotionType.LONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("splash potion of long swiftness"),
				splashPotion(PotionType.LONG_SWIFTNESS)
		);

		// Strong variants:
		testParsing(
				placeholderItem("strong splash potion of swiftness"),
				splashPotion(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("splash potion of strong swiftness"),
				splashPotion(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("splash potion of swiftness 2"),
				splashPotion(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("swiftness 2 splash potion"),
				splashPotion(PotionType.STRONG_SWIFTNESS)
		);
	}

	@Test
	public void testLingeringPotions() {
		testParsing(
				placeholderItem("swiftness lingering potion"),
				lingeringPotion(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("lingering swiftness potion"),
				lingeringPotion(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("lingering potion of swiftness"),
				lingeringPotion(PotionType.SWIFTNESS)
		);

		// Long variants:
		testParsing(
				placeholderItem("long lingering potion of swiftness"),
				lingeringPotion(PotionType.LONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("lingering potion of long swiftness"),
				lingeringPotion(PotionType.LONG_SWIFTNESS)
		);

		// Strong variants:
		testParsing(
				placeholderItem("strong lingering potion of swiftness"),
				lingeringPotion(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("lingering potion of strong swiftness"),
				lingeringPotion(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("lingering potion of swiftness 2"),
				lingeringPotion(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("swiftness 2 lingering potion"),
				lingeringPotion(PotionType.STRONG_SWIFTNESS)
		);
	}

	@Test
	public void testTippedArrows() {
		testParsing(
				placeholderItem("swiftness arrow"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("swiftness potion arrow"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("swiftness tipped potion arrow"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("swiftness tipped arrow"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("arrow of swiftness"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("arrow of swiftness potion"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("potion arrow of swiftness"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("tipped arrow of swiftness"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("tipped potion arrow of swiftness"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("tipped arrow of swiftness potion"),
				tippedArrow(PotionType.SWIFTNESS)
		);
		testParsing(
				placeholderItem("tipped swiftness potion arrow"),
				tippedArrow(PotionType.SWIFTNESS)
		);

		// Long variants:
		testParsing(
				placeholderItem("long tipped arrow of swiftness"),
				tippedArrow(PotionType.LONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("long arrow of swiftness"),
				tippedArrow(PotionType.LONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("tipped arrow of long swiftness"),
				tippedArrow(PotionType.LONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("arrow of long swiftness"),
				tippedArrow(PotionType.LONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("long swiftness arrow"),
				tippedArrow(PotionType.LONG_SWIFTNESS)
		);

		// Strong variants:
		testParsing(
				placeholderItem("strong tipped arrow of swiftness"),
				tippedArrow(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("strong arrow of swiftness"),
				tippedArrow(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("tipped arrow of strong swiftness"),
				tippedArrow(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("arrow of strong swiftness"),
				tippedArrow(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("tipped arrow of swiftness 2"),
				tippedArrow(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("swiftness 2 tipped arrow"),
				tippedArrow(PotionType.STRONG_SWIFTNESS)
		);
		testParsing(
				placeholderItem("strong swiftness arrow"),
				tippedArrow(PotionType.STRONG_SWIFTNESS)
		);
	}

	private ItemStack placeholderItem(@Nullable String displayName) {
		return PlaceholderItems.createPlaceholderItem(displayName);
	}

	private ItemStack potion(PotionType potionType) {
		return PotionUtils.setPotionType(new ItemStack(Material.POTION), potionType);
	}

	private ItemStack splashPotion(PotionType potionType) {
		return PotionUtils.setPotionType(new ItemStack(Material.SPLASH_POTION), potionType);
	}

	private ItemStack lingeringPotion(PotionType potionType) {
		return PotionUtils.setPotionType(new ItemStack(Material.LINGERING_POTION), potionType);
	}

	private ItemStack tippedArrow(PotionType potionType) {
		return PotionUtils.setPotionType(new ItemStack(Material.TIPPED_ARROW), potionType);
	}

	private void testParsing(@Nullable ItemStack placeholderItem, @Nullable ItemStack expected) {
		@Nullable ItemStack substitutedItem = PlaceholderItems.getSubstitutedItem(placeholderItem);
		Assert.assertEquals(
				Unsafe.nullableAsNonNull(expected),
				Unsafe.nullableAsNonNull(substitutedItem)
		);
	}
}
