package com.nisovin.shopkeepers.api.util;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;

/**
 * An unmodifiable view on an {@link ItemStack}.
 * <p>
 * This interface mirrors (most of) the read-only methods of {@link ItemStack}. Implementations are expected to delegate
 * these methods to an underlying item stack.
 * <p>
 * Although this interface does not allow the modification of the underlying item stack, any modifications to the
 * underlying item stack by other means are dynamically reflected by the methods of this interface.
 * <p>
 * In order to avoid accidentally passing unmodifiable item stacks to code that is not aware that the item stack cannot
 * be modified, the API always exposes unmodifiable item stacks via this interface. Users of the API should not rely on
 * whether or not the implementation of this interface extends {@link ItemStack}. If an {@link ItemStack} view of this
 * unmodifiable item stack is required, use either {@link #copy()} to create a normal modifiable item stack copy with
 * the same item data, or the less safe {@link #asItemStack()} (see its documentation for notes about its limitations).
 * <p>
 * The API uses this interface for at least two optimization purposes: Exposing unmodifiable views on internal immutable
 * item stacks without having to copy them first. And as an indicator whether an item stack passed as argument to a
 * method can be assumed to be immutable and does therefore not require to be additionally copied before being
 * internally stored. Methods that make use of this second aspect are expected to clarify this in their documentation.
 * It is unsafe to pass an {@link #asItemStack() ItemStack view} of an unmodifiable item stack to a method that does not
 * explicitly state that it supports unmodifiable item stacks.
 * <p>
 * {@link #equals(Object)} compares two {@link UnmodifiableItemStack}s based on their underlying item stacks, but can
 * also be used to compare the {@link UnmodifiableItemStack} directly to a normal {@link ItemStack}. However, for some
 * types of {@link ItemStack} this comparison may not be symmetrical. See {@link #equals(Object)} for more information
 * on this limitation.
 * <p>
 * {@link #serialize()} is a read-only operation that delegates to {@link ItemStack#serialize()} of the underlying item
 * stack. In order to be able to directly serialize the underlying item stack with Bukkit's configuration serialization
 * API, without having to wrap this {@link UnmodifiableItemStack} in another serializable container, manually invoking
 * {@link #serialize()}, or first creating a copy of the underlying item stack, this interface extends
 * {@link ConfigurationSerializable}. However, both the serialization and deserialization of this type delegate to
 * {@link ItemStack}. Consequently, an {@link UnmodifiableItemStack} is serialized like a normal item stack and ends up
 * being deserialized as a normal modifiable {@link ItemStack}.
 */
public interface UnmodifiableItemStack extends ConfigurationSerializable {

	/**
	 * Creates an {@link UnmodifiableItemStack} for the given {@link ItemStack}.
	 * <p>
	 * If the given item stack is already an {@link UnmodifiableItemStack}, this returns the given item stack itself.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the unmodifiable item stack, or <code>null</code> if the given item stack is <code>null</code>
	 */
	public static UnmodifiableItemStack of(ItemStack itemStack) {
		return ShopkeepersAPI.createUnmodifiableItemStack(itemStack);
	}

	// ----

	/**
	 * Returns a modifiable copy of the underlying {@link ItemStack}. See also {@link ItemStack#clone()}.
	 * 
	 * @return a modifiable copy of the underlying item stack
	 */
	public ItemStack copy();

	/**
	 * Returns an {@link ItemStack} view of this unmodifiable item stack.
	 * <p>
	 * This is not the same as {@link #copy()}, nor does this return the underlying item stack itself. Instead, this
	 * returns an {@link ItemStack} whose implementation delegates its read-only methods to the underlying item stack
	 * and throws an {@link UnsupportedOperationException} for any methods that would usually modify the item stack.
	 * <p>
	 * The returned item stack has several limitations that make it unsafe to be used as a substitute for normal item
	 * stacks in general:
	 * <ul>
	 * <li>The primary purpose of this method is to be able to pass an unmodifiable item stack to methods that require
	 * an {@link ItemStack} and that do not modify the item stack, without having to copy the item stack first. An
	 * example use case are utility methods that would otherwise need to be duplicated in order to work with both
	 * {@link ItemStack} and {@link UnmodifiableItemStack}. Another use case are Bukkit API methods that are known to
	 * internally copy the passed item stack anyway. However, one has to be extremely careful to not accidentally pass
	 * the returned item stack to methods that might then attempt to modify it, or invoke unsupported methods on it.
	 * <li>The returned item stack is only guaranteed to support the operations that are also listed in this interface.
	 * Some server implementations extend the Bukkit API and add methods to {@link ItemStack}. These methods, even if
	 * they appear to be read-only, may not be supported by the returned item stack. Using them can result in exceptions
	 * or undefined behavior during runtime.
	 * <li>CraftBukkit's {@code CraftItemStack} implementation does not account for custom {@link ItemStack}
	 * implementations: Its implementation of {@link ItemStack#isSimilar(ItemStack)} and
	 * {@link ItemStack#equals(Object)} will never consider the item stacks returned by this method to be equal to
	 * itself, even if all the item stack data matches. Consequently, it is therefore not possible for the returned item
	 * stack to implement {@link #isSimilar(ItemStack)} and {@link #equals(Object)} without violating the symmetry
	 * requirement for at least {@code CraftItemStack}s.<br>
	 * However, instead of blocking these methods from being usable at all, or limited to comparisons with Bukkit's
	 * {@link ItemStack}, both of which wouldn't resolve the issue either because the comparisons can still silently
	 * fail without warning when performed in the opposite direction, they will work fine as long as the comparison is
	 * done with the unmodifiable item stack as the left operator and the {@code CraftItemStack} as the right operator.
	 * However, users of this method have to be extremely careful about this aspect.
	 * </ul>
	 * <p>
	 * The returned item stack implements {@link UnmodifiableItemStack}. Methods that receive an {@link ItemStack} can
	 * therefore check whether the item stack is unmodifiable by checking if it is an instance of
	 * {@link UnmodifiableItemStack}.
	 * <p>
	 * Implementations of {@link UnmodifiableItemStack} may choose to extend {@link ItemStack} and then return
	 * themselves as the result of this method. However, users of this method should not rely on this to be the case.
	 * 
	 * @return an {@link ItemStack} view on this unmodifiable item stack
	 * @deprecated Using this method comes with certain risks. This method is marked as deprecated in order to make it
	 *             less convenient to use and thereby make developers aware of its limitations. If these limitations
	 *             turn out to be too severe, or if future Bukkit changes make it difficulty to support this method with
	 *             reasonable effort, this method may also be removed again without notice.
	 */
	@Deprecated
	public ItemStack asItemStack();

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
	 * <p>
	 * For some item stack implementations, this method is non-symmetric. See {@link #asItemStack()} for more
	 * information about this limitation.
	 * 
	 * @param itemStack
	 *            the item stack to compare with
	 * @return <code>true</code> if the item stacks are similar
	 * @see ItemStack#isSimilar(ItemStack)
	 */
	public boolean isSimilar(ItemStack itemStack);

	/**
	 * This method behaves like {@link ItemStack#isSimilar(ItemStack)}, but compares the underlying item stacks of this
	 * and the given {@link UnmodifiableItemStack}.
	 * 
	 * @param itemStack
	 *            the other unmodifiable item stack
	 * @return <code>true</code> if the underlying item stacks are similar
	 */
	public boolean isSimilar(UnmodifiableItemStack itemStack);

	/**
	 * Same as {@link #equals(Object)}, but avoids compiler and tooling warnings about {@link ItemStack} being unrelated
	 * to {@link UnmodifiableItemStack} when trying to compare them directly via {@link #equals(Object)}.
	 * 
	 * @param itemStack
	 *            the item stack to compare with
	 * @return <code>true</code> if the item stack is equal to this item stack
	 */
	public default boolean equals(ItemStack itemStack) {
		return this.equals((Object) itemStack);
	}

	/**
	 * See {@link ItemStack#equals(Object)}.
	 * <p>
	 * If the given object is an {@link UnmodifiableItemStack}, the underlying item stacks of this and the given
	 * unmodifiable item stack are compared. If the given item stack is a normal {@link ItemStack}, it is compared to
	 * the underlying item stack.
	 * <p>
	 * For some item stack implementations, this method is non-symmetric and therefore breaks the contract of
	 * {@link Object#equals(Object)}. See {@link #asItemStack()} for more information about this limitation.
	 * 
	 * @param obj
	 *            the other object
	 * @return <code>true</code> if this and the given object are equal
	 */
	@Override
	public boolean equals(Object obj);

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
	public Map<String, Object> serialize();

	/**
	 * Get a copy of this item stack's {@link ItemMeta}.
	 * 
	 * @return the item meta, or <code>null</code> if this item stack does not support item meta
	 * @see ItemStack#getItemMeta()
	 */
	public ItemMeta getItemMeta();

	/**
	 * Checks if this item stack has item meta.
	 * 
	 * @return <code>true</code> if this item stack has item meta
	 * @see ItemStack#hasItemMeta()
	 */
	public boolean hasItemMeta();
}
