package com.nisovin.shopkeepers.shopcreation;

import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.annotations.ReadWrite;
import com.nisovin.shopkeepers.util.bukkit.NamespacedKeyUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Helpers related to the shop creation item.
 */
public final class ShopCreationItem {

	/**
	 * The item persistent data {@link NamespacedKey} by which we identify the shop creation item.
	 */
	private static final NamespacedKey KEY_SHOP_CREATION_ITEM = NamespacedKeyUtils.create("shopkeepers", "shop_creation_item");

	public static ItemStack create() {
		return create(1);
	}

	public static ItemStack create(int amount) {
		return DerivedSettings.shopCreationItemData.createItemStack(amount);
	}

	public static boolean isShopCreationItem(@Nullable UnmodifiableItemStack itemStack) {
		return isShopCreationItem(ItemUtils.asItemStackOrNull(itemStack));
	}

	public static boolean isShopCreationItem(@ReadOnly @Nullable ItemStack itemStack) {
		if (Settings.identifyShopCreationItemByTag) {
			return hasTag(itemStack);
		} else {
			return Settings.shopCreationItem.matches(itemStack);
		}
	}

	public static boolean hasTag(@ReadOnly @Nullable ItemStack itemStack) {
		if (ItemUtils.isEmpty(itemStack)) return false;
		assert itemStack != null;

		ItemMeta meta = Unsafe.assertNonNull(itemStack.getItemMeta());
		PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
		// TODO Use has(NamespacedKey) without the data type validation once we depend on 1.20.4+?
		return dataContainer.has(KEY_SHOP_CREATION_ITEM, PersistentDataType.BOOLEAN);
	}

	// Returns true if the tag was freshly added.
	public static boolean addTag(@ReadWrite @Nullable ItemStack itemStack) {
		if (ItemUtils.isEmpty(itemStack)) return false;
		assert itemStack != null;

		ItemMeta meta = Unsafe.assertNonNull(itemStack.getItemMeta());
		PersistentDataContainer dataContainer = meta.getPersistentDataContainer();
		// TODO Use has(NamespacedKey) without the data type validation once we depend on 1.20.4+?
		if (dataContainer.has(KEY_SHOP_CREATION_ITEM, PersistentDataType.BOOLEAN)) {
			return false;
		}

		dataContainer.set(KEY_SHOP_CREATION_ITEM, PersistentDataType.BOOLEAN, true);
		itemStack.setItemMeta(meta);
		return true;
	}

	private ShopCreationItem() {
	}
}
