package com.nisovin.shopkeepers.util;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;

/**
 * An unmodifiable view on an {@link ItemStack}.
 * <p>
 * This class extends {@link ItemStack} so that it can be substituted in code that expects an {@link ItemStack}, as long
 * as that code does not attempt to modify the item stack. All getters delegate to the wrapped item stack.
 * <p>
 * The implementation of {@link #clone()} does not return a copy of this unmodifiable item stack, but instead returns a
 * modifiable copy of the underlying item stack.
 */
@DelegateDeserialization(ItemStack.class) // Serialized and deserialized as a normal modifiable ItemStack
public class SKUnmodifiableItemStack extends ItemStack implements UnmodifiableItemStack {

	/**
	 * Creates an {@link UnmodifiableItemStack} for the given {@link ItemStack}.
	 * <p>
	 * If the given item stack is already an {@link UnmodifiableItemStack}, this returns the given item stack itself.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the unmodifiable item stack, or <code>null</code> if the given item stack is <code>null</code>
	 */
	public static UnmodifiableItemStack of(@ReadOnly ItemStack itemStack) {
		if (itemStack == null) return null;
		if (itemStack instanceof UnmodifiableItemStack) {
			return (UnmodifiableItemStack) itemStack;
		}
		return new SKUnmodifiableItemStack(itemStack);
	}

	// ----

	private final ItemStack delegate;

	private SKUnmodifiableItemStack(@ReadOnly ItemStack itemStack) {
		assert itemStack != null;
		assert !(itemStack instanceof UnmodifiableItemStack);
		this.delegate = itemStack;
	}

	private UnsupportedOperationException unmodifiableException() {
		return new UnsupportedOperationException("This ItemStack cannot be modified!");
	}

	// Note: Any methods that the base class implements by delegating to other methods do not necessarily need to be
	// overridden here. However, we delegate all read-only methods to the underlying item stack nevertheless, because
	// the underlying item stack might provide an improved implementation specific to its concrete type of item stack
	// (eg. CraftItemStack).

	@Override
	public ItemStack copy() {
		return delegate.clone();
	}

	@Override
	public ItemStack asItemStack() {
		return this;
	}

	@Override
	public Material getType() {
		return delegate.getType();
	}

	@Override
	public void setType(Material type) {
		throw unmodifiableException();
	}

	@Override
	public int getAmount() {
		return delegate.getAmount();
	}

	@Override
	public void setAmount(int amount) {
		throw unmodifiableException();
	}

	@Override
	public MaterialData getData() {
		return delegate.getData();
	}

	@Override
	public void setData(MaterialData data) {
		throw unmodifiableException();
	}

	@Override
	public void setDurability(short durability) {
		throw unmodifiableException();
	}

	@Override
	public short getDurability() {
		return delegate.getDurability();
	}

	@Override
	public int getMaxStackSize() {
		return delegate.getMaxStackSize();
	}

	// The given item stack might also be another unmodifiable item stack.
	@Override
	public boolean isSimilar(@ReadOnly ItemStack itemStack) {
		if (this == itemStack) return true;
		if (itemStack instanceof UnmodifiableItemStack) {
			// This is expected to not modify or expose the passed item stack.
			return itemStack.isSimilar(delegate);
		} else {
			return delegate.isSimilar(itemStack);
		}
	}

	@Override
	public boolean isSimilar(UnmodifiableItemStack itemStack) {
		if (this == itemStack) return true;
		// This is expected to not modify or expose the passed item stack.
		return itemStack.isSimilar(delegate);
	}

	// TODO This method cannot be correctly implemented for CraftItemStack, because CraftItemStack always returns false
	// when being compared to an unknown type of ItemStack (i.e. this method cannot fulfill the symmetry requirement of
	// equals).
	@Override
	public boolean equals(@ReadOnly Object obj) {
		if (this == obj) return true;
		if (obj instanceof UnmodifiableItemStack) {
			UnmodifiableItemStack other = (UnmodifiableItemStack) obj;
			// This is expected to not modify or expose the passed item stack.
			return other.equals(delegate);
		} else {
			return delegate.equals(obj);
		}
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	@Override
	public ItemStack clone() {
		return delegate.clone();
	}

	@Override
	public boolean containsEnchantment(Enchantment enchantment) {
		return delegate.containsEnchantment(enchantment);
	}

	@Override
	public int getEnchantmentLevel(Enchantment enchantment) {
		return delegate.getEnchantmentLevel(enchantment);
	}

	@Override
	public Map<Enchantment, Integer> getEnchantments() {
		return delegate.getEnchantments();
	}

	// addEnchantments: Delegates to other methods.
	// addEnchantment: Delegates to other methods.
	// addUnsafeEnchantments: Delegates to other methods.

	@Override
	public void addUnsafeEnchantment(Enchantment enchantment, int level) {
		throw unmodifiableException();
	}

	@Override
	public int removeEnchantment(Enchantment enchantment) {
		throw unmodifiableException();
	}

	@Override
	public Map<String, Object> serialize() {
		return delegate.serialize();
	}

	@Override
	public ItemMeta getItemMeta() {
		return delegate.getItemMeta();
	}

	@Override
	public boolean hasItemMeta() {
		return delegate.hasItemMeta();
	}

	@Override
	public boolean setItemMeta(@ReadOnly ItemMeta itemMeta) {
		throw unmodifiableException();
	}

	@Override
	public String toString() {
		return "Unmodifiable" + delegate.toString();
	}
}
