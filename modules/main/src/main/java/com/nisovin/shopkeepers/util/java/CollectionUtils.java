package com.nisovin.shopkeepers.util.java;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class CollectionUtils {

	private CollectionUtils() {
	}

	/**
	 * Replaces the first occurrence of the given element inside the given list.
	 * 
	 * @param <E>
	 *            the element type
	 * @param list
	 *            the list, not <code>null</code>
	 * @param element
	 *            the element to replace
	 * @param replacement
	 *            the replacement
	 * @return <code>true</code> if the element has been found inside the list
	 */
	public static <E> boolean replace(List<E> list, E element, E replacement) {
		if (list instanceof RandomAccess) { // Also checks for null
			int index = list.indexOf(element);
			if (index != -1) {
				list.set(index, replacement);
				return true;
			}
			return false;
		} else {
			Validate.notNull(list, "list is null");
			ListIterator<E> iterator = list.listIterator();
			while (iterator.hasNext()) {
				E next = iterator.next();
				if (Objects.equals(element, next)) {
					iterator.set(replacement);
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * Creates a fixed-sized list backed by the given array.
	 * <p>
	 * Unlike {@link Arrays#asList(Object...)} this returns an {@link Collections#emptyList() empty list} if the given
	 * array is <code>null</code>.
	 * 
	 * @param <E>
	 *            the element type
	 * @param array
	 *            the array
	 * @return the list backed by the given array, or an empty list if the given array is <code>null</code>
	 */
	@SafeVarargs
	public static <E> List<E> asList(E... array) {
		return (array == null) ? Collections.emptyList() : Arrays.asList(array);
	}

	/**
	 * Sorts the given list using the given {@link Comparator} and then returns the list.
	 * 
	 * @param <E>
	 *            the element type
	 * @param <L>
	 *            the type of the list
	 * @param list
	 *            the list to sort
	 * @param comparator
	 *            the comparator
	 * @return the given list sorted
	 * @see List#sort(Comparator)
	 */
	public static <E, L extends List<? extends E>> L sort(L list, Comparator<? super E> comparator) {
		Validate.notNull(list, "list");
		Validate.notNull(comparator, "comparator");
		list.sort(comparator);
		return list;
	}

	/**
	 * Adds the given elements to the given collection and then returns the collection.
	 * 
	 * @param <E>
	 *            the element type
	 * @param <C>
	 *            the type of the collection
	 * @param collection
	 *            the collection
	 * @param toAdd
	 *            the elements to add
	 * @return the given collection with the elements added
	 * @see Collection#addAll(Collection)
	 */
	public static <E, C extends Collection<? super E>> C addAll(C collection, Collection<? extends E> toAdd) {
		Validate.notNull(collection, "collection");
		Validate.notNull(toAdd, "toAdd");
		collection.addAll(toAdd);
		return collection;
	}

	/**
	 * Searches through the given {@link Iterable} for an element that is accepted by the given {@link Predicate}.
	 * 
	 * @param <E>
	 *            the element type
	 * @param iterable
	 *            the elements to search through
	 * @param predicate
	 *            the Predicate, not <code>null</code>
	 * @return the first found element accepted by the Predicate, or <code>null</code> if either no such element was
	 *         found, or if the Predicate accepted a <code>null</code> element
	 */
	public static <E> E findFirst(Iterable<E> iterable, Predicate<? super E> predicate) {
		Validate.notNull(predicate, "predicate is null");
		for (E element : iterable) {
			if (predicate.test(element)) {
				return element;
			}
		}
		return null;
	}

	/**
	 * Checks if the given {@link Iterable} contains an element that is accepted by the given {@link Predicate}.
	 * 
	 * @param <E>
	 *            the element type
	 * @param iterable
	 *            the elements to search through
	 * @param predicate
	 *            the Predicate, not <code>null</code>
	 * @return <code>true</code> if an element is found that is accepted by the given Predicate
	 */
	public static <E> boolean contains(Iterable<E> iterable, Predicate<? super E> predicate) {
		Validate.notNull(predicate, "predicate is null");
		for (E element : iterable) {
			if (predicate.test(element)) {
				return true;
			}
		}
		return false;
	}

	// Note: Doesn't work for primitive arrays.
	@SafeVarargs
	public static <T> T[] concat(T[] array1, T... array2) {
		if (array1 == null) return array2;
		if (array2 == null) return array1;

		int length1 = array1.length;
		int length2 = array2.length;
		T[] result = Arrays.copyOf(array1, length1 + length2);
		System.arraycopy(array2, 0, result, length1, length2);
		return result;
	}

	public static <T> Stream<T> stream(Iterable<T> iterable) {
		if (iterable instanceof Collection) {
			return ((Collection<T>) iterable).stream();
		} else {
			return StreamSupport.stream(iterable.spliterator(), false);
		}
	}

	// Note: The returned Iterable can only be iterated once!
	public static <T> Iterable<T> toIterable(Stream<T> stream) {
		return stream::iterator;
	}
}
