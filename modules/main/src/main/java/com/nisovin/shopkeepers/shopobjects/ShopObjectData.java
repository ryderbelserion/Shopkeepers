package com.nisovin.shopkeepers.shopobjects;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DelegateDataContainer;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * A wrapper around the {@link DataContainer} that contains a shop object's data.
 * <p>
 * Subclasses, for example for specific {@link ShopObjectType}s, may extend this class with additional data access
 * operations.
 * <p>
 * This wrapper and its subclasses are expected to read and write through to the underlying data container. They shall
 * not copy or derive any state, or at least not rely on this copied state to actually be used, because external data
 * accesses may directly access the underlying data container and thereby bypass any copied state.
 */
public class ShopObjectData extends DelegateDataContainer {

	/**
	 * Creates a new {@link ShopObjectData}.
	 * 
	 * @param dataContainer
	 *            the underlying data container that contains the shop object data, or <code>null</code>
	 * @return the shop object data, or <code>null</code> if the given data container is <code>null</code>
	 */
	public static ShopObjectData of(DataContainer dataContainer) {
		if (dataContainer == null) return null;
		return new ShopObjectData(dataContainer);
	}

	/////

	public static final String DATA_KEY_SHOP_OBJECT_TYPE = "type";

	/**
	 * Creates a new {@link ShopObjectData}.
	 * 
	 * @param dataContainer
	 *            the underlying container that contains the shop object data, not <code>null</code>
	 */
	protected ShopObjectData(DataContainer dataContainer) {
		super(dataContainer);
	}

	/**
	 * Gets the shop object type identifier.
	 * 
	 * @return the shop object type identifier, not <code>null</code> or empty
	 * @throws InvalidDataException
	 *             if the shop object type identifier could not be found
	 */
	public final String getShopObjectTypeId() throws InvalidDataException {
		String objectTypeId = this.getString(DATA_KEY_SHOP_OBJECT_TYPE);
		if (StringUtils.isEmpty(objectTypeId)) {
			throw new InvalidDataException("Missing shop object type id!");
		}
		return objectTypeId;
	}

	/**
	 * Gets the {@link ShopObjectType}, derived from the stored {@link #getShopObjectTypeId() shop object type
	 * identifier}.
	 * 
	 * @return the shop object type, not <code>null</code>
	 * @throws InvalidDataException
	 *             if no valid shop object type identifier could be found
	 */
	public final AbstractShopObjectType<?> getShopObjectType() throws InvalidDataException {
		String objectTypeId = this.getShopObjectTypeId();
		assert objectTypeId != null;
		AbstractShopObjectType<?> objectType = SKShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().get(objectTypeId);
		if (objectType == null) {
			throw new InvalidDataException("Invalid shop object type: " + objectTypeId);
		}
		return objectType;
	}
}
