package com.nisovin.shopkeepers.api.shoptypes;

import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.Shopkeeper;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a chest and will deposit earnings
 * back into that chest.
 */
public interface PlayerShopkeeper extends Shopkeeper {

	/**
	 * Sets the owner of this shop.
	 * 
	 * @param player
	 *            the owner of this shop
	 */
	public void setOwner(Player player);

	public void setOwner(UUID ownerUUID, String ownerName);

	/**
	 * Gets the uuid of the player who owns this shop.
	 * 
	 * @return the owners player uuid
	 */
	public UUID getOwnerUUID();

	/**
	 * Gets the last known name of the player who owns this shop.
	 * 
	 * @return the owners last known player name
	 */
	public String getOwnerName();

	/**
	 * Gets a string representation of the owning player.
	 * <p>
	 * This contains the owner's last known name as well as his uuid.
	 * 
	 * @return a string representing the the owner
	 */
	public String getOwnerAsString();

	/**
	 * Checks if the given owner is owning this shop.
	 * 
	 * @param player
	 *            the player to check
	 * @return <code>true</code> if the given player owns this shop
	 */
	public boolean isOwner(Player player);

	/**
	 * Gets the owner of this shop IF he is online.
	 * 
	 * @return the owner of this shop, or <code>null</code> if the owner is offline
	 */
	public Player getOwner();

	/**
	 * Checks whether this shopkeeper is for hire.
	 * <p>
	 * The shopkeeper is for hire if a hire cost has been specified.
	 * 
	 * @return <code>true</code> if this shopkeeper is for hire
	 */
	public boolean isForHire();

	/**
	 * Sets this shopkeeper for hire using the given hire cost.
	 * 
	 * @param hireCost
	 *            the hire cost item, or <code>null</code> or empty to disable hiring for this shopkeeper
	 */
	public void setForHire(ItemStack hireCost);

	/**
	 * Gets the hiring cost of this shopkeeper.
	 * 
	 * @return a copy of the hiring cost item, or <code>null</code> if this shop is not for hire
	 */
	public ItemStack getHireCost();

	public void setChest(int chestX, int chestY, int chestZ);

	public Block getChest();

	public int getCurrencyInChest();
}
