package com.nisovin.shopkeepers.shopkeeper;

import org.checkerframework.checker.nullness.qual.PolyNull;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.util.data.container.DataContainer;
import com.nisovin.shopkeepers.util.data.container.DelegateDataContainer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A wrapper around the {@link DataContainer} that contains a shopkeeper's data.
 * <p>
 * This wrapper and its subclasses are expected to read and write through to the underlying data
 * container. They shall not copy or derive any state, because external components may directly
 * access the underlying data container and thereby bypass this wrapper.
 */
public class ShopkeeperData extends DelegateDataContainer {

	/**
	 * Gets a {@link ShopkeeperData} for the given data container.
	 * <p>
	 * If the given data container is already a {@link ShopkeeperData}, this returns the given data
	 * container itself. Otherwise, this returns a wrapper around the given data container.
	 * 
	 * @param dataContainer
	 *            the data container that contains the shopkeeper data, or <code>null</code>
	 * @return the shopkeeper data, or <code>null</code> if the given data container is
	 *         <code>null</code>
	 */
	public static @PolyNull ShopkeeperData of(@PolyNull DataContainer dataContainer) {
		if (dataContainer == null) return null;
		if (dataContainer instanceof ShopkeeperData) {
			return (ShopkeeperData) dataContainer;
		} else {
			return new ShopkeeperData(dataContainer);
		}
	}

	/**
	 * Gets a {@link ShopkeeperData} for the given data container.
	 * <p>
	 * Unlike {@link #of(DataContainer)}, this method does not accept <code>null</code> as input and
	 * ensures that no <code>null</code> value is returned.
	 * 
	 * @param dataContainer
	 *            the data container that contains the shopkeeper data, not <code>null</code>
	 * @return the shopkeeper data, not <code>null</code>
	 */
	public static ShopkeeperData ofNonNull(DataContainer dataContainer) {
		Validate.notNull(dataContainer, "dataContainer is null");
		return Unsafe.assertNonNull(of(dataContainer));
	}

	/////

	/**
	 * Creates a new {@link ShopkeeperData}.
	 * 
	 * @param dataContainer
	 *            the underlying data container, not <code>null</code>
	 */
	protected ShopkeeperData(DataContainer dataContainer) {
		super(dataContainer);
	}

	/**
	 * Applies migrations to this shopkeeper data.
	 * 
	 * @param logPrefix
	 *            a context specific log prefix, can be empty, not <code>null</code>
	 * @return <code>true</code> if the data has changed as a result of these migrations
	 * @throws InvalidDataException
	 *             if the data is invalid and cannot be migrated
	 * @see ShopkeeperDataMigrator#migrate(ShopkeeperData, String)
	 */
	public final boolean migrate(String logPrefix) throws InvalidDataException {
		return ShopkeeperDataMigrator.migrate(this, logPrefix);
	}
}
