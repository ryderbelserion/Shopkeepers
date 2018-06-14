package com.nisovin.shopkeepers.api.shopkeeper.player;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

/**
 * Shop creation data specific for player shops.
 */
public class PlayerShopCreationData extends ShopCreationData {

	public static PlayerShopCreationData create(Player creator, ShopType<?> shopType, ShopObjectType<?> objectType,
												Location spawnLocation, BlockFace targetedBlockFace, Player owner, Block chest) {
		return new PlayerShopCreationData(creator, shopType, objectType, spawnLocation, targetedBlockFace, owner, chest);
	}

	private final Player owner; // not null, modifiable
	private final Block chest; // not null

	protected PlayerShopCreationData(	Player creator, ShopType<?> shopType, ShopObjectType<?> objectType,
										Location spawnLocation, BlockFace targetedBlockFace, Player owner, Block chest) {
		super(creator, shopType, objectType, spawnLocation, targetedBlockFace);
		Validate.notNull(owner, "Owner is null!");
		Validate.notNull(chest, "Chest is null!");
		Validate.isTrue(spawnLocation.getWorld().equals(chest.getWorld()),
				"Chest is located in a different world than the spawn location!");
		this.owner = owner;
		this.chest = chest;
	}

	/**
	 * The owner of the player shop.
	 * <p>
	 * Usually the same player as the creator.
	 * 
	 * @return the shop owner
	 */
	public Player getOwner() {
		return owner;
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
