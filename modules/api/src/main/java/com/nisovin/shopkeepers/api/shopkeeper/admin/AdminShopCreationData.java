package com.nisovin.shopkeepers.api.shopkeeper.admin;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * Shop creation data specific for admin shops.
 */
public class AdminShopCreationData extends ShopCreationData {

	private static AdminShopType<?> toAdminShopType(ShopType<?> shopType) {
		Preconditions.checkArgument(shopType instanceof AdminShopType,
				"shopType has to be an AdminShopType");
		return (AdminShopType<?>) shopType;
	}

	/**
	 * Creates a {@link AdminShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, can be <code>null</code>
	 * @param shopType
	 *            the admin shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 * @return the {@link AdminShopCreationData}
	 * @deprecated Use {@link #create(Player, AdminShopType, ShopObjectType, Location, BlockFace)}
	 *             instead
	 */
	@Deprecated
	public static AdminShopCreationData create(
			@Nullable Player creator,
			ShopType<?> shopType,
			ShopObjectType<?> shopObjectType,
			@Nullable Location spawnLocation,
			@Nullable BlockFace targetedBlockFace
	) {
		return create(
				creator,
				toAdminShopType(shopType),
				shopObjectType,
				spawnLocation,
				targetedBlockFace
		);
	}

	/**
	 * Creates a {@link AdminShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, can be <code>null</code>
	 * @param shopType
	 *            the admin shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 * @return the {@link AdminShopCreationData}
	 */
	public static AdminShopCreationData create(
			@Nullable Player creator,
			AdminShopType<?> shopType,
			ShopObjectType<?> shopObjectType,
			@Nullable Location spawnLocation,
			@Nullable BlockFace targetedBlockFace
	) {
		return new AdminShopCreationData(
				creator,
				shopType,
				shopObjectType,
				spawnLocation,
				targetedBlockFace
		);
	}

	/**
	 * Creates a {@link AdminShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, can be <code>null</code>
	 * @param shopType
	 *            the admin shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 * @deprecated Use
	 *             {@link #AdminShopCreationData(Player, AdminShopType, ShopObjectType, Location, BlockFace)}
	 *             instead
	 */
	@Deprecated
	protected AdminShopCreationData(
			@Nullable Player creator,
			ShopType<?> shopType,
			ShopObjectType<?> shopObjectType,
			@Nullable Location spawnLocation,
			@Nullable BlockFace targetedBlockFace
	) {
		this(
				creator,
				toAdminShopType(shopType),
				shopObjectType,
				spawnLocation,
				targetedBlockFace
		);
	}

	/**
	 * Creates a {@link AdminShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, can be <code>null</code>
	 * @param shopType
	 *            the admin shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 */
	protected AdminShopCreationData(
			@Nullable Player creator,
			AdminShopType<?> shopType,
			ShopObjectType<?> shopObjectType,
			@Nullable Location spawnLocation,
			@Nullable BlockFace targetedBlockFace
	) {
		super(
				creator,
				shopType,
				shopObjectType,
				spawnLocation,
				targetedBlockFace
		);
	}
}
