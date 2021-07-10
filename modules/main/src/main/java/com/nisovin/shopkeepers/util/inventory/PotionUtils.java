package com.nisovin.shopkeepers.util.inventory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionType;

import com.nisovin.shopkeepers.util.bukkit.MinecraftEnumUtils;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Utility functions related to potion items, and other items with potion effects.
 */
public class PotionUtils {

	// TODO This may need to be updated on Minecraft updates.
	// Formatted like the keys of namespaced keys:
	private static Map<String, PotionType> POTION_TYPE_ALIASES = new HashMap<>();
	static {
		POTION_TYPE_ALIASES.put("empty", PotionType.UNCRAFTABLE);
		POTION_TYPE_ALIASES.put("leaping", PotionType.JUMP);
		POTION_TYPE_ALIASES.put("swiftness", PotionType.SPEED);
		POTION_TYPE_ALIASES.put("healing", PotionType.INSTANT_HEAL);
		POTION_TYPE_ALIASES.put("harming", PotionType.INSTANT_DAMAGE);
		POTION_TYPE_ALIASES.put("regeneration", PotionType.REGEN);
	}

	/**
	 * Parses the {@link PotionType} from the given input String.
	 * <p>
	 * Expected format: {@code [minecraft:]<potion name>}
	 * <p>
	 * The given input String is formatted like a valid Minecraft {@link NamespacedKey} (see
	 * {@link NamespacedKeyUtils#normalizeMinecraftNamespacedKey(String)}. The Minecraft namespace prefix is optional.
	 * 
	 * @param input
	 *            the input String
	 * @return the potion type, or <code>null</code> if the input is invalid
	 */
	public static PotionType parsePotionType(String input) {
		if (input == null) return null;

		// Parse as namespaced key:
		NamespacedKey namespacedKey = NamespacedKeyUtils.parse(input);
		if (namespacedKey == null) return null;
		if (!namespacedKey.getNamespace().equals(NamespacedKey.MINECRAFT)) return null;

		// Check the predefined potion type aliases:
		String potionName = namespacedKey.getKey();
		PotionType potionType = POTION_TYPE_ALIASES.get(potionName);
		if (potionType != null) return potionType;

		// Try to get the potion type by its Bukkit enum name:
		// This also matches the Minecraft name in most cases. Any non-matching Minecraft names should be covered by the
		// potion type aliases above.
		String potionEnumName = MinecraftEnumUtils.normalizeEnumName(potionName);
		try {
			return PotionType.valueOf(potionEnumName);
		} catch (IllegalArgumentException e) {
		}

		// Could not determine the potion type:
		return null;
	}

	/*
	 * # Expected formats:
	 * Potion: [long] [strong] [potion] [of] [long] [strong] <potion type> [2|ii] [potion] [2|ii]
	 * Splash potion: [long] [strong] <splash> [potion] [of] [long] [strong] <potion type> [2|ii] <splash> [potion] [2|ii]
	 * Lingering potion: [long] [strong] <lingering> [potion] [of] [long] [strong] <potion type> [2|ii] <lingering> [potion] [2|ii]
	 * Tipped arrow: [long] [strong] [tipped] [potion] <arrow> [of] [long] [strong] <potion type> [2|ii] [tipped] [potion] <arrow> [2|ii]
	 * 
	 * # Notes:
	 * - Angle brackets indicate required portions of the patterns. Only the potion type is expected to always be specified.
	 *   The keywords 'splash', 'lingering', and 'arrow' are used to differentiate the item types and their corresponding
	 *   patterns. If none of these keywords is found, the first pattern and a normal potion item are assumed.
	 * - Square brackets indicate optional words. These are either keywords that further specify the exact item or potion type,
	 *   or are simply ignored, but don't cause the parsing to fail when present. Vertical bars indicate alternative words that
	 *   have the same effect.
	 *   The keywords `long`, `strong`, `2`, or `ii` can be used to specify long or strong variants of the specified type of
	 *   potion. There are currently no potion variants that are both long and strong at the same time. Consequently, only one of
	 *   these keywords is allowed to be used at the same time. However, we currently ignore any additional occurrences of the
	 *   respectively other keywords. If the specified potion type does not support the selected variant, the keyword is
	 *   currently ignored as well. But this might change and potentially become more strict in a future release.
	 * - Each keyword can occur at most once within the input, but there may be multiple valid locations at which it is allowed
	 *   to occur (which is why the above formats mention some keywords multiple times). However, for simplicity, the parsing
	 *   does not actually take the order or dependencies of words into account currently, but only checks for the presence of
	 *   the various keywords. But this might change and potentially become more strict in a future release.
	 */
	public static ItemStack parsePotionItem(String input) {
		if (input == null || input.isEmpty()) return null;

		// General formatting:
		// This uses the formatting of namespaced keys. However, we don't parse the input as a namespaced key here,
		// because it may contain additional words that further specify the exact type of the item and potion.
		input = NamespacedKeyUtils.normalizeMinecraftNamespacedKey(input);

		// Split words:
		List<String> words = Arrays.asList(input.split("_"));

		// Remove common optional words:
		CollectionUtils.replace(words, "potion", null);
		CollectionUtils.replace(words, "of", null);

		// Determine the item type based on keywords:
		Material itemType = null;
		if (CollectionUtils.replace(words, "arrow", null)) {
			// Keywords: [tipped] [potion] arrow [of]
			itemType = Material.TIPPED_ARROW;
			CollectionUtils.replace(words, "tipped", null);
		} else if (CollectionUtils.replace(words, "splash", null)) {
			// Keywords: splash [potion] [of]
			itemType = Material.SPLASH_POTION;
		} else if (CollectionUtils.replace(words, "lingering", null)) {
			// Keywords: lingering [potion] [of]
			itemType = Material.LINGERING_POTION;
		} else {
			// Keywords: [potion] [of]
			itemType = Material.POTION;
		}
		assert itemType != null;

		// Long and strong potion variants:
		boolean extended = CollectionUtils.replace(words, "long", null);
		boolean upgraded = CollectionUtils.replace(words, "strong", null) || CollectionUtils.replace(words, "2", null) || CollectionUtils.replace(words, "ii", null);

		// Parse the potion type from the remaining input:
		input = words.stream().filter(Objects::nonNull).collect(Collectors.joining("_"));
		PotionType potionType = parsePotionType(input);
		if (potionType == null) {
			// Could not determine the potion type:
			return null;
		}

		// Validate potion variants:
		if (extended && !potionType.isExtendable()) {
			extended = false;
		}
		if (upgraded && !potionType.isUpgradeable()) {
			upgraded = false;
		}
		if (extended && upgraded) {
			// Only one of these variants is allowed at the same time:
			upgraded = false;
		}

		ItemStack item = new ItemStack(itemType, 1);
		PotionData potionData = new PotionData(potionType, extended, upgraded);
		item = setPotionData(item, potionData);
		return item;
	}

	/**
	 * Sets the {@link PotionData} of the given {@link ItemStack}, if its {@link ItemStack#getItemMeta() ItemMeta} is of
	 * type {@link PotionMeta}.
	 * <p>
	 * This applies, for example, to potions, splash potions, lingering potions, and tipped arrows.
	 * 
	 * @param itemStack
	 *            the item stack, not <code>null</code>
	 * @param potionData
	 *            the potion data, not <code>null</code>
	 * @return the same item stack
	 * @see PotionMeta#setBasePotionData(PotionData)
	 */
	public static ItemStack setPotionData(ItemStack itemStack, PotionData potionData) {
		Validate.notNull(itemStack, "itemStack is null");
		Validate.notNull(potionData, "potionData is null");
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta instanceof PotionMeta) {
			PotionMeta potionMeta = (PotionMeta) itemMeta;
			potionMeta.setBasePotionData(potionData);
			itemStack.setItemMeta(potionMeta);
		}
		return itemStack;
	}

	private PotionUtils() {
	}
}
