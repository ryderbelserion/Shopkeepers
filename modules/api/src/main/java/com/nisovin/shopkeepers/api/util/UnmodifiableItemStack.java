package com.nisovin.shopkeepers.api.util;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.internal.ApiInternals;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;

/**
 * An unmodifiable view on an {@link ItemStack}.
 * <p>
 * This interface mirrors most of the read-only methods of {@link ItemStack}. Implementations are
 * expected to delegate these methods to an underlying item stack.
 * <p>
 * Although this interface does not allow the modification of the underlying item stack, any
 * modifications to the underlying item stack by other means are dynamically reflected by the
 * methods of this interface.
 * <p>
 * If an {@link ItemStack} is required, use {@link #copy()} to create a normal modifiable item stack
 * copy with the same item data.
 * <p>
 * The Shopkeepers API uses this interface for at least two optimization purposes: Exposing
 * unmodifiable views on internal item stacks without having to copy them first. And as an indicator
 * whether an item stack passed as an argument to a method can be assumed to be immutable and does
 * therefore not require to be additionally copied before being internally stored. Callers of these
 * methods are expected to not modify the underlying item stacks of the passed unmodifiable item
 * stacks.
 * <p>
 * {@link #equals(Object)} compares two {@link UnmodifiableItemStack}s based on their underlying
 * item stacks. To compare the data of an unmodifiable item stack to a normal {@link ItemStack}, use
 * {@link #equals(ItemStack)} or {@link #isSimilar(ItemStack)} instead.
 * <p>
 * This interface extends {@link ConfigurationSerializable}. However, both the serialization and
 * deserialization of this type delegate to {@link ItemStack}. Consequently, an
 * {@link UnmodifiableItemStack} is serialized like a normal item stack and ends up being
 * deserialized as a normal modifiable {@link ItemStack}.
 */
public interface UnmodifiableItemStack extends ConfigurationSerializable {

	/**
	 * Creates an {@link UnmodifiableItemStack} for the given {@link ItemStack}.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the unmodifiable item stack, or <code>null</code> if the given item stack is
	 *         <code>null</code>
	 */
	public static @PolyNull UnmodifiableItemStack of(@PolyNull ItemStack itemStack) {
		return ApiInternals.getInstance().createUnmodifiableItemStack(itemStack);
	}

	/**
	 * Creates an {@link UnmodifiableItemStack} for the given {@link ItemStack}.
	 * <p>
	 * Unlike {@link #of(ItemStack)}, this method does not accept <code>null</code> as input and
	 * ensures that no <code>null</code> value is returned.
	 * 
	 * @param itemStack
	 *            the item stack, not <code>null</code>
	 * @return the unmodifiable item stack, not <code>null</code>
	 * @see UnmodifiableItemStack#of(ItemStack)
	 */
	public static UnmodifiableItemStack ofNonNull(ItemStack itemStack) {
		Preconditions.checkNotNull(itemStack, "itemStack is null");
		return Unsafe.assertNonNull(of(itemStack));
	}

	// ----

	/**
	 * Returns a modifiable copy of the underlying {@link ItemStack}. See also
	 * {@link ItemStack#clone()}.
	 * 
	 * @return a modifiable copy of the underlying item stack
	 */
	public ItemStack copy();

	/**
	 * Creates a shallow copy of this unmodifiable item stack, i.e. another
	 * {@link UnmodifiableItemStack} that is backed by the same underlying item stack as this
	 * unmodifiable item stack.
	 * <p>
	 * This can be useful in cases in which an unmodifiable item stack with a different object
	 * identity is required, but a full copy of the underlying item stack is not necessary.
	 * 
	 * @return the shallow copy
	 */
	public UnmodifiableItemStack shallowCopy();

	/**
	 * Gets the item stack's type.
	 * 
	 * @return the item type, not <code>null</code>
	 * @see ItemStack#getType()
	 */
	public Material getType();

	/**
	 * Gets the item stack's size.
	 * 
	 * @return the item stack size
	 * @see ItemStack#getAmount()
	 */
	public int getAmount();

	/**
	 * Gets the item's max stack size.
	 * 
	 * @return the item's max stack size, or <code>-1</code> if the max stack size is not known
	 * @see ItemStack#getMaxStackSize()
	 */
	public int getMaxStackSize();

	/**
	 * Compares the item stacks, ignoring their amounts.
	 * 
	 * @param itemStack
	 *            the item stack to compare with
	 * @return <code>true</code> if the item stacks are similar
	 * @see ItemStack#isSimilar(ItemStack)
	 */
	public boolean isSimilar(@Nullable ItemStack itemStack);

	/**
	 * This method behaves like {@link ItemStack#isSimilar(ItemStack)}, but compares the underlying
	 * item stacks of this and the given {@link UnmodifiableItemStack}.
	 * 
	 * @param itemStack
	 *            the other unmodifiable item stack
	 * @return <code>true</code> if the underlying item stacks are similar
	 */
	public boolean isSimilar(@Nullable UnmodifiableItemStack itemStack);

	/**
	 * Compares the underlying item stack of this unmodifiable item stack to the given
	 * {@link ItemStack}.
	 * 
	 * @param itemStack
	 *            the item stack to compare with
	 * @return <code>true</code> if the item stacks are equal
	 * @see ItemStack#equals(Object)
	 */
	public boolean equals(@Nullable ItemStack itemStack);

	/**
	 * Compares this unmodifiable item stack to the given object.
	 * <p>
	 * In order to not violate the symmetry requirement of {@link Object#equals(Object)}, this
	 * method only considers other {@link UnmodifiableItemStack}s as equal, and only if their
	 * underlying item stacks are equal. To compare this unmodifiable item stack with a normal
	 * {@link ItemStack}, use {@link #equals(ItemStack)} instead.
	 * 
	 * @param obj
	 *            the other object
	 * @return <code>true</code> if this and the given object are equal
	 */
	@Override
	public boolean equals(@Nullable Object obj);

	/**
	 * Checks if this item stack contains the given enchantment.
	 * 
	 * @param enchantment
	 *            the enchantment
	 * @return <code>true</code> if the item stack contains the specified enchantment
	 * @see ItemStack#containsEnchantment(Enchantment)
	 */
	public boolean containsEnchantment(Enchantment enchantment);

	/**
	 * Gets the level of the specified enchantment on this item stack.
	 * 
	 * @param enchantment
	 *            the enchantment
	 * @return the level, or <code>0</code>
	 * @see ItemStack#getEnchantmentLevel(Enchantment)
	 */
	public int getEnchantmentLevel(Enchantment enchantment);

	/**
	 * Gets a map of all the enchantments and their levels on this item stack.
	 * 
	 * @return the map of enchantments
	 * @see ItemStack#getEnchantments()
	 */
	public Map<Enchantment, Integer> getEnchantments();

	/**
	 * Creates a Map representation of this item stack.
	 * 
	 * @return a Map containing the current state of this item stack
	 * @see ItemStack#serialize()
	 */
	@Override
	public Map<String, Object> serialize();

	/**
	 * Get a copy of this item stack's {@link ItemMeta}.
	 * 
	 * @return the item meta, or <code>null</code> if this item stack does not support item meta
	 * @see ItemStack#getItemMeta()
	 */
	public @Nullable ItemMeta getItemMeta();

	/**
	 * Checks if this item stack has item meta.
	 * 
	 * @return <code>true</code> if this item stack has item meta
	 * @see ItemStack#hasItemMeta()
	 */
	public boolean hasItemMeta();
}
