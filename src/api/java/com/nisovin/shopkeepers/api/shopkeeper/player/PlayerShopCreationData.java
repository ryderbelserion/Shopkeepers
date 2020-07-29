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
												Location spawnLocation, BlockFace targetedBlockFace, Block shopContainer) {
		return new PlayerShopCreationData(creator, shopType, objectType, spawnLocation, targetedBlockFace, shopContainer);
	}

	private final Block shopContainer; // not null

	protected PlayerShopCreationData(	Player creator, ShopType<?> shopType, ShopObjectType<?> objectType,
										Location spawnLocation, BlockFace targetedBlockFace, Block shopContainer) {
		super(creator, shopType, objectType, spawnLocation, targetedBlockFace);
		Validate.isTrue(shopType instanceof PlayerShopType, "Shop type has to be a PlayerShopType!");
		// The shop container needs to be located in a world, which is only available for non-virtual shops:
		// TODO Decouple shopkeeper/shop object location from shop container location? (allows containers in different
		// world, and virtual player shopkeepers connected to a container located in a world)
		Validate.isTrue(!(objectType instanceof VirtualShopObjectType), "Cannot create virtual player shops!");
		Validate.notNull(shopContainer, "shopContainer is null");
		Validate.isTrue(spawnLocation.getWorld().equals(shopContainer.getWorld()),
				"The shop container is located in a different world than the spawn location!");
		// the creator cannot be null for player shopkeepers:
		Validate.notNull(creator, "Creator cannot be null!");
		this.shopContainer = shopContainer;
	}

	/**
	 * The container which is backing the player shop.
	 * <p>
	 * Has to be located in the same world the shopkeeper.
	 * <p>
	 * This does not necessarily have to be a chest, but could be another type of supported shop container as well.
	 * 
	 * @return the shop container
	 * @deprecated {@link #getShopContainer()}
	 */
	public Block getShopChest() {
		return getShopContainer();
	}

	/**
	 * The container which is backing the player shop.
	 * <p>
	 * Has to be located in the same world the shopkeeper.
	 * <p>
	 * This does not necessarily have to be a chest, but could be another type of supported shop container as well.
	 * 
	 * @return the shop container
	 */
	public Block getShopContainer() {
		return shopContainer;
	}
}
