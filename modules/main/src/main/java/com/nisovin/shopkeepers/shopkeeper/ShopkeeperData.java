package com.nisovin.shopkeepers.shopkeeper;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DelegateDataContainer;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * A wrapper around the {@link DataContainer} that contains a shopkeeper's data.
 * <p>
 * The {@link #getShopObjectData() ShopObjectData} is lazily setup upon first access and then cached. If the underlying
 * data container for the shop object is dynamically replaced, this change might not be reflected by this
 * {@link ShopkeeperData}.
 */
public class ShopkeeperData extends DelegateDataContainer {

	/**
	 * Creates a new {@link ShopkeeperData}.
	 * 
	 * @param dataContainer
	 *            the underlying container that contains the shopkeeper data
	 * @return the shopkeeper data, or <code>null</code> if the given data container is <code>null</code>
	 */
	public static ShopkeeperData of(DataContainer dataContainer) {
		if (dataContainer == null) return null;
		return new ShopkeeperData(dataContainer);
	}

	/////

	public static final String DATA_KEY_SHOP_TYPE = "type";
	public static final String DATA_KEY_SHOP_OBJECT = "object";

	private ShopObjectData shopObjectData = null; // Lazily setup and then cached

	/**
	 * Creates a new {@link ShopkeeperData}.
	 * 
	 * @param dataContainer
	 *            the underlying container that contains the shopkeeper data, not <code>null</code>
	 */
	private ShopkeeperData(DataContainer dataContainer) {
		super(dataContainer);
	}

	/**
	 * Gets the shop type identifier.
	 * 
	 * @return the shop type identifier, not <code>null</code> or empty
	 * @throws ShopkeeperCreateException
	 *             if the shop type identifier could not be found
	 */
	public final String getShopTypeId() throws ShopkeeperCreateException {
		String shopTypeId = this.getString(DATA_KEY_SHOP_TYPE);
		if (StringUtils.isEmpty(shopTypeId)) {
			throw new ShopkeeperCreateException("Missing shop type id!");
		}
		return shopTypeId;
	}

	/**
	 * Gets the {@link ShopType}, derived from the stored {@link #getShopTypeId() shop type identifier}.
	 * 
	 * @return the shop type, not <code>null</code>
	 * @throws ShopkeeperCreateException
	 *             if no valid shop type identifier could be found
	 */
	public final AbstractShopType<?> getShopType() throws ShopkeeperCreateException {
		String shopTypeId = this.getShopTypeId();
		AbstractShopType<?> shopType = SKShopkeepersPlugin.getInstance().getShopTypeRegistry().get(shopTypeId);
		if (shopType == null) {
			throw new ShopkeeperCreateException("Unknown shop type: " + shopTypeId);
		}
		return shopType;
	}

	/**
	 * Gets the {@link ShopObjectData}.
	 * 
	 * @return the shop object data, not <code>null</code>
	 * @throws ShopkeeperCreateException
	 *             if no shop object data could be found
	 */
	public final ShopObjectData getShopObjectData() throws ShopkeeperCreateException {
		if (shopObjectData == null) {
			DataContainer shopObjectDataContainer = this.getContainer(DATA_KEY_SHOP_OBJECT);
			if (shopObjectDataContainer == null) {
				throw new ShopkeeperCreateException("Missing shop object data!");
			}
			shopObjectData = ShopObjectData.of(shopObjectDataContainer);
		}
		return shopObjectData;
	}

	/**
	 * Creates an empty shop object data entry, replacing any previous shop object data that may have been present.
	 * 
	 * @return the new empty shop object data, not <code>null</code>
	 */
	public final ShopObjectData createEmptyShopObjectData() {
		// This replaces any previously setup shop object data:
		shopObjectData = ShopObjectData.of(this.createContainer(DATA_KEY_SHOP_OBJECT));
		return shopObjectData;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ShopkeeperData [data=");
		builder.append(dataContainer);
		builder.append("]");
		return builder.toString();
	}
}
