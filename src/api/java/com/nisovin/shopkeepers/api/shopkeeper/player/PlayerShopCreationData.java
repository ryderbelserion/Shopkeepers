package com.nisovin.shopkeepers.api.shopkeeper.player;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObjectType;

/**
 * Shop creation data specific for player shops.
 */
public class PlayerShopCreationData extends ShopCreationData {

	public static PlayerShopCreationData create(Player creator, ShopType<?> shopType, ShopObjectType<?> objectType,
												Location spawnLocation, BlockFace targetedBlockFace, Block chest) {
		return new PlayerShopCreationData(creator, shopType, objectType, spawnLocation, targetedBlockFace, chest);
	}

	private final Block chest; // not null

	protected PlayerShopCreationData(	Player creator, ShopType<?> shopType, ShopObjectType<?> objectType,
										Location spawnLocation, BlockFace targetedBlockFace, Block chest) {
		super(creator, shopType, objectType, spawnLocation, targetedBlockFace);
		Validate.isTrue(shopType instanceof PlayerShopType, "Shop type has to be a PlayerShopType!");
		// chest needs to be located in a world, which is only available for non-virtual shops:
		// TODO decouple shopkeeper/shop object location from chest location? (allows chest in different world, and
		// virtual player shopkeepers connected to a chest located in a world)
		Validate.isTrue(!(objectType instanceof VirtualShopObjectType), "Cannot create virtual player shops!");
		Validate.notNull(chest, "Chest is null!");
		Validate.isTrue(spawnLocation.getWorld().equals(chest.getWorld()),
				"Chest is located in a different world than the spawn location!");
		// the creator cannot be null for player shopkeepers:
		Validate.notNull(creator, "Creator cannot be null!");
		this.chest = chest;
	}

	/**
	 * The chest which is backing the player shop.
	 * <p>
	 * Has to be located in the same world the shopkeeper.
	 * 
	 * @return the shop chest
	 */
	public Block getShopChest() {
		return chest;
	}
}
