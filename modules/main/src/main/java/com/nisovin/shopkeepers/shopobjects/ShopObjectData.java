package com.nisovin.shopkeepers.shopobjects;

import org.checkerframework.checker.nullness.qual.PolyNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.container.DelegateDataContainer;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A wrapper around the {@link DataContainer} that contains a shop object's data.
 * <p>
 * This wrapper and its subclasses are expected to read and write through to the underlying data
 * container. They shall not copy or derive any state, because external components may directly
 * access the underlying data container and thereby bypass this wrapper.
 */
public class ShopObjectData extends DelegateDataContainer {

	/**
	 * Gets a {@link ShopObjectData} for the given data container.
	 * <p>
	 * If the given data container is already a {@link ShopObjectData}, this returns the given data
	 * container itself. Otherwise, this returns a wrapper around the given data container.
	 * 
	 * @param dataContainer
	 *            the data container that contains the shop object data, or <code>null</code>
	 * @return the {@link ShopObjectData}, or <code>null</code> if the given data container is
	 *         <code>null</code>
	 */
	public static @PolyNull ShopObjectData of(@PolyNull DataContainer dataContainer) {
		if (dataContainer == null) return null;
		if (dataContainer instanceof ShopObjectData) {
			return (ShopObjectData) dataContainer;
		} else {
			return new ShopObjectData(dataContainer);
		}
	}

	/**
	 * Gets a {@link ShopObjectData} for the given data container.
	 * <p>
	 * Unlike {@link #of(DataContainer)}, this method does not accept <code>null</code> as input and
	 * ensures that no <code>null</code> value is returned.
	 * 
	 * @param dataContainer
	 *            the data container that contains the shop object data, not <code>null</code>
	 * @return the {@link ShopObjectData}, not <code>null</code>
	 */
	public static ShopObjectData ofNonNull(DataContainer dataContainer) {
		Validate.notNull(dataContainer, "dataContainer is null");
		return Unsafe.assertNonNull(of(dataContainer));
	}

	/////

	/**
	 * Creates a new {@link ShopObjectData}.
	 * 
	 * @param dataContainer
	 *            the underlying data container, not <code>null</code>
	 */
	protected ShopObjectData(DataContainer dataContainer) {
		super(dataContainer);
	}
}
