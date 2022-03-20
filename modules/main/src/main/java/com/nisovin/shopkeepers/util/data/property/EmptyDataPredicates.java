package com.nisovin.shopkeepers.util.data.property;

import java.util.List;
import java.util.function.Predicate;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * Default {@link Predicate}s that evaluate if some data object is empty.
 * 
 * @see DataKeyAccessor#emptyDataPredicate(Predicate)
 */
public final class EmptyDataPredicates {

	/**
	 * A {@link Predicate} that checks if the given data is an {@link String#isEmpty() empty
	 * String}.
	 */
	public static final Predicate<Object> EMPTY_STRING = (data) -> {
		return (data instanceof String) && ((String) data).isEmpty();
	};

	/**
	 * A {@link Predicate} that checks if the given data is an {@link DataContainer#isEmpty() empty
	 * DataContainer}.
	 */
	public static final Predicate<Object> EMPTY_CONTAINER = (data) -> {
		DataContainer dataContainer = DataContainer.of(data);
		return dataContainer != null && dataContainer.isEmpty();
	};

	/**
	 * A {@link Predicate} that checks if the given data is an {@link List#isEmpty() empty List}.
	 */
	public static final Predicate<Object> EMPTY_LIST = (data) -> {
		return (data instanceof List) && ((List<?>) data).isEmpty();
	};

	private EmptyDataPredicates() {
	}
}
