package com.nisovin.shopkeepers.util.inventory;

import java.util.Map;

import org.bukkit.Material;
import org.bukkit.configuration.serialization.DelegateDeserialization;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.checkerframework.checker.nullness.qual.PolyNull;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;

/**
 * An unmodifiable view on an {@link ItemStack}.
 */
@DelegateDeserialization(ItemStack.class) // De-/Serialized as a normal modifiable ItemStack
public class SKUnmodifiableItemStack implements UnmodifiableItemStack {

	/**
	 * Creates an {@link UnmodifiableItemStack} for the given {@link ItemStack}.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the unmodifiable item stack, or <code>null</code> if the given item stack is
	 *         <code>null</code>
	 */
	public static @PolyNull UnmodifiableItemStack of(@ReadOnly @PolyNull ItemStack itemStack) {
		if (itemStack == null) return null;
		return new SKUnmodifiableItemStack(itemStack);
	}

	// ----

	private final ItemStack delegate;

	private SKUnmodifiableItemStack(@ReadOnly ItemStack itemStack) {
		assert itemStack != null;
		this.delegate = itemStack;
	}

	/**
	 * Gets the underlying {@link ItemStack}.
	 * <p>
	 * The caller is expected to not modify or expose the returned item stack.
	 * 
	 * @return the underlying item stack
	 * @deprecated For internal use only, when we know that the item stack is not going to be
	 *             modified and we can therefore avoid copying it before using it in a context that
	 *             requires an {@link ItemStack}.
	 */
	@Deprecated
	public ItemStack getInternalItemStack() {
		return delegate;
	}

	@Override
	public ItemStack copy() {
		return delegate.clone();
	}

	@Override
	public UnmodifiableItemStack shallowCopy() {
		return UnmodifiableItemStack.ofNonNull(delegate);
	}

	@Override
	public Material getType() {
		return delegate.getType();
	}

	@Override
	public int getAmount() {
		return delegate.getAmount();
	}

	@Override
	public int getMaxStackSize() {
		return delegate.getMaxStackSize();
	}

	@Override
	public boolean isSimilar(@ReadOnly @Nullable ItemStack itemStack) {
		return delegate.isSimilar(itemStack);
	}

	@Override
	public boolean isSimilar(@Nullable UnmodifiableItemStack itemStack) {
		if (itemStack == null) return false;
		if (itemStack == this) return true;
		// Compare the underlying item stacks:
		// The order in which these delegate item stacks are compared does not matter.
		// This is expected to not modify or expose the passed item stack:
		return itemStack.isSimilar(delegate);
	}

	@Override
	public boolean equals(@ReadOnly @Nullable ItemStack itemStack) {
		return delegate.equals(itemStack);
	}

	@Override
	public boolean equals(@ReadOnly @Nullable Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof UnmodifiableItemStack)) return false;

		UnmodifiableItemStack other = (UnmodifiableItemStack) obj;
		// Compare the underlying item stacks:
		// The order in which these delegate item stacks are compared does not matter.
		// This is expected to not modify or expose the passed item stack:
		return other.equals(delegate);
	}

	@Override
	public int hashCode() {
		return delegate.hashCode();
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

	@Override
	public Map<String, Object> serialize() {
		return delegate.serialize();
	}

	@Override
	public @Nullable ItemMeta getItemMeta() {
		return delegate.getItemMeta();
	}

	@Override
	public boolean hasItemMeta() {
		return delegate.hasItemMeta();
	}

	@Override
	public String toString() {
		return "Unmodifiable" + delegate.toString();
	}
}
