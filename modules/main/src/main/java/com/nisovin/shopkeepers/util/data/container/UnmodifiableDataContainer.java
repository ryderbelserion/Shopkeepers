package com.nisovin.shopkeepers.util.data.container;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An unmodifiable view of another {@link DataContainer}.
 */
public final class UnmodifiableDataContainer extends DelegateDataContainer {

	/**
	 * Creates a new {@link UnmodifiableDataContainer}.
	 * 
	 * @param dataContainer
	 *            the underlying data container, not <code>null</code>
	 */
	public UnmodifiableDataContainer(DataContainer dataContainer) {
		super(dataContainer);
	}

	private UnsupportedOperationException unmodifiableException() {
		return new UnsupportedOperationException("This DataContainer is unmodifiable!");
	}

	@Override
	public void set(String key, @Nullable Object value) {
		throw unmodifiableException();
	}

	@Override
	public void clear() {
		throw unmodifiableException();
	}

	@Override
	public DataContainer asView() {
		return this;
	}
}
