package com.nisovin.shopkeepers.api.shopkeeper.admin;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * Shop creation data specific for admin shops.
 */
public class AdminShopCreationData extends ShopCreationData {

	public static AdminShopCreationData create(	Player creator, ShopType<?> shopType, ShopObjectType<?> objectType,
												Location spawnLocation, BlockFace targetedBlockFace) {
		return new AdminShopCreationData(creator, shopType, objectType, spawnLocation, targetedBlockFace);
	}

	protected AdminShopCreationData(Player creator, ShopType<?> shopType, ShopObjectType<?> objectType,
									Location spawnLocation, BlockFace targetedBlockFace) {
		super(creator, shopType, objectType, spawnLocation, targetedBlockFace);
		Validate.isTrue(shopType instanceof AdminShopType, "Shop type has to be a AdminShopType!");
	}
}
