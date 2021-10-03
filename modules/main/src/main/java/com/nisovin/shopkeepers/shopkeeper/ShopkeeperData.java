package com.nisovin.shopkeepers.shopkeeper;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.DelegateDataContainer;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * A wrapper around the {@link DataContainer} that contains a shopkeeper's data.
 * <p>
 * Subclasses, for example for specific {@link ShopType}s, may extend this class with additional data access operations.
 * <p>
 * This wrapper and its subclasses are expected to read and write through to the underlying data container. They shall
 * not copy or derive any state, or at least not rely on this copied state to actually be used, because external data
 * accesses may directly access the underlying data container and thereby bypass any copied state.
 * <p>
 * The {@link #getShopObjectData() ShopObjectData} is lazily setup upon first access and then cached. It reads and
 * writes through to the underlying data container that contains the shop object's data. If the underlying data
 * container for the shop object is dynamically replaced, the previously returned {@link ShopObjectData} will
 * dynamically access the data of the new data container. If the underlying shop object data is removed or no longer
 * represents a valid data container, the previously returned {@link ShopObjectData} will behave like an empty data
 * container, and write operations will result in the missing underlying data container to be newly created, possibly
 * overwriting any other invalid data that may currently be present for the same data key.
 */
public class ShopkeeperData extends DelegateDataContainer {

	/**
	 * Creates a new {@link ShopkeeperData}.
	 * 
	 * @param dataContainer
	 *            the underlying data container that contains the shopkeeper data, or <code>null</code>
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
	protected ShopkeeperData(DataContainer dataContainer) {
		super(dataContainer);
	}

	/**
	 * Gets the shop type identifier.
	 * 
	 * @return the shop type identifier, not <code>null</code> or empty
	 * @throws InvalidDataException
	 *             if the shop type identifier could not be found
	 */
	public final String getShopTypeId() throws InvalidDataException {
		Object data = this.get(DATA_KEY_SHOP_TYPE);
		if (data != null && !(data instanceof String)) {
			throw new InvalidDataException("Invalid shop type id data!");
		}

		String shopTypeId = StringUtils.getNotEmpty((String) data);
		if (shopTypeId == null) {
			throw new InvalidDataException("Missing shop type id!");
		}
		return shopTypeId;
	}

	/**
	 * Gets the {@link ShopType}, derived from the stored {@link #getShopTypeId() shop type identifier}.
	 * 
	 * @return the shop type, not <code>null</code>
	 * @throws InvalidDataException
	 *             if no valid shop type identifier could be found
	 */
	public final AbstractShopType<?> getShopType() throws InvalidDataException {
		String shopTypeId = this.getShopTypeId();
		AbstractShopType<?> shopType = SKShopkeepersPlugin.getInstance().getShopTypeRegistry().get(shopTypeId);
		if (shopType == null) {
			throw new InvalidDataException("Unknown shop type: " + shopTypeId);
		}
		return shopType;
	}

	/**
	 * Gets the {@link ShopObjectData}.
	 * 
	 * @return the shop object data, not <code>null</code>
	 * @throws InvalidDataException
	 *             if no shop object data could be found
	 */
	public final ShopObjectData getShopObjectData() throws InvalidDataException {
		if (shopObjectData == null) {
			// ShopObjectData dynamically accesses the underlying shop object data:
			shopObjectData = ShopObjectData.of(this.getDataValueContainer(DATA_KEY_SHOP_OBJECT));
		}

		// Whenever this method is called to access the shop object data, we freshly check if the underlying shop object
		// data is present and a valid data container:
		Object data = this.get(DATA_KEY_SHOP_OBJECT);
		if (data == null) {
			throw new InvalidDataException("Missing shop object data!");
		}
		if (!DataContainer.isDataContainer(data)) {
			throw new InvalidDataException("Invalid shop object data!");
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
}
