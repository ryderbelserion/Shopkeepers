package com.nisovin.shopkeepers.util.data.matcher;

import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.bukkit.configuration.serialization.ConfigurationSerializable;

import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.path.DataPath;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Deeply compares two data objects and provides information on the first mismatch that is found.
 * <p>
 * Subclasses may implement different rules for how the data objects are matched.
 */
public class DataMatcher {

	/**
	 * The result of a matching operation.
	 * <p>
	 * Use {@link Result#isMismatch()} to check if a mismatch was found.
	 */
	public static class Result {

		/**
		 * A shared {@link Result} instance that represents the state of no mismatch having been found.
		 */
		public static final Result NO_MISMATCH = new Result();

		/**
		 * Creates a new {@link Result} that represents a mismatch.
		 * 
		 * @param path
		 *            the path to the mismatching elements, not <code>null</code>
		 * @param leftObject
		 *            the left mismatching object, not <code>null</code>
		 * @param rightObject
		 *            the right mismatching object, not <code>null</code>
		 * @return the {@link Result}, not <code>null</code>
		 */
		public static Result mismatch(DataPath path, Object leftObject, Object rightObject) {
			Validate.notNull(path, "path is null");
			return new Result(path, leftObject, rightObject);
		}

		// These fields are all null if no mismatch was found:
		private DataPath path;
		private final Object leftObject;
		private final Object rightObject;

		/**
		 * Creates a new {@link Result} that represents the state of no mismatch having been found.
		 */
		private Result() {
			this(null, null, null);
		}

		/**
		 * Creates a new {@link Result}.
		 * 
		 * @param path
		 *            the path to the mismatching elements, or <code>null</code> if no mismatch was found
		 * @param leftObject
		 *            the left mismatching object, or <code>null</code> if no mismatch was found
		 * @param rightObject
		 *            the right mismatching object, or <code>null</code> if no mismatch was found
		 */
		public Result(DataPath path, Object leftObject, Object rightObject) {
			if (path == null) {
				Validate.isTrue(leftObject == null, "path is null, but leftObject is not null");
				Validate.isTrue(rightObject == null, "path is null, but rightObject is not null");
			} else {
				Validate.notNull(leftObject, "leftObject is null");
				Validate.notNull(rightObject, "rightObject is null");
			}

			this.path = path;
			this.leftObject = leftObject;
			this.rightObject = rightObject;
		}

		/**
		 * Checks if a mismatch was found.
		 * 
		 * @return <code>true</code> if a mismatch was found
		 */
		public final boolean isMismatch() {
			return (path != null);
		}

		/**
		 * Gets the path to the mismatching data elements.
		 * 
		 * @return the path, or <code>null</code> if no mismatch was found
		 */
		public final DataPath getPath() {
			return path;
		}

		/**
		 * Gets the element on the left side that was found to mismatch a corresponding object on the right side.
		 * 
		 * @return the left mismatching object, or <code>null</code> if no mismatch was found
		 */
		public final Object getLeftObject() {
			return leftObject;
		}

		/**
		 * Gets the element on the right side that was found to mismatch a corresponding object on the left side.
		 * 
		 * @return the right mismatching object, or <code>null</code> if no mismatch was found
		 */
		public final Object getRightObject() {
			return rightObject;
		}
	}

	/**
	 * A {@link DataMatcher} that compares data objects and their recursively contained elements using their respective
	 * {@link Object#equals(Object)} implementations.
	 * <p>
	 * However, unlike {@link Objects#equals(Object, Object)}, this {@link DataMatcher} may consider different types of
	 * {@link DataContainer} data sources as equal.
	 */
	public static final DataMatcher EQUALITY = new DataMatcher();

	/**
	 * A {@link DataMatcher} that behaves like {@link DataMatcher#EQUALITY}, but compares {@link Number}s
	 * {@link MathUtils#fuzzyEquals(double, double) fuzzily}.
	 */
	public static final DataMatcher FUZZY_NUMBERS = new DataMatcher() {
		@Override
		protected Result matchNumbers(DataPath path, Number number1, Number number2) {
			if (MathUtils.fuzzyEquals(number1.doubleValue(), number2.doubleValue())) {
				return Result.NO_MISMATCH;
			} else {
				return Result.mismatch(path, number1, number2);
			}
		}
	};

	/**
	 * Creates a new {@link DataMatcher}.
	 */
	protected DataMatcher() {
	}

	/**
	 * Checks if the given objects match.
	 * 
	 * @param leftObject
	 *            the left data object, can be <code>null</code>
	 * @param rightObject
	 *            the right data object, can be <code>null</code>
	 * @return <code>true</code> if no mismatch is found
	 */
	public final boolean matches(Object leftObject, Object rightObject) {
		return !this.match(leftObject, rightObject).isMismatch();
	}

	/**
	 * Checks if the given objects match.
	 * 
	 * @param leftObject
	 *            the left data object, can be <code>null</code>
	 * @param rightObject
	 *            the right data object, can be <code>null</code>
	 * @return the {@link Result}, not <code>null</code>
	 */
	public final Result match(Object leftObject, Object rightObject) {
		return this.match(DataPath.EMPTY, leftObject, rightObject);
	}

	/**
	 * Checks if the given objects match.
	 * <p>
	 * This checks if the given objects match because they are either the same instance or both <code>null</code>, and
	 * otherwise delegates the matching to {@link #matchObjects(List, Object, Object)}.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftObject
	 *            the left object, can be <code>null</code>
	 * @param rightObject
	 *            the right object, can be <code>null</code>
	 * @return the {@link Result}, not <code>null</code>
	 */
	protected final Result match(DataPath path, Object leftObject, Object rightObject) {
		assert path != null;
		if (leftObject == rightObject) return Result.NO_MISMATCH;
		return this.matchObjects(path, leftObject, rightObject);
	}

	/**
	 * Matches the given objects.
	 * <p>
	 * This is invoked by {@link #match(List, Object, Object)} when the objects are neither the same instance nor both
	 * <code>null</code>.
	 * <p>
	 * This delegates the matching to other methods depending on the types of the objects. Subclasses can override this
	 * or any of the type specific matching methods to implement different matching rules for specific types of objects.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftObject
	 *            the left object, can be <code>null</code>
	 * @param rightObject
	 *            the right object, can be <code>null</code>
	 * @return the {@link Result}, not <code>null</code>
	 */
	protected Result matchObjects(DataPath path, Object leftObject, Object rightObject) {
		assert path != null;
		// Note: We don't check if one of the objects is null here and then indicate a mismatch, because subclasses may
		// want to match null objects with non-null objects.
		// TODO Account for cyclic object references.
		// TODO Take object instances into account rather than only comparing object states. I.e. if the left object has
		// two references to the same object instance, then the right object needs to also have two references to the
		// same corresponding object on the right side.

		Result result = this.checkMatchDataContainers(path, leftObject, rightObject);
		if (result != null) return result;

		result = this.checkMatchLists(path, leftObject, rightObject);
		if (result != null) return result;

		result = this.checkMatchConfigurationSerializables(path, leftObject, rightObject);
		if (result != null) return result;

		result = this.checkMatchNumbers(path, leftObject, rightObject);
		if (result != null) return result;

		return this.matchObjectsExact(path, leftObject, rightObject);
	}

	/**
	 * Checks if the given objects are matching {@link DataContainer}s.
	 * <p>
	 * By default, if the given objects are both {@link DataContainer}s, the actual matching is delegated to
	 * {@link #matchDataContainers(List, DataContainer, DataContainer)}.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftObject
	 *            the left object, can be <code>null</code>
	 * @param rightObject
	 *            the right object, can be <code>null</code>
	 * @return a {@link Result}, or <code>null</code> if undecided (by default, if none of the objects is a
	 *         {@link DataContainer})
	 */
	protected Result checkMatchDataContainers(DataPath path, Object leftObject, Object rightObject) {
		assert path != null;
		boolean leftIsDataContainer = DataContainer.isDataContainer(leftObject);
		boolean rightIsDataContainer = DataContainer.isDataContainer(rightObject);
		if (leftIsDataContainer && rightIsDataContainer) {
			return this.matchDataContainers(path, DataContainer.of(leftObject), DataContainer.of(rightObject));
		} else if (leftIsDataContainer ^ rightIsDataContainer) {
			return Result.mismatch(path, leftObject, rightObject);
		} else {
			assert !leftIsDataContainer && !rightIsDataContainer;
			return null; // Undecided
		}
	}

	/**
	 * Matches the given {@link DataContainer}s.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftDataContainer
	 *            the left data container, not <code>null</code>
	 * @param rightDataContainer
	 *            the right data container, not <code>null</code>
	 * @return the {@link Result}, not <code>null</code>
	 */
	protected Result matchDataContainers(DataPath path, DataContainer leftDataContainer, DataContainer rightDataContainer) {
		assert path != null && leftDataContainer != null && rightDataContainer != null;
		Map<String, ?> leftValues = leftDataContainer.getValues();
		Map<String, ?> rightValues = rightDataContainer.getValues();
		if (leftValues.size() != rightValues.size()) {
			return Result.mismatch(path, leftValues, rightValues);
		}

		Iterator<? extends Entry<String, ?>> leftValuesIterator = leftValues.entrySet().iterator();
		while (leftValuesIterator.hasNext()) {
			Entry<String, ?> entry = leftValuesIterator.next();
			String key = entry.getKey();
			Object leftValue = entry.getValue();
			assert key != null && leftValue != null;

			Object rightValue = rightValues.get(key); // Null if there is no corresponding mapping
			DataPath entryPath = path.append(key);
			Result entryMatchResult = this.match(entryPath, leftValue, rightValue);
			if (entryMatchResult.isMismatch()) {
				return entryMatchResult;
			}
		}
		return Result.NO_MISMATCH;
	}

	/**
	 * Checks if the given objects are matching {@link List}s.
	 * <p>
	 * By default, if the given objects are both {@link List}s, the actual matching is delegated to
	 * {@link #matchLists(List, List, List)}.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftObject
	 *            the left object, can be <code>null</code>
	 * @param rightObject
	 *            the right object, can be <code>null</code>
	 * @return a {@link Result}, or <code>null</code> if undecided (by default, if none of the objects is a list)
	 */
	protected Result checkMatchLists(DataPath path, Object leftObject, Object rightObject) {
		assert path != null;
		boolean leftIsList = (leftObject instanceof List);
		boolean rightIsList = (rightObject instanceof List);
		if (leftIsList && rightIsList) {
			return this.matchLists(path, (List<?>) leftObject, (List<?>) rightObject);
		} else if (leftIsList ^ rightIsList) {
			return Result.mismatch(path, leftObject, rightObject);
		} else {
			assert !leftIsList && !rightIsList;
			return null; // Undecided
		}
	}

	/**
	 * Matches the given {@link List}s.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftList
	 *            the left list, not <code>null</code>
	 * @param rightList
	 *            the right list, not <code>null</code>
	 * @return the {@link Result}, not <code>null</code>
	 */
	protected Result matchLists(DataPath path, List<?> leftList, List<?> rightList) {
		assert path != null && leftList != null && rightList != null;
		// Getting the list sizes is assumed to usually be fast:
		if (leftList.size() != rightList.size()) {
			return Result.mismatch(path, leftList, rightList);
		}

		ListIterator<?> leftIterator = leftList.listIterator();
		ListIterator<?> rightIterator = rightList.listIterator();
		int index = 0;
		while (leftIterator.hasNext() && rightIterator.hasNext()) {
			Object leftElement = leftIterator.next();
			Object rightElement = rightIterator.next();
			DataPath elementPath = path.append(String.valueOf(index));
			Result elementMatchResult = this.match(elementPath, leftElement, rightElement);
			if (elementMatchResult.isMismatch()) {
				return elementMatchResult;
			}
			index++;
		}
		return Result.NO_MISMATCH;
	}

	/**
	 * Checks if the given objects are matching {@link ConfigurationSerializables}s.
	 * <p>
	 * By default, this {@link ConfigUtils#serialize(ConfigurationSerializable) serializes} each
	 * {@link ConfigurationSerializable} object and then tries to match them as
	 * {@link #checkMatchDataContainers(DataPath, Object, Object) DataContainers}. We also allow a
	 * {@link ConfigurationSerializable} object on one side to match a {@link DataContainer} with matching contents
	 * (including the serialized type alias) on the other side.
	 * <p>
	 * Matching the serialized states of the given objects, instead of comparing them directly via
	 * {@link Object#equals(Object)}, ensures that we also apply our custom matching rules to the internal data of these
	 * objects.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftObject
	 *            the left object, can be <code>null</code>
	 * @param rightObject
	 *            the right object, can be <code>null</code>
	 * @return a {@link Result}, or <code>null</code> if undecided
	 */
	protected Result checkMatchConfigurationSerializables(DataPath path, Object leftObject, Object rightObject) {
		assert path != null;
		if (leftObject instanceof ConfigurationSerializable) {
			// Note: This also includes and compares the serialized type aliases of the given objects.
			leftObject = ConfigUtils.serialize((ConfigurationSerializable) leftObject);
		}
		if (rightObject instanceof ConfigurationSerializable) {
			rightObject = ConfigUtils.serialize((ConfigurationSerializable) rightObject);
		}

		return this.checkMatchDataContainers(path, leftObject, rightObject);
	}

	/**
	 * Checks if the given objects are matching {@link Number}s.
	 * <p>
	 * By default, if the given objects are both {@link Number}s, the actual matching is delegated to
	 * {@link #matchNumbers(List, Number, Number)}.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftObject
	 *            the left object, can be <code>null</code>
	 * @param rightObject
	 *            the right object, can be <code>null</code>
	 * @return a {@link Result}, or <code>null</code> if undecided (by default, if none of the objects is a number)
	 */
	protected Result checkMatchNumbers(DataPath path, Object leftObject, Object rightObject) {
		assert path != null;
		boolean leftIsNumber = (leftObject instanceof Number);
		boolean rightIsNumber = (rightObject instanceof Number);
		if (leftIsNumber && rightIsNumber) {
			return this.matchNumbers(path, (Number) leftObject, (Number) rightObject);
		} else if (leftIsNumber ^ rightIsNumber) {
			return Result.mismatch(path, leftObject, rightObject);
		} else {
			assert !leftIsNumber && !rightIsNumber;
			return null; // Undecided
		}
	}

	/**
	 * Matches the given numbers.
	 * <p>
	 * By default, the numbers have to perfectly match, in both type and value.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftNumber
	 *            the left number, not <code>null</code>
	 * @param rightNumber
	 *            the right number, not <code>null</code>
	 * @return the {@link Result}, not <code>null</code>
	 */
	protected Result matchNumbers(DataPath path, Number leftNumber, Number rightNumber) {
		assert path != null && leftNumber != null && rightNumber != null;
		return this.matchObjectsExact(path, leftNumber, rightNumber);
	}

	/**
	 * Matches the given objects using {@link Objects#equals(Object, Object)}.
	 * 
	 * @param path
	 *            the current path, not <code>null</code> or empty
	 * @param leftObject
	 *            the left object, can be <code>null</code>
	 * @param rightObject
	 *            the right object, can be <code>null</code>
	 * @return the {@link Result}, not <code>null</code>
	 */
	protected final Result matchObjectsExact(DataPath path, Object leftObject, Object rightObject) {
		assert path != null;
		if (Objects.equals(leftObject, rightObject)) {
			return Result.NO_MISMATCH;
		} else {
			return Result.mismatch(path, leftObject, rightObject);
		}
	}
}
