package com.nisovin.shopkeepers.util.data.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Immutable representation of the location of an element inside the hierarchical structure of some
 * data object.
 * <p>
 * Data objects are often recursively composed of branching elements such as {@link DataContainer}s
 * and {@link List}s. Each segment of a {@link DataPath} represents a 'level of depth' in this
 * hierarchical structure.
 * <p>
 * There is no fixed definition for what constitutes a 'level of depth' for different types of data
 * objects, or how a {@link DataPath} represents these levels. Currently,
 * {@link DataPath#resolve(Object)} supports branching for {@link DataContainer}s and {@link List}s
 * and uses the element key or the String representation of the list index for the respective
 * element lookup.
 */
public final class DataPath {

	// TODO Add a user-friendly string representation and parsing of data paths, maybe using 'dot'
	// as segment separator (and blocking it from being used as key in DataContainers?)
	// TODO Change the representation of list elements from "a.1.c" to "a[1].c"? Introduce segment
	// types ('queries') that can only be resolved against specific types of data objects?

	/**
	 * An empty {@link DataPath}.
	 */
	public static final DataPath EMPTY = new DataPath();

	private final List<? extends String> segments; // Not null, can be empty, immutable

	private DataPath() {
		this(Collections.emptyList(), true);
	}

	/**
	 * Creates a new {@link DataPath} with a single segment.
	 * 
	 * @param segment
	 *            the segment, not <code>null</code> or empty
	 */
	public DataPath(String segment) {
		this(
				Collections.singletonList(Validate.notEmpty(segment, "segment is null or empty")),
				true
		);
	}

	/**
	 * Creates a new {@link DataPath} that consists of the given segments.
	 * <p>
	 * Each segment needs to be not <code>null</code> or empty.
	 * 
	 * @param segments
	 *            the segments, not <code>null</code>
	 */
	public DataPath(List<? extends String> segments) {
		this(segments, false);
	}

	private DataPath(List<? extends String> segments, boolean assumeSafe) {
		if (assumeSafe) {
			this.segments = segments;
		} else {
			Validate.notNull(segments, "segments is null");
			segments.forEach(segment -> {
				Validate.notEmpty(segment, "one of the segments is null or empty");
			});
			this.segments = Collections.unmodifiableList(new ArrayList<>(segments));
		}
	}

	/**
	 * Gets the immutable list of segments.
	 * 
	 * @return the segments, not <code>null</code>, can be empty
	 */
	public List<? extends String> getSegments() {
		return segments;
	}

	/**
	 * Gets the number of segments in this path.
	 * 
	 * @return the number of segments
	 */
	public int getLength() {
		return segments.size();
	}

	/**
	 * Checks if this path is empty, i.e. has a length of {@code 0}.
	 * 
	 * @return <code>true</code> if this path is empty
	 */
	public boolean isEmpty() {
		return (this.getLength() == 0);
	}

	/**
	 * Gets the first segment of this path.
	 * 
	 * @return the first segment, or <code>null</code> if this path is empty
	 */
	public @Nullable String getFirstSegment() {
		return this.isEmpty() ? null : segments.get(0);
	}

	/**
	 * Gets the last segment of this path.
	 * 
	 * @return the last segment, or <code>null</code> if this path is empty
	 */
	public @Nullable String getLastSegment() {
		return this.isEmpty() ? null : segments.get(segments.size() - 1);
	}

	/**
	 * Gets a new path that consists of the segments of this path with the last segment removed.
	 * 
	 * @return the parent path, or <code>null</code> if this path is empty
	 */
	public @Nullable DataPath getParentPath() {
		return this.isEmpty() ? null : this.getSubPath(0, segments.size() - 1);
	}

	/**
	 * Gets a new path that consists of the segments of this path with the first segment removed.
	 * 
	 * @return the child path, or <code>null</code> if this path is empty
	 */
	public @Nullable DataPath getChildPath() {
		return this.isEmpty() ? null : this.getSubPath(1, segments.size());
	}

	/**
	 * Gets a new path that consists of the segments between the specified {@code fromIndex}
	 * (inclusive) and {@code toIndex} (exclusive).
	 * <p>
	 * If {@code fromIndex} and {@code toIndex} are equal, the returned path is empty.
	 * 
	 * @param fromIndex
	 *            the index of the first segment, inclusive
	 * @param toIndex
	 *            the index of the last segment, exclusive
	 * @return the sub path, not <code>null</code>
	 * @throw IndexOutOfBoundsException if {@code (fromIndex < 0)} or {@code (toIndex > length)} or
	 *        {@code (fromIndex > toIndex)}
	 */
	public DataPath getSubPath(int fromIndex, int toIndex) {
		return new DataPath(segments.subList(fromIndex, toIndex), true);
	}

	/**
	 * Gets a new path that combines the segments of this path with the given segment.
	 * 
	 * @param segment
	 *            the segment to append, not <code>null</code> or empty
	 * @return the new path, not <code>null</code>
	 */
	public DataPath append(String segment) {
		Validate.notEmpty(segment, "segment is null or empty");
		List<? extends String> newSegments = CollectionUtils.unmodifiableCopyAndAdd(
				segments, segment
		);
		return new DataPath(newSegments, true);
	}

	/**
	 * Gets a new path that combines the segments of both this and the given path.
	 * 
	 * @param path
	 *            the path to append, not <code>null</code>
	 * @return the new path, not <code>null</code>
	 */
	public DataPath append(DataPath path) {
		Validate.notNull(path, "path is null");
		if (this.isEmpty()) return path;
		if (path.isEmpty()) return this;
		List<? extends String> newSegments = CollectionUtils.unmodifiableCopyAndAddAll(
				segments, path.getSegments()
		);
		return new DataPath(newSegments, true);
	}

	/**
	 * Resolves this path against the given data object.
	 * <p>
	 * This returns the object pointed to by this path relative to the given object by traversing
	 * the hierarchical structure of the given object as specified by this path.
	 * 
	 * @param dataObject
	 *            the data object to resolve this path against, can be <code>null</code>
	 * @return the resolved data object, or <code>null</code> if no object is found for this path
	 */
	public @Nullable Object resolve(@Nullable Object dataObject) {
		if (dataObject == null) return null;
		if (this.isEmpty()) return dataObject;

		String nextSegment = Unsafe.assertNonNull(this.getFirstSegment());
		Object nextElement = null;

		DataContainer dataContainer = DataContainer.of(dataObject);
		if (dataContainer != null) {
			nextElement = dataContainer.get(nextSegment);
		} else if (dataObject instanceof List) {
			List<?> list = (List<?>) dataObject;
			Integer index = ConversionUtils.parseInt(nextSegment);
			if (index != null) {
				nextElement = list.get(index.intValue());
			}
		} else {
			// TODO Allow the registration of DataPath resolvers for custom types of data objects?
			return null;
		}

		if (nextElement != null) {
			DataPath childPath = Unsafe.assertNonNull(this.getChildPath());
			return childPath.resolve(nextElement);
		} else {
			return null;
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + segments.hashCode();
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof DataPath)) return false;
		DataPath other = (DataPath) obj;
		if (!segments.equals(other.segments)) return false;
		return true;
	}

	@Override
	public String toString() {
		return segments.toString();
	}
}
