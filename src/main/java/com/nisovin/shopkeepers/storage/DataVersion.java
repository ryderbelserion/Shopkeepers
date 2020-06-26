package com.nisovin.shopkeepers.storage;

import com.nisovin.shopkeepers.api.storage.ShopkeeperStorage;

/**
 * Represents the data version used by the {@link ShopkeeperStorage}.
 * <p>
 * This data version is a combination of our own Shopkeepers data version and Minecraft's data version.
 */
public class DataVersion {

	private final int shopkeepersDataVersion;
	private final int minecraftDataVersion;
	private final String combindedDataVersion;

	public DataVersion(int shopkeepersDataVersion, int minecraftDataVersion) {
		this.shopkeepersDataVersion = shopkeepersDataVersion;
		this.minecraftDataVersion = minecraftDataVersion;
		// We concatenate the two data versions to create a String representing the combined data version:
		this.combindedDataVersion = (shopkeepersDataVersion + "|" + minecraftDataVersion);
	}

	/**
	 * Gets Shopkeepers' data version.
	 * 
	 * @return Shopkeepers' data version
	 */
	public int getShopkeepers() {
		return shopkeepersDataVersion;
	}

	/**
	 * Gets Minecraft's data version.
	 * 
	 * @return Minecraft's data version
	 */
	public int getMinecraft() {
		return minecraftDataVersion;
	}

	/**
	 * Gets the combined data version.
	 * 
	 * @return the combined data version
	 */
	public String getCombinded() {
		return combindedDataVersion;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + minecraftDataVersion;
		result = prime * result + shopkeepersDataVersion;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof DataVersion)) return false;
		DataVersion other = (DataVersion) obj;
		if (minecraftDataVersion != other.minecraftDataVersion) return false;
		if (shopkeepersDataVersion != other.shopkeepersDataVersion) return false;
		return true;
	}

	@Override
	public String toString() {
		return combindedDataVersion;
	}
}
