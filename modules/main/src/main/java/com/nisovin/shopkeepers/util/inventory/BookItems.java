package com.nisovin.shopkeepers.util.inventory;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Utilities related to book items.
 */
public final class BookItems {

	/**
	 * Checks if the given {@link ItemStack} is {@link ItemUtils#isEmpty(ItemStack) non-empty} and a
	 * {@link Material#WRITTEN_BOOK written book}.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return <code>true</code> if the item stack is a written book
	 */
	public static boolean isWrittenBook(@ReadOnly @Nullable ItemStack itemStack) {
		if (itemStack == null) return false;
		if (itemStack.getType() != Material.WRITTEN_BOOK) return false;
		// We also check the item's stack size to ensure that it is not empty:
		if (itemStack.getAmount() <= 0) return false;
		return true;
	}

	/**
	 * Gets the {@link BookMeta} of the given item, if it is a {@link #isWrittenBook(ItemStack)
	 * written book}.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the book meta, or <code>null</code> if the given item stack is not a written book
	 */
	public static @Nullable BookMeta getBookMeta(@ReadOnly @Nullable ItemStack itemStack) {
		if (!isWrittenBook(itemStack)) return null;
		return Unsafe.castNonNull(Unsafe.assertNonNull(itemStack).getItemMeta());
	}

	/**
	 * Gets the {@link BookMeta} of the given item, if it is a {@link #isWrittenBook(ItemStack)
	 * written book}.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return the book meta, or <code>null</code> if the given item stack is not a written book
	 */
	public static @Nullable BookMeta getBookMeta(@Nullable UnmodifiableItemStack itemStack) {
		return getBookMeta(ItemUtils.asItemStackOrNull(itemStack));
	}

	// BOOK TITLE

	/**
	 * Gets the book title of the given {@link ItemStack}, if it is a
	 * {@link #isWrittenBook(ItemStack) written book}.
	 * 
	 * @param itemStack
	 *            the item stack
	 * @return the book title, or <code>null</code> if the given item stack either is no written
	 *         book or has no title
	 */
	public static @Nullable String getBookTitle(@ReadOnly @Nullable ItemStack itemStack) {
		BookMeta bookMeta = getBookMeta(itemStack);
		if (bookMeta == null) return null;
		return getTitle(bookMeta);
	}

	/**
	 * Gets the book title from the given {@link BookMeta}.
	 * 
	 * @param bookMeta
	 *            the book meta, not <code>null</code>
	 * @return the book title, or <code>null</code> if the book has no title
	 */
	public static @Nullable String getTitle(@ReadOnly BookMeta bookMeta) {
		Validate.notNull(bookMeta, "bookMeta is null");
		// For our purposes, we ignore books with empty titles for now and therefore return null
		// here for them as well.
		// TODO Support them? They can't be created in vanilla Minecraft.
		// Null if the book has no title, or if the title is empty:
		return StringUtils.getNotEmpty(bookMeta.getTitle());
	}

	// BOOK GENERATION

	/**
	 * Gets the {@link Generation} from the given {@link BookMeta}.
	 * <p>
	 * Minecraft treats book items without explicit generation like original books. We therefore
	 * also return {@link Generation#ORIGINAL} if the book meta does not explicitly specify a
	 * generation.
	 * 
	 * @param bookMeta
	 *            the book meta, not <code>null</code>
	 * @return the book generation, not <code>null</code>
	 */
	public static Generation getGeneration(@ReadOnly BookMeta bookMeta) {
		Validate.notNull(bookMeta, "bookMeta is null");
		Generation generation = bookMeta.getGeneration(); // Can be null
		if (generation == null) {
			// If the generation is missing, Minecraft treats the book item as an original, and so
			// do we:
			return Generation.ORIGINAL;
		} else {
			return generation;
		}
	}

	/**
	 * Checks if book items with the given {@link Generation} can be copied.
	 * 
	 * @param generation
	 *            the book generation, not <code>null</code>
	 * @return <code>true</code> if book items with the given generation can be copied
	 */
	public static boolean isCopyable(Generation generation) {
		return (getCopyGeneration(generation) != null);
	}

	/**
	 * Gets the {@link Generation} that a book item with the given generation would use when being
	 * copied.
	 * 
	 * @param generation
	 *            the generation of the book item that is being copied, not <code>null</code>
	 * @return the generation of the book copy, or <code>null</code> if book items with the given
	 *         generation cannot be copied
	 */
	public static @Nullable Generation getCopyGeneration(Generation generation) {
		Validate.notNull(generation, "generation is null");
		switch (generation) {
		case ORIGINAL:
			return Generation.COPY_OF_ORIGINAL;
		case COPY_OF_ORIGINAL:
			return Generation.COPY_OF_COPY;
		default:
			// Book items with any other generation cannot be copied:
			return null;
		}
	}

	/**
	 * Checks if the given book {@link Generation} is a copy.
	 * <p>
	 * The generations {@link Generation#COPY_OF_ORIGINAL} and {@link Generation#COPY_OF_COPY} are
	 * considered to be copies, whereas any other generations (including
	 * {@link Generation#TATTERED}) are not.
	 * 
	 * @param generation
	 *            the book generation, not <code>null</code>
	 * @return <code>true</code> if the given book generation is a copy
	 */
	public static boolean isCopy(Generation generation) {
		return (generation == Generation.COPY_OF_ORIGINAL)
				|| (generation == Generation.COPY_OF_COPY);
	}

	// BOOK COPIES

	/**
	 * Checks if the given {@link ItemStack} is a copyable book.
	 * <p>
	 * This checks if the given item is a {@link #isWrittenBook(ItemStack) written book} and, based
	 * on its {@link Generation}, if it can be {@link #isCopyable(Generation) copied}.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return <code>true</code> if the item stack is a copyable book
	 */
	public static boolean isCopyableBook(@ReadOnly @Nullable ItemStack itemStack) {
		BookMeta bookMeta = getBookMeta(itemStack);
		if (bookMeta == null) return false; // Not a written book
		return isCopyable(bookMeta);
	}

	/**
	 * Checks if the book with the given {@link BookMeta} can be copied.
	 * <p>
	 * This uses the book's {@link Generation} to determine if it can be
	 * {@link #isCopyable(Generation) copied}.
	 * 
	 * @param bookMeta
	 *            the book meta, not <code>null</code>
	 * @return <code>true</code> if the book can be copied
	 */
	public static boolean isCopyable(@ReadOnly BookMeta bookMeta) {
		Generation generation = getGeneration(bookMeta);
		return isCopyable(generation);
	}

	/**
	 * Checks if the given {@link ItemStack} is a book copy.
	 * <p>
	 * This checks if the given item is a {@link #isWrittenBook(ItemStack) written book} and, based
	 * on its {@link Generation}, if it is a {@link #isCopy(Generation) copy}.
	 * 
	 * @param itemStack
	 *            the item stack, can be <code>null</code>
	 * @return <code>true</code> if the item stack is a book copy
	 */
	public static boolean isBookCopy(@ReadOnly @Nullable ItemStack itemStack) {
		BookMeta bookMeta = getBookMeta(itemStack);
		if (bookMeta == null) return false; // Not a written book
		return isCopy(bookMeta);
	}

	/**
	 * Checks if the book item with the given {@link BookMeta} is a copy.
	 * <p>
	 * This uses the book's {@link Generation} to determine if it is a {@link #isCopy(Generation)
	 * copy}.
	 * 
	 * @param bookMeta
	 *            the book meta, not <code>null</code>
	 * @return <code>true</code> if the book is a copy
	 */
	public static boolean isCopy(@ReadOnly BookMeta bookMeta) {
		Generation generation = getGeneration(bookMeta);
		return isCopy(generation);
	}

	/**
	 * Copies the given book item.
	 * <p>
	 * The book copy is identical to the given book item, except that it uses the
	 * {@link #getCopyGeneration(Generation) copy generation} of the book's current generation. The
	 * returned book copy has a stack size of {@code 1}.
	 * <p>
	 * The given {@link ItemStack} is required to be a {@link #isCopyableBook(ItemStack) copyable
	 * written book}.
	 * 
	 * @param bookItem
	 *            the book item to copy, not <code>null</code>
	 * @return the book copy, not <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the given item stack is not a copyable book
	 */
	public static ItemStack copyBook(@ReadOnly ItemStack bookItem) {
		Validate.isTrue(isWrittenBook(bookItem), "bookItem is not a written book");

		// Copy the book item, with a stack size of 1:
		ItemStack copy = ItemUtils.copyWithAmount(bookItem, 1);

		// Update the book generation:
		// Getting the old generation from the book copy instead of the original item avoids that we
		// need to retrieve the ItemMeta twice.
		BookMeta copyBookMeta = Unsafe.castNonNull(copy.getItemMeta());
		Generation oldGeneration = getGeneration(copyBookMeta);
		Generation copyGeneration = getCopyGeneration(oldGeneration);
		Validate.notNull(copyGeneration, "bookItem is not copyable");

		copyBookMeta.setGeneration(copyGeneration);
		copy.setItemMeta(copyBookMeta);
		return copy;
	}

	private BookItems() {
	}
}
