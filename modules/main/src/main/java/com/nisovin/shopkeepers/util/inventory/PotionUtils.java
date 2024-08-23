package com.nisovin.shopkeepers.util.inventory;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.MinecraftEnumUtils;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Utility functions related to potion items, and other items with potion effects.
 */
public final class PotionUtils {

	// TODO This may need to be updated on Minecraft updates.
	// Formatted like the keys of namespaced keys:
	private static final Map<String, PotionType> POTION_TYPE_ALIASES = new HashMap<>();
	// Visible for testing:
	public static Map<? extends String, ? extends PotionType> POTION_TYPE_ALIASES_VIEW = Collections.unmodifiableMap(POTION_TYPE_ALIASES);
	static {
		// Removed in Bukkit 1.20.5:
		// POTION_TYPE_ALIASES.put("empty", PotionType.UNCRAFTABLE);
		// Renamed in Bukkit 1.20.5, but the old names are still recognized by us:
		POTION_TYPE_ALIASES.put("jump", PotionType.LEAPING);
		POTION_TYPE_ALIASES.put("speed", PotionType.SWIFTNESS);
		POTION_TYPE_ALIASES.put("instant_heal", PotionType.HEALING);
		POTION_TYPE_ALIASES.put("instant_damage", PotionType.HARMING);
		POTION_TYPE_ALIASES.put("regen", PotionType.REGENERATION);
	}

	/**
	 * Parses the {@link PotionType} from the given input String.
	 * <p>
	 * Expected format: {@code [minecraft:]<potion name>}
	 * <p>
	 * The given input String is formatted like a valid Minecraft {@link NamespacedKey} (see
	 * {@link NamespacedKeyUtils#normalizeMinecraftNamespacedKey(String)}). The Minecraft namespace
	 * prefix is optional.
	 * 
	 * @param input
	 *            the input String, not <code>null</code>
	 * @return the potion type, or <code>null</code> if the input is invalid
	 */
	public static @Nullable PotionType parsePotionType(String input) {
		Validate.notNull(input, "input is null");

		// Parse as namespaced key:
		NamespacedKey namespacedKey = NamespacedKeyUtils.parse(input);
		if (namespacedKey == null) return null;
		if (!namespacedKey.getNamespace().equals(NamespacedKey.MINECRAFT)) return null;

		// Check the predefined potion type aliases:
		String potionName = namespacedKey.getKey();
		PotionType potionType = POTION_TYPE_ALIASES.get(potionName);
		if (potionType != null) return potionType;

		// Try to get the potion type by its Bukkit enum name:
		// This also matches the Minecraft name in most cases. Any non-matching Minecraft names
		// should be covered by the potion type aliases above.
		String potionEnumName = MinecraftEnumUtils.normalizeEnumName(potionName);
		try {
			return PotionType.valueOf(potionEnumName);
		} catch (IllegalArgumentException e) {
		}

		// Could not determine the potion type:
		return null;
	}

	/**
	 * Parses a potion item from a textual format.
	 * <p>
	 * Expected formats:
	 * <ul>
	 * <li>Potion: [long] [strong] [potion] [of] [long] [strong] <potion type> [2|ii] [potion]
	 * [2|ii]
	 * <li>Splash potion: [long] [strong] <splash> [potion] [of] [long] [strong] <potion type>
	 * [2|ii] <splash> [potion] [2|ii]
	 * <li>Lingering potion: [long] [strong] <lingering> [potion] [of] [long] [strong] <potion type>
	 * [2|ii] <lingering> [potion] [2|ii]
	 * <li>Tipped arrow: [long] [strong] [tipped] [potion] <arrow> [of] [long] [strong] <potion
	 * type> [2|ii] [tipped] [potion] <arrow> [2|ii]
	 * </ul>
	 * <p>
	 * Notes:
	 * <ul>
	 * <li>Angle brackets indicate required portions of the patterns. Only the potion type is
	 * expected to always be specified. The keywords 'splash', 'lingering', and 'arrow' are used to
	 * differentiate the item types and their corresponding patterns. If none of these keywords is
	 * found, the first pattern and a normal potion item are assumed.
	 * <li>Square brackets indicate optional words. These are either keywords that further specify
	 * the exact item or potion type, or are simply ignored, but don't cause the parsing to fail
	 * when present. Vertical bars indicate alternative words that have the same effect.
	 * <li>The keywords `long`, `strong`, `2`, or `ii` can be used to specify long or strong
	 * variants of the specified type of potion. There are currently no potion variants that are
	 * both long and strong at the same time. Consequently, only one of these keywords is allowed to
	 * be used at the same time. However, we currently ignore any additional occurrences of the
	 * respectively other keywords. If the specified potion type does not support the selected
	 * variant, the keyword is currently ignored as well. But this might change and potentially
	 * become stricter in a future release.
	 * <li>Each keyword can occur at most once within the input, but there may be multiple valid
	 * locations at which it is allowed to occur (which is why the above formats mention some
	 * keywords multiple times). However, for simplicity, the parsing does not actually take the
	 * order or dependencies of words into account currently, but only checks for the presence of
	 * the various keywords. But this might change and potentially become stricter in a future
	 * release.
	 * </ul>
	 * 
	 * @param input
	 *            the textural input, not <code>null</code>
	 * @return the parsed item stack, or <code>null</code>
	 */
	public static @Nullable ItemStack parsePotionItem(String input) {
		Validate.notNull(input, "input is null");
		if (input.isEmpty()) return null;

		// General formatting:
		// This uses the formatting of namespaced keys. However, we don't parse the input as a
		// namespaced key here, because it may contain additional words that further specify the
		// exact type of the item and potion.
		String formattedInput = NamespacedKeyUtils.normalizeMinecraftNamespacedKey(input);

		// Split words:
		List<@Nullable String> words = Unsafe.castNonNull(Arrays.asList(formattedInput.split("_")));

		// Remove common optional words:
		CollectionUtils.replace(words, "potion", null);
		CollectionUtils.replace(words, "of", null);

		// Determine the item type based on keywords:
		Material itemType;
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
		boolean isLong = CollectionUtils.replace(words, "long", null);
		boolean isStrong = CollectionUtils.replace(words, "strong", null)
				|| CollectionUtils.replace(words, "2", null)
				|| CollectionUtils.replace(words, "ii", null);

		// Parse the potion type from the remaining input:
		String potionTypeInput = words.stream()
				.filter(Objects::nonNull)
				.<String>map(Unsafe::assertNonNull)
				.collect(Collectors.joining("_"));
		PotionType potionType = parsePotionType(potionTypeInput);
		if (potionType == null) {
			// Could not determine the potion type:
			return null;
		}

		// Validate potion variants:
		if (isLong && !potionType.isExtendable()) {
			isLong = false;
		}
		if (isStrong && !potionType.isUpgradeable()) {
			isStrong = false;
		}
		if (isLong && isStrong) {
			// Only one of these variants is allowed at the same time:
			isStrong = false;
		}

		// Get the specified potion variant:
		if (isLong) {
			var longPotionType = getLongPotionType(potionType);
			if (longPotionType != null) {
				potionType = longPotionType;
			}
		} else if (isStrong) {
			var strongPotionType = getStrongPotionType(potionType);
			if (strongPotionType != null) {
				potionType = strongPotionType;
			}
		}

		ItemStack item = new ItemStack(itemType, 1);
		item = setPotionType(item, potionType);
		return item;
	}

	public static @Nullable PotionType getLongPotionType(PotionType potionType) {
		var key = potionType.getKey();
		if (key.getKey().startsWith("long_")) return potionType;
		if (!potionType.isExtendable()) return null;

		return Registry.POTION.get(NamespacedKeyUtils.create(key.getNamespace(), "long_" + key.getKey()));
	}

	public static @Nullable PotionType getStrongPotionType(PotionType potionType) {
		var key = potionType.getKey();
		if (key.getKey().startsWith("strong_")) return potionType;
		if (!potionType.isUpgradeable()) return null;

		return Registry.POTION.get(NamespacedKeyUtils.create(key.getNamespace(), "strong_" + key.getKey()));
	}

	/**
	 * Sets the {@link PotionType} of the given {@link ItemStack}, if its
	 * {@link ItemStack#getItemMeta() ItemMeta} is of type {@link PotionMeta}.
	 * <p>
	 * This applies, for example, to potions, splash potions, lingering potions, and tipped arrows.
	 * 
	 * @param itemStack
	 *            the item stack, not <code>null</code>
	 * @param potionType
	 *            the potion type, not <code>null</code>
	 * @return the same item stack
	 * @see PotionMeta#setBasePotionType(PotionType)
	 */
	public static ItemStack setPotionType(@ReadWrite ItemStack itemStack, PotionType potionType) {
		Validate.notNull(itemStack, "itemStack is null");
		Validate.notNull(potionType, "potionType is null");
		ItemMeta itemMeta = itemStack.getItemMeta();
		if (itemMeta instanceof PotionMeta) {
			PotionMeta potionMeta = (PotionMeta) itemMeta;
			potionMeta.setBasePotionType(potionType);
			itemStack.setItemMeta(potionMeta);
		}
		return itemStack;
	}

	// TODO Needs to be updated whenever new properties are added to PotionEffect.
	public static boolean equalsIgnoreDuration(PotionEffect effect1, PotionEffect effect2) {
		if (effect1 == effect2) return true;
		return effect1.getType().equals(effect2.getType())
				&& effect1.isAmbient() == effect2.isAmbient()
				&& effect1.getAmplifier() == effect2.getAmplifier()
				&& effect1.hasParticles() == effect2.hasParticles()
				&& effect1.hasIcon() == effect2.hasIcon();
	}

	/**
	 * Checks if the given collection of {@link PotionEffect} contains the given potion effect,
	 * ignoring the {@link PotionEffect#getDuration()}.
	 * 
	 * @param effects
	 *            the collection to check in
	 * @param effect
	 *            the effect to check for
	 * @return the found matching potion effect, or <code>null</code>
	 * @see #equalsIgnoreDuration(PotionEffect, PotionEffect)
	 */
	public static @Nullable PotionEffect findIgnoreDuration(
			Collection<? extends PotionEffect> effects,
			PotionEffect effect
	) {
		for (PotionEffect collectionEffect : effects) {
			if (PotionUtils.equalsIgnoreDuration(collectionEffect, effect)) {
				return collectionEffect;
			}
		}
		return null;
	}

	private PotionUtils() {
	}
}
