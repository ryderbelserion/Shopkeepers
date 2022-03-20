package com.nisovin.shopkeepers.tradelog.data;

import java.util.Objects;
import java.util.UUID;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * An immutable snapshot of the information about a shop.
 * <p>
 * This information is not necessarily up-to-date, but represents the shop's information at a
 * certain point in time (e.g. the shop might have been modified or even deleted).
 */
public class ShopRecord {

	/**
	 * Creates a {@link ShopRecord} for the given shopkeeper.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper
	 * @return the shop record
	 */
	public static ShopRecord of(Shopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		UUID shopUniqueId = shopkeeper.getUniqueId();
		String shopTypeId = shopkeeper.getType().getIdentifier();
		PlayerShopkeeper playerShop = (shopkeeper instanceof PlayerShopkeeper) ? (PlayerShopkeeper) shopkeeper : null;
		PlayerRecord owner = null;
		if (playerShop != null) {
			owner = PlayerRecord.of(playerShop.getOwnerUUID(), playerShop.getOwnerName());
		}
		String shopName = shopkeeper.getName(); // Can be empty
		String worldName = shopkeeper.getWorldName();
		int x = shopkeeper.getX();
		int y = shopkeeper.getY();
		int z = shopkeeper.getZ();
		return new ShopRecord(shopUniqueId, shopTypeId, owner, shopName, worldName, x, y, z);
	}

	private final UUID uniqueId;
	private final String typeId;
	// The owner might no longer match the current owner of the shopkeeper.
	private final @Nullable PlayerRecord owner; // Can be null (e.g. for admin shops)
	private final String name; // Can be empty
	private final @Nullable String worldName; // Can be null for virtual shops, not empty
	// Coordinates are all 0 for virtual shops:
	private final int x;
	private final int y;
	private final int z;

	public ShopRecord(
			UUID shopUniqueId,
			String shopTypeId,
			@Nullable PlayerRecord owner,
			String shopName,
			@Nullable String worldName,
			int x,
			int y,
			int z
	) {
		Validate.notNull(shopUniqueId, "shopUniqueId is null");
		Validate.notNull(shopTypeId, "shopTypeId is null");
		Validate.notNull(shopName, "shopName is null"); // Can be empty
		if (worldName != null) {
			Validate.notEmpty(worldName, "worldName is empty");
		}
		this.uniqueId = shopUniqueId;
		this.typeId = shopTypeId;
		this.owner = owner;
		this.name = shopName;
		this.worldName = worldName;
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/**
	 * Gets the shop's unique id.
	 * 
	 * @return the unique shop id
	 */
	public UUID getUniqueId() {
		return uniqueId;
	}

	/**
	 * Gets the shop's type id.
	 * 
	 * @return the shop's type id
	 */
	public String getTypeId() {
		return typeId;
	}

	/**
	 * Gets the shop's name.
	 * 
	 * @return the shop's name, can be empty
	 */
	public String getName() {
		return name;
	}

	/**
	 * Gets the shop's owner.
	 * 
	 * @return the owner, can be <code>null</code>
	 */
	public @Nullable PlayerRecord getOwner() {
		return owner;
	}

	/**
	 * Gets the shop's world name.
	 * 
	 * @return the world name, can <code>null</code> for virtual shops, not empty
	 */
	public @Nullable String getWorldName() {
		return worldName;
	}

	/**
	 * Gets the x coordinate.
	 * 
	 * @return the x coordinate
	 */
	public int getX() {
		return x;
	}

	/**
	 * Gets the y coordinate.
	 * 
	 * @return the y coordinate
	 */
	public int getY() {
		return y;
	}

	/**
	 * Gets the z coordinate.
	 * 
	 * @return the z coordinate
	 */
	public int getZ() {
		return z;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ShopRecord [uniqueId=");
		builder.append(uniqueId);
		builder.append(", typeId=");
		builder.append(typeId);
		builder.append(", owner=");
		builder.append(owner);
		builder.append(", name=");
		builder.append(name);
		builder.append(", worldName=");
		builder.append(worldName);
		builder.append(", x=");
		builder.append(x);
		builder.append(", y=");
		builder.append(y);
		builder.append(", z=");
		builder.append(z);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uniqueId.hashCode();
		result = prime * result + typeId.hashCode();
		result = prime * result + Objects.hashCode(owner);
		result = prime * result + name.hashCode();
		result = prime * result + Objects.hashCode(worldName);
		result = prime * result + x;
		result = prime * result + y;
		result = prime * result + z;
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ShopRecord)) return false;
		ShopRecord other = (ShopRecord) obj;
		if (!uniqueId.equals(other.uniqueId)) return false;
		if (!typeId.equals(other.typeId)) return false;
		if (!Objects.equals(owner, other.owner)) return false;
		if (!name.equals(other.name)) return false;
		if (!Objects.equals(worldName, other.worldName)) return false;
		if (x != other.x) return false;
		if (y != other.y) return false;
		if (z != other.z) return false;
		return true;
	}
}
