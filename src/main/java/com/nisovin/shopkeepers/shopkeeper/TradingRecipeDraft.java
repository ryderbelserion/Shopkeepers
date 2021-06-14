package com.nisovin.shopkeepers.shopkeeper;

import java.util.Objects;

import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;

/**
 * Similar to {@link TradingRecipe}, but each item can be empty.
 * <p>
 * In order to avoid extensive item cloning, this class directly stores the item stacks that are given during
 * construction, without copying them first. This class exposes the item stacks always only as
 * {@link UnmodifiableItemStack}s, but the original underlying item stacks might get modified externally.
 * TODO Make the instances of this modifiable and reuse them inside the editor?
 */
public class TradingRecipeDraft {

	public static final TradingRecipeDraft EMPTY = new TradingRecipeDraft(
			(UnmodifiableItemStack) null, (UnmodifiableItemStack) null, (UnmodifiableItemStack) null
	);

	protected final UnmodifiableItemStack resultItem;
	protected final UnmodifiableItemStack item1;
	protected final UnmodifiableItemStack item2;

	/**
	 * Creates a {@link TradingRecipeDraft}.
	 * <p>
	 * Empty items are normalized to <code>null</code>. If <code>item1</code> is empty, <code>item2</code> will take its
	 * place.
	 * <p>
	 * In order to avoid extensive item cloning, this class stores the given item stacks directly, without copying them
	 * first. This class exposes the item stacks always only as {@link UnmodifiableItemStack}s.
	 * 
	 * @param resultItem
	 *            the result item, can be empty
	 * @param item1
	 *            the first buy item, can be empty
	 * @param item2
	 *            the second buy item, can be empty
	 */
	public TradingRecipeDraft(@ReadOnly ItemStack resultItem, @ReadOnly ItemStack item1, @ReadOnly ItemStack item2) {
		this(UnmodifiableItemStack.of(resultItem), UnmodifiableItemStack.of(item1), UnmodifiableItemStack.of(item2));
	}

	/**
	 * Creates a {@link TradingRecipeDraft}.
	 * <p>
	 * Empty items are normalized to <code>null</code>. If <code>item1</code> is empty, <code>item2</code> will take its
	 * place.
	 * <p>
	 * In order to avoid extensive item cloning, this class stores the given item stacks directly, without copying them
	 * first. This class exposes the item stacks always only as {@link UnmodifiableItemStack}s.
	 * 
	 * @param resultItem
	 *            the result item, can be empty
	 * @param item1
	 *            the first buy item, can be empty
	 * @param item2
	 *            the second buy item, can be empty
	 */
	public TradingRecipeDraft(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2) {
		this.resultItem = ItemUtils.getNullIfEmpty(resultItem);
		item1 = ItemUtils.getNullIfEmpty(item1);
		item2 = ItemUtils.getNullIfEmpty(item2);
		// Swap items if item1 is empty:
		if (item1 == null) {
			item1 = item2;
			item2 = null;
		}
		this.item1 = item1;
		this.item2 = item2;
	}

	/**
	 * Gets the result item.
	 * 
	 * @return an unmodifiable view on the result item, can be <code>null</code>
	 */
	public final UnmodifiableItemStack getResultItem() {
		return resultItem;
	}

	/**
	 * Gets the first required item.
	 * 
	 * @return an unmodifiable view on the first required item, can be <code>null</code>
	 */
	public final UnmodifiableItemStack getItem1() {
		return item1;
	}

	/**
	 * Gets the second required item.
	 * 
	 * @return an unmodifiable view on the second required item, can be <code>null</code>
	 */
	public final UnmodifiableItemStack getItem2() {
		return item2;
	}

	/**
	 * Checks whether this trading recipe draft has a {@link #getItem2() second item}.
	 * 
	 * @return <code>true</code> if the second item is not empty
	 */
	public boolean hasItem2() {
		return item2 != null;
	}

	/**
	 * Checks if all the items of this recipe are empty.
	 * 
	 * @return <code>true</code> if empty
	 */
	public final boolean isEmpty() {
		return resultItem == null && item1 == null && item2 == null;
	}

	/**
	 * Checks if this draft represents a valid trading recipe.
	 * <p>
	 * I.e. this checks if at least the result item and the first require item are not empty.
	 *
	 * @return <code>true</code> if valid
	 */
	public final boolean isValid() {
		return resultItem != null && item1 != null;
	}

	public final boolean areItemsEqual(@ReadOnly ItemStack resultItem, @ReadOnly ItemStack item1, @ReadOnly ItemStack item2) {
		// When using Objects#equals, the compiler / tooling complains about UnmodifiableItemStack being unrelated to
		// ItemStack. This used utility function is aware that we can compare unmodifiable with normal item stacks.
		if (!ItemUtils.equals(this.resultItem, resultItem)) return false;
		if (!ItemUtils.equals(this.item1, item1)) return false;
		if (!ItemUtils.equals(this.item2, item2)) return false;
		return true;
	}

	public final boolean areItemsEqual(UnmodifiableItemStack resultItem, UnmodifiableItemStack item1, UnmodifiableItemStack item2) {
		return areItemsEqual(ItemUtils.asItemStackOrNull(resultItem), ItemUtils.asItemStackOrNull(item1), ItemUtils.asItemStackOrNull(item2));
	}

	public final boolean areItemsEqual(TradingRecipeDraft otherRecipe) {
		if (otherRecipe == null) return false;
		return this.areItemsEqual(otherRecipe.resultItem, otherRecipe.item1, otherRecipe.item2);
	}

	public final boolean areItemsEqual(TradingRecipe otherRecipe) {
		if (otherRecipe == null) return false;
		return this.areItemsEqual(otherRecipe.getResultItem(), otherRecipe.getItem1(), otherRecipe.getItem2());
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("TradingRecipeDraft [resultItem=");
		builder.append(resultItem);
		builder.append(", item1=");
		builder.append(item1);
		builder.append(", item2=");
		builder.append(item2);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((resultItem == null) ? 0 : resultItem.hashCode());
		result = prime * result + ((item1 == null) ? 0 : item1.hashCode());
		result = prime * result + ((item2 == null) ? 0 : item2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof TradingRecipeDraft)) return false;
		TradingRecipeDraft other = (TradingRecipeDraft) obj;
		if (!Objects.equals(resultItem, other.resultItem)) return false;
		if (!Objects.equals(item1, other.item1)) return false;
		if (!Objects.equals(item2, other.item2)) return false;
		return true;
	}
}
