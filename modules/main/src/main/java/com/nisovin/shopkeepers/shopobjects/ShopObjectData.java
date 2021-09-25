package com.nisovin.shopkeepers.shopobjects;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DelegateDataContainer;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * A wrapper around the {@link DataContainer} that contains a shop object's data.
 */
public class ShopObjectData extends DelegateDataContainer {

	/**
	 * Creates a new {@link ShopObjectData}.
	 * 
	 * @param dataContainer
	 *            the underlying container that contains the shop object data
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
	private ShopObjectData(DataContainer dataContainer) {
		super(dataContainer);
	}

	/**
	 * Gets the shop object type identifier.
	 * 
	 * @return the shop object type identifier, not <code>null</code> or empty
	 * @throws ShopkeeperCreateException
	 *             if the shop object type identifier could not be found
	 */
	public final String getShopObjectTypeId() throws ShopkeeperCreateException {
		String objectTypeId = this.getString(DATA_KEY_SHOP_OBJECT_TYPE);
		if (StringUtils.isEmpty(objectTypeId)) {
			throw new ShopkeeperCreateException("Missing shop object type id!");
		}
		return objectTypeId;
	}

	/**
	 * Gets the {@link ShopObjectType}, derived from the stored {@link #getShopObjectTypeId() shop object type
	 * identifier}.
	 * 
	 * @return the shop object type, not <code>null</code>
	 * @throws ShopkeeperCreateException
	 *             if no valid shop object type identifier could be found
	 */
	public final AbstractShopObjectType<?> getShopObjectType() throws ShopkeeperCreateException {
		String objectTypeId = this.getShopObjectTypeId();
		assert objectTypeId != null;
		AbstractShopObjectType<?> objectType = SKShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().get(objectTypeId);
		if (objectType == null) {
			throw new ShopkeeperCreateException("Invalid shop object type: " + objectTypeId);
		}
		return objectType;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ShopObjectData [data=");
		builder.append(dataContainer);
		builder.append("]");
		return builder.toString();
	}
}
