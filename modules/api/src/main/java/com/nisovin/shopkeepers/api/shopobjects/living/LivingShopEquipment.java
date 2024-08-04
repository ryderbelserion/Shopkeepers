package com.nisovin.shopkeepers.api.shopobjects.living;

import java.util.Map;

import org.bukkit.inventory.EquipmentSlot;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;

/**
 * The equipment of a {@link LivingShopObject}.
 */
public interface LivingShopEquipment {

	/**
	 * Gets all equipped items.
	 * 
	 * @return an unmodifiable view on the equipped items
	 */
	public Map<? extends EquipmentSlot, ? extends UnmodifiableItemStack> getItems();

	/**
	 * Gets the equipped item for the specified {@link EquipmentSlot}.
	 * 
	 * @param slot
	 *            the equipment slot
	 * @return the equipped item, or <code>null</code> if no item is equipped for the specified slot
	 */
	public @Nullable UnmodifiableItemStack getItem(EquipmentSlot slot);

	/**
	 * Sets the equipped item for the specified {@link EquipmentSlot}.
	 * 
	 * @param slot
	 *            the equipment slot
	 * @param item
	 *            the item to equip, or <code>null</code> to clear any currently equipped item
	 */
	public void setItem(EquipmentSlot slot, @Nullable UnmodifiableItemStack item);

	/**
	 * Clears all equipped items.
	 */
	public void clear();
}
