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

	private static PlayerShopType<?> toPlayerShopType(ShopType<?> shopType) {
		Validate.isTrue(shopType instanceof PlayerShopType, "shopType has to be a PlayerShopType");
		return (PlayerShopType<?>) shopType;
	}

	/**
	 * Creates a {@link PlayerShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, not <code>null</code>
	 * @param shopType
	 *            the player shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 * @param shopContainer
	 *            the shop container, not <code>null</code>
	 * @return the {@link PlayerShopCreationData}
	 * @deprecated Use {@link #create(Player, PlayerShopType, ShopObjectType, Location, BlockFace, Block)} instead
	 */
	@Deprecated
	public static PlayerShopCreationData create(Player creator, ShopType<?> shopType, ShopObjectType<?> shopObjectType,
												Location spawnLocation, BlockFace targetedBlockFace, Block shopContainer) {
		return create(creator, toPlayerShopType(shopType), shopObjectType, spawnLocation, targetedBlockFace, shopContainer);
	}

	/**
	 * Creates a {@link PlayerShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, not <code>null</code>
	 * @param shopType
	 *            the player shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 * @param shopContainer
	 *            the shop container, not <code>null</code>
	 * @return the {@link PlayerShopCreationData}
	 */
	public static PlayerShopCreationData create(Player creator, PlayerShopType<?> shopType, ShopObjectType<?> shopObjectType,
												Location spawnLocation, BlockFace targetedBlockFace, Block shopContainer) {
		return new PlayerShopCreationData(creator, shopType, shopObjectType, spawnLocation, targetedBlockFace, shopContainer);
	}

	private final Block shopContainer; // not null

	/**
	 * Creates a {@link PlayerShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, not <code>null</code>
	 * @param shopType
	 *            the player shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 * @param shopContainer
	 *            the shop container, not <code>null</code>
	 */
	protected PlayerShopCreationData(	Player creator, ShopType<?> shopType, ShopObjectType<?> shopObjectType,
										Location spawnLocation, BlockFace targetedBlockFace, Block shopContainer) {
		this(creator, toPlayerShopType(shopType), shopObjectType, spawnLocation, targetedBlockFace, shopContainer);
	}

	/**
	 * Creates a {@link PlayerShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, not <code>null</code>
	 * @param shopType
	 *            the player shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 * @param shopContainer
	 *            the shop container, not <code>null</code>
	 */
	protected PlayerShopCreationData(	Player creator, PlayerShopType<?> shopType, ShopObjectType<?> shopObjectType,
										Location spawnLocation, BlockFace targetedBlockFace, Block shopContainer) {
		super(creator, shopType, shopObjectType, spawnLocation, targetedBlockFace);
		// The shop container needs to be located in a world, which is only available for non-virtual shops:
		// TODO Decouple shopkeeper/shop object location from shop container location? (allows containers in different
		// world, and virtual player shopkeepers connected to a container located in a world)
		Validate.isTrue(!(shopObjectType instanceof VirtualShopObjectType), "Cannot create virtual player shops!");
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
	@Deprecated
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
