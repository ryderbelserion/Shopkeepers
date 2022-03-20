package com.nisovin.shopkeepers.util.data.container.value;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * An unmodifiable view of another {@link DataValue}.
 */
public final class UnmodifiableDataValue extends DelegateDataValue {

	/**
	 * Creates a new {@link UnmodifiableDataValue}.
	 * 
	 * @param dataValue
	 *            the underlying {@link DataValue}, not <code>null</code>
	 */
	public UnmodifiableDataValue(DataValue dataValue) {
		super(dataValue);
	}

	private UnsupportedOperationException unmodifiableException() {
		return new UnsupportedOperationException("This DataValue is unmodifiable!");
	}

	@Override
	public void set(@Nullable Object value) {
		throw unmodifiableException();
	}

	@Override
	public void clear() {
		throw unmodifiableException();
	}

	@Override
	public DataValue asView() {
		return this;
	}
}
