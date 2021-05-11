package com.nisovin.shopkeepers.api.shopkeeper.player;

import java.util.UUID;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

/**
 * A shopkeeper that is managed by a player. This shopkeeper draws its supplies from a container and will deposit
 * earnings back into that container.
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
	 * @return the owner's player uuid, not <code>null</code>
	 */
	public UUID getOwnerUUID();

	/**
	 * Gets the last known name of the player who owns this shop.
	 * 
	 * @return the owner's last known name, not <code>null</code>
	 */
	public String getOwnerName();

	/**
	 * Gets a string representation of the owning player.
	 * <p>
	 * This contains the owner's last known name as well as his uuid.
	 * 
	 * @return a string representing the the owner
	 */
	public String getOwnerString();

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
	 * Checks whether the trade notifications for the shop owner are enabled for this shopkeeper.
	 * <p>
	 * This property only affects the trade notifications that are sent to the shop owner. It has no effect on the
	 * general trade notifications that may be sent for this shopkeeper to other players.
	 * 
	 * @return <code>true</code> if the shop owner trade notifications are enabled
	 */
	public boolean isNotifyOnTrades();

	/**
	 * Sets whether the trade notifications for the shop owner are enabled for this shopkeeper.
	 * 
	 * @param enabled
	 *            the new activation state
	 * @see #isNotifyOnTrades()
	 */
	public void setNotifyOnTrades(boolean enabled);

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

	/**
	 * Gets the container's x coordinate.
	 * 
	 * @return the container's x coordinate
	 * @deprecated Use {@link #getContainerX()}
	 */
	public int getChestX();

	/**
	 * Gets the container's y coordinate.
	 * 
	 * @return the container's y coordinate
	 * @deprecated Use {@link #getContainerY()}
	 */
	public int getChestY();

	/**
	 * Gets the container's z coordinate.
	 * 
	 * @return the container's z coordinate.
	 * @deprecated Use {@link #getContainerZ()}
	 */
	public int getChestZ();

	/**
	 * @param containerX
	 * @param containerY
	 * @param containerZ
	 * @deprecated Use {@link #setContainer(int, int, int)}
	 */
	public void setChest(int containerX, int containerY, int containerZ);

	/**
	 * 
	 * @return
	 * @deprecated Use {@link #getContainer()}
	 */
	public Block getChest();

	/**
	 * 
	 * @return
	 * @deprecated Use {@link #getCurrencyInContainer()}
	 */
	public int getCurrencyInChest();

	/**
	 * Gets the container's x coordinate.
	 * 
	 * @return the container's x coordinate
	 */
	public int getContainerX();

	/**
	 * Gets the container's y coordinate.
	 * 
	 * @return the container's y coordinate
	 */
	public int getContainerY();

	/**
	 * Gets the container's z coordinate.
	 * 
	 * @return the container's z coordinate.
	 */
	public int getContainerZ();

	public void setContainer(int containerX, int containerY, int containerZ);

	/**
	 * Gets the block of the shop's container.
	 * <p>
	 * This does not necessarily have to be a chest, but could be another type of supported shop container as well.
	 * <p>
	 * The block might not actually be a valid container type currently (for example if something has broken or changed
	 * the type of the block in the meantime).
	 * 
	 * @return the shop's container block
	 */
	public Block getContainer();

	/**
	 * Gets the amount of currency stored inside the shop's container.
	 * <p>
	 * Returns <code>0</code> if the container does not exist currently.
	 * 
	 * @return the amount of currency inside the shop's container
	 */
	public int getCurrencyInContainer();

	// SHOPKEEPER UIs - shortcuts for common UI types:

	/**
	 * Attempts to open the hiring interface of this shopkeeper for the specified player.
	 * <p>
	 * Fails if this shopkeeper type doesn't support hiring (ex. admin shops).
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the interface was successfully opened
	 */
	public boolean openHireWindow(Player player);

	/**
	 * Attempts to open the container inventory of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the interface was successfully opened
	 * @deprecated {@link #openContainerWindow(Player)}
	 */
	public boolean openChestWindow(Player player);

	/**
	 * Attempts to open the container inventory of this shopkeeper for the specified player.
	 * 
	 * @param player
	 *            the player
	 * @return <code>true</code> if the interface was successfully opened
	 */
	public boolean openContainerWindow(Player player);
}
