package com.nisovin.shopkeepers.shopkeeper;

import java.util.Objects;

import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Similar to {@link TradingRecipe}, but each item can be empty, independently of the other items
 * (e.g. {@link #getItem1() item1} can be empty even if {@link #getItem2() item2} is non-empty).
 * <p>
 * In order to avoid extensive item cloning, this class directly stores the item stacks that are
 * given during construction, without copying them first. This class exposes the item stacks always
 * only as {@link UnmodifiableItemStack}s, but the original underlying item stacks might get
 * modified externally. TODO Make the instances of this modifiable and reuse them inside the editor?
 */
public class TradingRecipeDraft {

	public static final TradingRecipeDraft EMPTY = new TradingRecipeDraft(
			(UnmodifiableItemStack) null, null, null
	);

	protected final @Nullable UnmodifiableItemStack resultItem;
	protected final @Nullable UnmodifiableItemStack item1;
	protected final @Nullable UnmodifiableItemStack item2;

	/**
	 * Creates a {@link TradingRecipeDraft}.
	 * <p>
	 * Empty items are normalized to <code>null</code>. If <code>item1</code> is empty,
	 * <code>item2</code> will take its place.
	 * <p>
	 * In order to avoid extensive item cloning, this class stores the given item stacks directly,
	 * without copying them first. This class exposes the item stacks always only as
	 * {@link UnmodifiableItemStack}s.
	 * 
	 * @param resultItem
	 *            the result item, can be empty
	 * @param item1
	 *            the first buy item, can be empty
	 * @param item2
	 *            the second buy item, can be empty
	 */
	public TradingRecipeDraft(
			@ReadOnly @Nullable ItemStack resultItem,
			@ReadOnly @Nullable ItemStack item1,
			@ReadOnly @Nullable ItemStack item2
	) {
		this(
				UnmodifiableItemStack.of(resultItem),
				UnmodifiableItemStack.of(item1),
				UnmodifiableItemStack.of(item2)
		);
	}

	/**
	 * Creates a {@link TradingRecipeDraft}.
	 * <p>
	 * Empty items are normalized to <code>null</code>.
	 * <p>
	 * In order to avoid extensive item cloning, this class stores the given item stacks directly,
	 * without copying them first. This class exposes the item stacks always only as
	 * {@link UnmodifiableItemStack}s.
	 * 
	 * @param resultItem
	 *            the result item, can be empty
	 * @param item1
	 *            the first buy item, can be empty
	 * @param item2
	 *            the second buy item, can be empty
	 */
	public TradingRecipeDraft(
			@Nullable UnmodifiableItemStack resultItem,
			@Nullable UnmodifiableItemStack item1,
			@Nullable UnmodifiableItemStack item2
	) {
		this.resultItem = ItemUtils.getNullIfEmpty(resultItem);
		this.item1 = ItemUtils.getNullIfEmpty(item1);
		this.item2 = ItemUtils.getNullIfEmpty(item2);
	}

	/**
	 * Gets the result item.
	 * 
	 * @return an unmodifiable view on the result item, can be <code>null</code>
	 */
	public @Nullable UnmodifiableItemStack getResultItem() {
		return resultItem;
	}

	/**
	 * Gets the first required item.
	 * 
	 * @return an unmodifiable view on the first required item, can be <code>null</code>
	 */
	public @Nullable UnmodifiableItemStack getItem1() {
		return item1;
	}

	/**
	 * Gets the second required item.
	 * 
	 * @return an unmodifiable view on the second required item, can be <code>null</code>
	 */
	public final @Nullable UnmodifiableItemStack getItem2() {
		return item2;
	}

	/**
	 * Gets the first required item of a valid trading recipe based on this draft.
	 * <p>
	 * Unlike {@link #getItem1()}, this reorders the input items of this trading recipe draft and
	 * returns {@link #getItem2() item2} if {@link #getItem1() item1} is empty.
	 * 
	 * @return an unmodifiable view on the first required item of a valid trading recipe based on
	 *         this draft, not <code>null</code> if this draft is {@link #isValid() valid}
	 */
	public final @Nullable UnmodifiableItemStack getRecipeItem1() {
		return (item1 != null) ? item1 : item2;
	}

	/**
	 * Gets the second required item of a valid trading recipe based on this draft.
	 * <p>
	 * Unlike {@link #getItem2()}, this reorders the input items of this trading recipe draft and
	 * returns <code>null</code> if {@link #getItem1() item1} is empty.
	 * 
	 * @return an unmodifiable view on the second required item of a valid trading recipe based on
	 *         this draft, can be <code>null</code>
	 */
	public final @Nullable UnmodifiableItemStack getRecipeItem2() {
		return (item1 != null) ? item2 : null;
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
	 * I.e. this checks if the result item and at least one of the input items are not empty.
	 *
	 * @return <code>true</code> if valid
	 */
	public final boolean isValid() {
		return resultItem != null && (item1 != null || item2 != null);
	}

	public final boolean areItemsEqual(
			@ReadOnly @Nullable ItemStack resultItem,
			@ReadOnly @Nullable ItemStack item1,
			@ReadOnly @Nullable ItemStack item2
	) {
		if (!ItemUtils.equals(this.resultItem, resultItem)) return false;
		if (!ItemUtils.equals(this.item1, item1)) return false;
		if (!ItemUtils.equals(this.item2, item2)) return false;
		return true;
	}

	public final boolean areItemsEqual(
			@Nullable UnmodifiableItemStack resultItem,
			@Nullable UnmodifiableItemStack item1,
			@Nullable UnmodifiableItemStack item2
	) {
		return this.areItemsEqual(
				ItemUtils.asItemStackOrNull(resultItem),
				ItemUtils.asItemStackOrNull(item1),
				ItemUtils.asItemStackOrNull(item2)
		);
	}

	public final boolean areItemsEqual(@Nullable TradingRecipeDraft otherRecipe) {
		if (otherRecipe == null) return false;
		return this.areItemsEqual(otherRecipe.resultItem, otherRecipe.item1, otherRecipe.item2);
	}

	public final boolean areItemsEqual(@Nullable TradingRecipe otherRecipe) {
		if (otherRecipe == null) return false;
		return this.areItemsEqual(
				otherRecipe.getResultItem(),
				otherRecipe.getItem1(),
				otherRecipe.getItem2()
		);
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
		result = prime * result + Objects.hashCode(resultItem);
		result = prime * result + Objects.hashCode(item1);
		result = prime * result + Objects.hashCode(item2);
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
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
