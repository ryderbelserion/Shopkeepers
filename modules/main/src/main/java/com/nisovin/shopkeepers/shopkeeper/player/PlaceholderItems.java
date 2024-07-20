package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.inventory.EnchantmentUtils;
import com.nisovin.shopkeepers.util.inventory.EnchantmentUtils.EnchantmentWithLevel;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.inventory.PotionUtils;

/**
 * Helper methods related to placeholder items.
 * <p>
 * In some situations, such as when setting up the trades of certain types of player shopkeepers, or
 * when configuring the equipment of shopkeeper mobs, players can use these placeholder items as
 * substitutes for items they do not have.
 * <p>
 * The display name of the placeholder item specifies the substituted item type. Other properties of
 * the substituted item cannot be specified.
 * <p>
 * For developers: When a player uses placeholder items in an editor, instead of saving the
 * placeholder items themselves and later dynamically substituting them, e.g. when creating the
 * trading recipes for the stored trade offers, consider immediately replacing and saving the
 * substituted items instead. This has the following benefits:
 * <ul>
 * <li>Easier to use API: The trade offers specify the actually traded items.
 * <li>Placeholder items can break after server or plugin updates, for example because items were
 * renamed or are no longer recognized for other reasons. When used in trades, this would allow
 * other players to trade for items that were originally not intended when the trade was set up.
 * </ul>
 */
public final class PlaceholderItems {

	/**
	 * Creates a placeholder item.
	 * <p>
	 * If placeholder items are disabled (set to AIR), this returns an empty item stack that is not
	 * considered to be a {@link #isPlaceholderItem(ItemStack) placeholder item}.
	 * 
	 * @param displayName
	 *            the placeholder item's display name
	 * @return the placeholder item, not <code>null</code>
	 */
	public static ItemStack createPlaceholderItem(@Nullable String displayName) {
		ItemStack item = DerivedSettings.placeholderItemData.createItemStack();
		ItemUtils.setDisplayName(item, displayName);
		return item;
	}

	/**
	 * Checks if the given {@link ItemStack} is a placeholder item (i.e. is not empty and matches
	 * {@link DerivedSettings#placeholderItemData}).
	 * <p>
	 * This does not check if the given placeholder item specifies a
	 * {@link #isValidPlaceholderItem(ItemStack) valid substituted item}.
	 * <p>
	 * Empty item stacks are never considered to be placeholder items.
	 * 
	 * @param itemStack
	 *            the item stack
	 * @return <code>true</code> if the item stack is a placeholder item
	 */
	public static boolean isPlaceholderItem(@ReadOnly @Nullable ItemStack itemStack) {
		if (ItemUtils.isEmpty(itemStack)) return false;
		return DerivedSettings.placeholderItemData.matches(itemStack);
	}

	/**
	 * Gets the {@link ItemStack} that is substituted by the given placeholder {@link ItemStack}, if
	 * it actually is a valid placeholder item (i.e. if we can successfully derive its substituted
	 * item).
	 * 
	 * @param placeholderItem
	 *            the (potential) placeholder item
	 * @return the substituted item stack, or <code>null</code> if the given item stack is not a
	 *         valid placeholder
	 */
	public static @Nullable ItemStack getSubstitutedItem(
			@ReadOnly @Nullable ItemStack placeholderItem
	) {
		if (!isPlaceholderItem(placeholderItem)) return null;
		assert placeholderItem != null;

		// Get the display name:
		@Nullable ItemMeta meta = placeholderItem.getItemMeta();
		if (meta == null) return null;

		String displayName = meta.getDisplayName();

		// Not null, but can be empty:
		if (displayName.isEmpty()) return null;

		// Basic formatting:
		String normalizedDisplayName = normalizeDisplayName(displayName);

		// Check for a specified material:
		Material material = ItemUtils.parseMaterial(normalizedDisplayName);
		if (material != null) {
			// Validate the material:
			if (material.isLegacy() || material.isAir() || !material.isItem()) {
				return null;
			}

			// We preserve the stack size and specific meta data of the placeholder item stack:
			ItemStack substitutedItem = new ItemStack(material, placeholderItem.getAmount());
			applyItemMeta(substitutedItem, meta);
			return substitutedItem;
		}

		// Check for a specified enchantment:
		EnchantmentWithLevel enchantmentWithLevel = EnchantmentUtils.parseEnchantmentWithLevel(normalizedDisplayName);
		if (enchantmentWithLevel != null) {
			Enchantment enchantment = enchantmentWithLevel.getEnchantment();
			int level = enchantmentWithLevel.getLevel();
			return EnchantmentUtils.createEnchantedBook(enchantment, level);
		}

		// Check for a specified potion item:
		ItemStack potionItem = PotionUtils.parsePotionItem(normalizedDisplayName);
		if (potionItem != null) {
			return potionItem;
		}

		return null;
	}

	private static void applyItemMeta(@ReadWrite ItemStack substitutedItem, ItemMeta placeholderMeta) {
		assert substitutedItem != null && placeholderMeta != null;

		// In order to also use placeholder items to specify the carried block of endermans, we
		// preserve any present block state data:
		// This only works if the substituted item is a block type, and if the specified block state
		// data is compatible with the substituted block type.
		if (!substitutedItem.getType().isBlock()) return;
		if (!(placeholderMeta instanceof BlockDataMeta)) return;

		BlockDataMeta blockStateMeta = (BlockDataMeta) placeholderMeta;
		if (!blockStateMeta.hasBlockData()) return;

		@Nullable ItemMeta substitutedMeta = substitutedItem.getItemMeta();
		if (!(substitutedMeta instanceof BlockDataMeta)) return;

		BlockDataMeta substitutedBlockStateMeta = (BlockDataMeta) substitutedMeta;
		// Note: getBlockData only works for block types, and might add missing default block states
		// and omit unsupported block states.
		substitutedBlockStateMeta.setBlockData(blockStateMeta.getBlockData(substitutedItem.getType()));

		substitutedItem.setItemMeta(substitutedMeta);

		// TODO Also support preserving other item data / components (except the display name, and
		// any other item data that is used to identify the placeholder item itself)?
		// Even though players usually cannot configure the additional item data themselves, this
		// would allow admins to sell predefined placeholder items for items with additional meta
		// data.
		// However, there is currently no Bukkit API to automatically remove any item properties
		// that are used for the identification of the placeholder item itself.
		// Another alternative, that would also allow meta data to be specified that is used for the
		// identification of the placeholder item itself, would be to let admins specify the meta
		// data in for example Bukkit serialized form, or as NBT / component string, or maybe even
		// the complete item stack creation string, via a custom NBT tag. We could also offer a
		// command to let admins/players create a placeholder item for a given held item. And we can
		// then use Bukkit.getItemFactory().createItemStack to create the placeholder item from the
		// stored tag if present.
	}

	/**
	 * Gets the {@link Material} that is substituted by the given placeholder {@link ItemStack}, if
	 * it actually is a valid placeholder item.
	 * <p>
	 * Only checks for a substituted material. This does not check for other substituted item data,
	 * such as enchanted books, potions, etc.
	 * 
	 * @param placeholderItem
	 *            the (potential) placeholder item
	 * @return the substituted material (not necessarily an {@link Material#isItem() item type}), or
	 *         <code>null</code> if the given item stack is not a valid placeholder
	 */
	public static @Nullable Material getSubstitutedMaterial(
			@ReadOnly @Nullable ItemStack placeholderItem
	) {
		if (!isPlaceholderItem(placeholderItem)) return null;
		assert placeholderItem != null;

		// Get the display name:
		ItemMeta meta = Unsafe.assertNonNull(placeholderItem.getItemMeta());
		String displayName = meta.getDisplayName();

		// Not null, but can be empty:
		if (displayName.isEmpty()) return null;

		// Basic formatting:
		String normalizedDisplayName = normalizeDisplayName(displayName);

		// Check for a specified material:
		Material material = ItemUtils.parseMaterial(normalizedDisplayName);
		if (material != null) {
			// Validate the material:
			if (material.isLegacy() || material.isAir()) {
				return null;
			}

			return material; // Not necessarily an item type!
		}

		return null;
	}

	private static String normalizeDisplayName(String displayName) {
		// Basic formatting:
		String normalizedDisplayName = displayName.trim();
		normalizedDisplayName = Unsafe.assertNonNull(ChatColor.stripColor(normalizedDisplayName));
		return normalizedDisplayName;
	}

	/**
	 * Checks if the given {@link ItemStack} is a valid placeholder item, i.e. with valid
	 * {@link #getSubstitutedItem(ItemStack) substituted item}.
	 * 
	 * @param itemStack
	 *            the item stack
	 * @return <code>true</code> if the item stack is a valid placeholder
	 */
	public static boolean isValidPlaceholderItem(@ReadOnly @Nullable ItemStack itemStack) {
		return (getSubstitutedItem(itemStack) != null);
	}

	/**
	 * If the given item stack is a {@link #isValidPlaceholderItem(ItemStack) valid placeholder},
	 * this returns the {@link #getSubstitutedItem(ItemStack) substituted item stack}. Otherwise,
	 * this returns the given {@link ItemStack} itself.
	 * 
	 * @param itemStack
	 *            the (potential) placeholder item
	 * @return either the substituted item stack, if the given item stack is a valid placeholder, or
	 *         otherwise the given item stack itself
	 */
	public static @PolyNull ItemStack replace(@ReadOnly @PolyNull ItemStack itemStack) {
		ItemStack substitutedItem = getSubstitutedItem(itemStack);
		return (substitutedItem != null) ? substitutedItem : itemStack;
	}

	/**
	 * If the given item stack is a {@link #isValidPlaceholderItem(ItemStack) valid placeholder},
	 * this returns the {@link #getSubstitutedItem(ItemStack) substituted item stack}. Otherwise,
	 * this returns the given {@link ItemStack} itself.
	 * 
	 * @param itemStack
	 *            the (potential) placeholder item, not <code>null</code>
	 * @return either the substituted item stack, if the given item stack is a valid placeholder, or
	 *         otherwise the given item stack itself
	 */
	public static ItemStack replaceNonNull(@ReadOnly ItemStack itemStack) {
		return Unsafe.assertNonNull(replace(itemStack));
	}

	/**
	 * If the given item stack is a {@link #isValidPlaceholderItem(ItemStack) valid placeholder},
	 * this returns the {@link #getSubstitutedItem(ItemStack) substituted item stack}. Otherwise,
	 * this returns the given {@link UnmodifiableItemStack} itself.
	 * 
	 * @param itemStack
	 *            the (potential) placeholder item
	 * @return either the substituted item stack, if the given item stack is a valid placeholder, or
	 *         otherwise the given item stack itself
	 */
	public static @PolyNull UnmodifiableItemStack replace(
			@PolyNull UnmodifiableItemStack itemStack
	) {
		return UnmodifiableItemStack.of(replace(ItemUtils.asItemStackOrNull(itemStack)));
	}

	/**
	 * If the given item stack is a {@link #isValidPlaceholderItem(ItemStack) valid placeholder},
	 * this returns the {@link #getSubstitutedItem(ItemStack) substituted item stack}. Otherwise,
	 * this returns the given {@link UnmodifiableItemStack} itself.
	 * 
	 * @param itemStack
	 *            the (potential) placeholder item, not <code>null</code>
	 * @return either the substituted item stack, if the given item stack is a valid placeholder, or
	 *         otherwise the given item stack itself
	 */
	public static UnmodifiableItemStack replaceNonNull(UnmodifiableItemStack itemStack) {
		return Unsafe.assertNonNull(replace(itemStack));
	}

	private PlaceholderItems() {
	}
}
