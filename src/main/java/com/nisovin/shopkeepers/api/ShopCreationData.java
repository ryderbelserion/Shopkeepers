package com.nisovin.shopkeepers.api;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/**
 * Holds the different possible arguments needed for the creation of a shopkeeper of a certain type.
 * <p>
 * Additional data may be added via sub-classing, or dynamically via {@link #setValue(String, Object)}.
 */
public class ShopCreationData {

	private final Player creator; // can be null
	private final ShopType<?> shopType; // not null
	private final ShopObjectType shopObjectType; // not null
	private final Location spawnLocation; // not null, modifiable
	private BlockFace targetedBlockFace; // can be null, modifiable

	private Map<String, Object> additionalData;

	/**
	 * Create a {@link ShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, can be <code>null</code>
	 * @param shopType
	 *            the shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, not <code>null</code>
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 */
	public ShopCreationData(Player creator, ShopType<?> shopType, ShopObjectType shopObjectType,
							Location spawnLocation, BlockFace targetedBlockFace) {
		Validate.notNull(shopType, "Shop type is null!");
		Validate.notNull(shopObjectType, "Shop object type is null!");
		Validate.notNull(spawnLocation, "Spawn location is null!");
		// TODO add spawnLocation.isFinite validation once bukkit 1.12+ is supported
		this.creator = creator;
		this.shopType = shopType;
		this.shopObjectType = shopObjectType;
		this.spawnLocation = spawnLocation.clone();
		this.targetedBlockFace = targetedBlockFace;
	}

	/**
	 * The creator of the shop.
	 * 
	 * @return the creating player, can be <code>null</code>
	 */
	public Player getCreator() {
		return creator;
	}

	/**
	 * The type of shop to create.
	 * 
	 * @return the shop type, not <code>null</code>
	 */
	public ShopType<?> getShopType() {
		return shopType;
	}

	/**
	 * The object type for the shop.
	 * 
	 * @return the shop object type, not <code>null</code>
	 */
	public ShopObjectType getShopObjectType() {
		return shopObjectType;
	}

	/**
	 * The location the shopkeeper gets created at.
	 * 
	 * @return the spawn location, not <code>null</code>
	 */
	public Location getSpawnLocation() {
		return spawnLocation.clone();
	}

	/**
	 * Sets the spawn location.
	 * <p>
	 * Has to be located in the same world as the previous spawn location.
	 * 
	 * @param newSpawnLocation
	 *            the new spawn location
	 */
	public void setSpawnLocation(Location newSpawnLocation) {
		Validate.notNull(newSpawnLocation, "New spawn location is null!");
		Validate.isTrue(spawnLocation.getWorld().equals(newSpawnLocation.getWorld()),
				"Cannot set the spawn location to another world!");
		spawnLocation.setX(newSpawnLocation.getX());
		spawnLocation.setY(newSpawnLocation.getY());
		spawnLocation.setZ(newSpawnLocation.getZ());
		spawnLocation.setPitch(newSpawnLocation.getPitch());
		spawnLocation.setYaw(newSpawnLocation.getYaw());
	}

	/**
	 * The block face clicked or targeted during shop creation.
	 * <p>
	 * Used for example by sign shops to specify the direction the sign is facing.
	 * 
	 * @return the targeted block face, can be <code>null</code>
	 */
	public BlockFace getTargetedBlockFace() {
		return targetedBlockFace;
	}

	/**
	 * Sets the targeted block face.
	 * 
	 * @param blockFace
	 *            the new block face
	 */
	public void setTargetedBlockFace(BlockFace blockFace) {
		this.targetedBlockFace = blockFace;
	}

	/**
	 * Gets a previously set value for the specific key.
	 * 
	 * @param key
	 *            the key
	 * @param value
	 *            the value, or <code>null</code>
	 */
	@SuppressWarnings("unchecked")
	public <T> T getValue(String key) {
		if (additionalData == null) return null;
		return (T) additionalData.get(key);
	}

	/**
	 * Sets a value for the specified key.
	 * 
	 * @param key
	 *            the key
	 * @return the value, or <code>null</code> to remove the value for the specified key
	 */
	public <T> void setValue(String key, T value) {
		if (value == null) {
			if (additionalData == null) return;
			additionalData.remove(key);
		} else {
			if (additionalData == null) {
				additionalData = new HashMap<>();
			}
			additionalData.put(key, value);
		}
	}

	/**
	 * Creation data specific for player shops.
	 */
	public static class PlayerShopCreationData extends ShopCreationData {

		private Player owner; // not null, modifiable
		private Block chest; // not null

		public PlayerShopCreationData(	Player creator, ShopType<?> shopType, ShopObjectType objectType,
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
}
