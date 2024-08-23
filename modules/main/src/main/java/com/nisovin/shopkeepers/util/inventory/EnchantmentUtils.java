package com.nisovin.shopkeepers.util.inventory;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class EnchantmentUtils {

	// Minecraft uses shorts to store enchantment levels:
	private static final int MIN_LEVEL = Short.MIN_VALUE;
	private static final int MAX_LEVEL = Short.MAX_VALUE;

	// Lower case keys:
	private static final Map<String, Integer> LEVEL_NAMES = new HashMap<>();
	static {
		LEVEL_NAMES.put("min", Integer.MIN_VALUE); // Uses the enchantment's normal min level
		LEVEL_NAMES.put("max", Integer.MAX_VALUE); // Uses the enchantment's normal max level
		LEVEL_NAMES.put("i", 1);
		LEVEL_NAMES.put("ii", 2);
		LEVEL_NAMES.put("iii", 3);
		LEVEL_NAMES.put("iv", 4);
		LEVEL_NAMES.put("v", 5);
		LEVEL_NAMES.put("vi", 6);
		LEVEL_NAMES.put("vii", 7);
		LEVEL_NAMES.put("viii", 8);
		LEVEL_NAMES.put("ix", 9);
		LEVEL_NAMES.put("x", 10);
	}

	// TODO This may require updating on Minecraft updates.
	// Formatted like the keys of namespaced keys:
	private static final Map<String, Enchantment> ALIASES = new HashMap<>();
	static {
		ALIASES.put("curse_of_binding", Enchantment.BINDING_CURSE);
		ALIASES.put("curse_of_vanishing", Enchantment.VANISHING_CURSE);
		ALIASES.put("sweeping_edge", Enchantment.SWEEPING_EDGE);
		ALIASES.put("channelling", Enchantment.CHANNELING);
	}

	public static @Nullable Enchantment parseEnchantment(String input) {
		Validate.notNull(input, "input is null");

		// Parse namespaced key:
		NamespacedKey namespacedKey = NamespacedKeyUtils.parse(input);
		if (namespacedKey == null) return null;
		if (!namespacedKey.getNamespace().equals(NamespacedKey.MINECRAFT)) {
			return null;
		}

		// Check the predefined aliases:
		String enchantmentName = namespacedKey.getKey();
		Enchantment enchantment = ALIASES.get(enchantmentName);
		if (enchantment != null) {
			return enchantment;
		}

		// Lookup by key:
		return Enchantment.getByKey(namespacedKey);
	}

	/**
	 * Combination of an {@link Enchantment} and its level.
	 */
	public static final class EnchantmentWithLevel {

		private final Enchantment enchantment;
		private final int level;

		public EnchantmentWithLevel(Enchantment enchantment, int level) {
			Validate.notNull(enchantment, "enchantment is null");
			this.enchantment = enchantment;
			this.level = level;
		}

		/**
		 * Gets the {@link Enchantment}.
		 * 
		 * @return the enchantment
		 */
		public Enchantment getEnchantment() {
			return enchantment;
		}

		/**
		 * Gets the enchantment level.
		 * 
		 * @return the enchantment level
		 */
		public int getLevel() {
			return level;
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("EnchantmentWithLevel [enchantment=");
			builder.append(enchantment);
			builder.append(", level=");
			builder.append(level);
			builder.append("]");
			return builder.toString();
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + enchantment.hashCode();
			result = prime * result + level;
			return result;
		}

		@Override
		public boolean equals(@Nullable Object obj) {
			if (this == obj) return true;
			if (!(obj instanceof EnchantmentWithLevel)) return false;
			EnchantmentWithLevel other = (EnchantmentWithLevel) obj;
			if (!enchantment.equals(other.enchantment)) return false;
			if (level != other.level) return false;
			return true;
		}
	}

	// Format: <enchantment> <level>
	// The level parsing is quite lenient: If the specified level is invalid, we fallback to a valid
	// level.
	// TODO Add an option to toggle this behavior to this method.
	public static @Nullable EnchantmentWithLevel parseEnchantmentWithLevel(String input) {
		Validate.notNull(input, "input is null");

		// General formatting:
		String formattedInput = input.trim();
		formattedInput = formattedInput.toLowerCase(Locale.ROOT);

		String enchantmentInput = formattedInput;

		Enchantment enchantment;
		Integer level = null; // Null results in the enchantment's default (min) value to be used

		// Parse level, if present:
		int lastSpace = formattedInput.lastIndexOf(' ');
		if (lastSpace != -1) {
			String levelString = formattedInput.substring(lastSpace + 1);
			level = ConversionUtils.parseInt(levelString);
			if (level == null) {
				// Could not parse the level as integer. Check for predefined level names:
				level = LEVEL_NAMES.get(levelString); // Can be null
			}
			if (level != null) {
				// We parsed a level:
				enchantmentInput = formattedInput.substring(0, lastSpace);
			} else {
				// Could not parse the level. Maybe the section behind the last space is part of the
				// enchantment name. We therefore initially keep the input as is. If it is not part
				// of the enchantment name but actually an invalid level, parsing the enchantment
				// will initially fail. We then try to parse the enchantment again without this
				// portion of the input.
			}
		} // Else: No level specified.

		// Parse the enchantment:
		enchantment = parseEnchantment(enchantmentInput);
		if (enchantment == null) {
			// If we found a portion of the input that might be supposed to specify the level, but
			// we were not able to parse the level, check if we can parse the enchantment by
			// ignoring this portion of the input:
			if (level == null && lastSpace != -1) {
				enchantmentInput = enchantmentInput.substring(0, lastSpace);
				enchantment = parseEnchantment(enchantmentInput);
			}

			if (enchantment == null) {
				// We were not able to parse the enchantment:
				return null;
			}
		}
		assert enchantment != null;

		// Replace placeholder levels:
		if (level == null || level == Integer.MIN_VALUE) {
			level = enchantment.getStartLevel();
		} else if (level == Integer.MAX_VALUE) {
			level = enchantment.getMaxLevel();
		} else {
			// Normalize the specified level into valid bounds:
			// Note: We allow players to specify enchantment levels that are outside the bounds used
			// in vanilla Minecraft, because they work fine for the most part and may therefore be
			// used on servers.
			if (level < MIN_LEVEL) {
				level = MIN_LEVEL;
			} else if (level > MAX_LEVEL) {
				level = MAX_LEVEL;
			}
		}
		assert level != null;

		return new EnchantmentWithLevel(enchantment, level);
	}

	/**
	 * Checks if the given enchantment level is within the {@link Enchantment#getStartLevel() lower}
	 * and {@link Enchantment#getMaxLevel() upper} bounds of the given {@link Enchantment}.
	 * <p>
	 * However, even though higher or lower enchantment levels cannot be created in vanilla
	 * Minecraft (except via commands), they can be created by other means (e.g. commands, plugins,
	 * etc.) and may work fine. Some servers may therefore decide to create enchanted items with
	 * levels that are outside their normal bounds.
	 * 
	 * @param enchantment
	 *            the enchantment, not <code>null</code>
	 * @param level
	 *            the enchantment level
	 * @return <code>true</code> if the level in within the valid bounds
	 */
	public static boolean isValidLevel(Enchantment enchantment, int level) {
		Validate.notNull(enchantment, "enchantment is null");
		return level >= enchantment.getStartLevel() && level <= enchantment.getMaxLevel();
	}

	/**
	 * Creates an enchanted book item that stores the given {@link Enchantment}.
	 * 
	 * @param enchantment
	 *            the enchantment, not <code>null</code>
	 * @param level
	 *            the enchantment level
	 * @return the enchanted book item, not <code>null</code>
	 */
	public static ItemStack createEnchantedBook(Enchantment enchantment, int level) {
		Validate.notNull(enchantment, "enchantment is null");
		ItemStack item = new ItemStack(Material.ENCHANTED_BOOK, 1);
		EnchantmentStorageMeta itemMeta = Unsafe.castNonNull(item.getItemMeta());
		itemMeta.addStoredEnchant(enchantment, level, true); // Ignore level restrictions
		item.setItemMeta(itemMeta);
		return item;
	}

	private EnchantmentUtils() {
	}
}
