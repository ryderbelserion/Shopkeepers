package com.nisovin.shopkeepers.container.protection;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Chest;
import org.bukkit.block.data.type.Chest.Type;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * <b>Container protection.</b>
 * <p>
 * <b>Protected containers:</b><br>
 * A container directly used by a player shopkeeper is 'directly protected'. Any adjacent chests (N, W, S, E) that form
 * a double chest with a directly protected chest are protected as well.
 * <p>
 * <b>Bypass:</b><br>
 * The owner of a shop corresponding to a protected container and players with the bypass permission are not affected
 * by the listed protections. Also, certain protections can be disabled via config settings.
 * <p>
 * <b>Protections:</b><br>
 * <ul>
 * <li>Protected containers cannot be accessed.
 * <li>Protected containers cannot be broken or destroyed by explosions.
 * <li>Protected containers don't transfer items, ex. into or from hoppers, droppers, etc.
 * <li>Chests cannot be placed adjacent to a directly protected chest, if they would connect to it and form a double
 * chest.
 * <li>Hoppers and droppers cannot be placed adjacent to a protected container, if they would be able to receive or
 * inject items.
 * <li>Rails cannot be placed below a protected container (to prevent hopper carts stealing items).
 * </ul>
 * Note that there is no protection for the following cases:
 * <ul>
 * <li>Even though rails cannot be placed below protected containers, hopper carts are still able to be placed / pushed
 * to end up below protected containers. The item movement protection will however still prevent items from being
 * extracted from the container.
 * <li>Adjacent unconnected chests are not protected. It should however not be possible to access any protected adjacent
 * chest by that.
 * <li>Adjacent or chains of droppers and hoppers are not protected. So if item movement is enabled in the config and
 * the shop owner places those connected to his shop container, other players will be able to access (or break) them.
 * </ul>
 */
public class ProtectedContainers {

	private final SKShopkeepersPlugin plugin;
	private final ContainerProtectionListener containerProtectionListener = new ContainerProtectionListener(this);
	private final InventoryMoveItemListener inventoryMoveItemListener = new InventoryMoveItemListener(this);
	// Player shopkeepers by location key:
	private final Map<String, List<PlayerShopkeeper>> protectedContainers = new HashMap<>();

	public ProtectedContainers(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void enable() {
		if (Settings.protectContainers) {
			Bukkit.getPluginManager().registerEvents(containerProtectionListener, plugin);
			if (Settings.preventItemMovement) {
				Bukkit.getPluginManager().registerEvents(inventoryMoveItemListener, plugin);
			}
		}
	}

	public void disable() {
		// Cleanup:
		HandlerList.unregisterAll(containerProtectionListener);
		HandlerList.unregisterAll(inventoryMoveItemListener);
		protectedContainers.clear();
	}

	private String getKey(String worldName, int x, int y, int z) {
		return worldName + ";" + x + ";" + y + ";" + z;
	}

	public void addContainer(String worldName, int x, int y, int z, PlayerShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		String key = this.getKey(worldName, x, y, z);
		List<PlayerShopkeeper> shopkeepers = protectedContainers.get(key);
		if (shopkeepers == null) {
			shopkeepers = new ArrayList<>(1);
			protectedContainers.put(key, shopkeepers);
		}
		shopkeepers.add(shopkeeper);
	}

	public void removeContainer(String worldName, int x, int y, int z, PlayerShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		String key = this.getKey(worldName, x, y, z);
		List<PlayerShopkeeper> shopkeepers = protectedContainers.get(key);
		if (shopkeepers == null) return;
		shopkeepers.remove(shopkeeper);
		if (shopkeepers.isEmpty()) {
			protectedContainers.remove(key);
		}
	}

	// Gets the shopkeepers which are directly using the container at the specified location:
	private List<PlayerShopkeeper> _getShopkeepers(String worldName, int x, int y, int z) {
		String key = this.getKey(worldName, x, y, z);
		return protectedContainers.get(key);
	}

	// Gets the shopkeepers which are directly using the specified container block:
	private List<PlayerShopkeeper> _getShopkeepers(Block block) {
		return this._getShopkeepers(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	// Gets the shopkeepers which are directly using the container at the specified location:
	public List<PlayerShopkeeper> getShopkeepers(String worldName, int x, int y, int z) {
		List<PlayerShopkeeper> shopkeepers = this._getShopkeepers(worldName, x, y, z);
		return (shopkeepers == null) ? Collections.emptyList() : Collections.unmodifiableList(shopkeepers);
	}

	public List<PlayerShopkeeper> getShopkeepers(Block block) {
		return this.getShopkeepers(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	//

	// Checks if this exact block is protected:
	public boolean isContainerDirectlyProtected(String worldName, int x, int y, int z, Player player) {
		List<PlayerShopkeeper> shopkeepers = this._getShopkeepers(worldName, x, y, z);
		// Check if there are any shopkeepers using this container:
		if (shopkeepers == null) return false;
		assert !shopkeepers.isEmpty();
		if (player != null) {
			// Check whether the player is affected by the protection:
			// Note: The bypass permission does not get checked here but needs to be checked separately.
			// We always allow shop owners to access their shop container (regardless of other shopkeepers using the
			// same container):
			for (PlayerShopkeeper shopkeeper : shopkeepers) {
				if (shopkeeper.isOwner(player)) return false;
			}
		}
		// There exists a protection for this container and the player doesn't own any shopkeeper using it:
		return true;
	}

	public boolean isContainerDirectlyProtected(Block block, Player player) {
		return this.isContainerDirectlyProtected(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), player);
	}

	//

	// Gets reused by isContainerProtected calls:
	private final List<PlayerShopkeeper> tempResultsList = new ArrayList<>();

	/**
	 * Checks if the given container block is protected.
	 * <p>
	 * The block is protected if either:
	 * <ul>
	 * <li>The container block is directly used by a shopkeeper.
	 * <li>The block is a chest that is connected to another chest block (forms a double chest) which is directly used
	 * by a shopkeeper.
	 * </ul>
	 * 
	 * @param containerBlock
	 *            the container block (the block might not actually be a container anymore though)
	 * @param player
	 *            the player to check the protection for, or <code>null</code> to check for protection without taking
	 *            shop owners into account
	 * @return <code>true</code> if the block is protected
	 */
	public boolean isContainerProtected(Block containerBlock, Player player) {
		Validate.notNull(containerBlock, "containerBlock is null!");

		this.getShopkeepersUsingContainer(containerBlock, tempResultsList);
		if (tempResultsList.isEmpty()) {
			// No protection found:
			return false;
		}

		// Protection found:
		boolean result = true;
		// Check if the player is affected by the protection:
		if (player != null) {
			// Note: The bypass permission does not get checked here but needs to be checked separately.
			// We always allow shop owners to access their shop container (regardless of other shopkeepers using the
			// same container):
			for (PlayerShopkeeper shopkeeper : tempResultsList) {
				if (shopkeeper.isOwner(player)) {
					result = false;
					break;
				}
			}
		}
		// Cleanup temporary results list:
		tempResultsList.clear();
		return result;
	}

	/**
	 * Checks if the given block is a protected shop container.
	 * <p>
	 * This checks if the specified block is actually a supported shop container and takes the bypass permission into
	 * account.
	 * 
	 * @param block
	 *            the block
	 * @param player
	 *            the player to check the protection for, or <code>null</code> to check for protection without taking
	 *            shop owners and players with bypass permission into account
	 * @return <code>true</code> if the block is a protected container
	 */
	public boolean isProtectedContainer(Block block, Player player) {
		if (block == null || !ShopContainers.isSupportedContainer(block.getType())) return false;
		if (!this.isContainerProtected(block, player)) return false;
		if (player != null && PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) return false;
		return true;
	}

	// Gets the shopkeepers which use the container at the given location (directly or by a connected chest):
	public List<PlayerShopkeeper> getShopkeepersUsingContainer(Block containerBlock) {
		return this.getShopkeepersUsingContainer(containerBlock, null);
	}

	// Gets the shopkeepers which use the container at the given location (directly or by a connected chest), and adds
	// them to the provided list:
	private List<PlayerShopkeeper> getShopkeepersUsingContainer(Block containerBlock, List<PlayerShopkeeper> results) {
		Validate.notNull(containerBlock, "containerBlock is null!");
		// Create results list if none is provided:
		if (results == null) {
			results = new ArrayList<>();
		}

		// Check if the block is directly used by shopkeepers:
		List<PlayerShopkeeper> shopkeepers = this._getShopkeepers(containerBlock);
		if (shopkeepers != null) {
			assert !shopkeepers.isEmpty();
			results.addAll(shopkeepers);
		}

		// If the block actually is a chest, check for a connected chest:
		Material chestType = containerBlock.getType();
		if (ItemUtils.isChest(chestType)) {
			Chest chestData = (Chest) containerBlock.getBlockData();
			BlockFace chestFacing = chestData.getFacing();
			BlockFace connectedFace = getConnectedBlockFace(chestFacing, chestData.getType());
			if (connectedFace != null) {
				Block connectedChest = containerBlock.getRelative(connectedFace);
				// In case of inconsistency of the block data (i.e. connected chest missing or not mutually connected),
				// we consider the block to be connected (and by that protected) anyways, because such inconsistencies
				// might also occur during handling of block placements.
				// Minecraft determines double chests by these consistency criteria:
				// Same chest type, same facing, opposite chest type (opposite connected block faces)
				shopkeepers = this._getShopkeepers(connectedChest);
				if (shopkeepers != null) {
					results.addAll(shopkeepers);
				}
			}
		}
		return results;
	}

	private static BlockFace getConnectedBlockFace(BlockFace chestFacing, Type chestType) {
		switch (chestFacing) {
		case NORTH:
			switch (chestType) {
			case RIGHT:
				return BlockFace.WEST;
			case LEFT:
				return BlockFace.EAST;
			default:
				return null; // Not connected
			}
		case EAST:
			switch (chestType) {
			case RIGHT:
				return BlockFace.NORTH;
			case LEFT:
				return BlockFace.SOUTH;
			default:
				return null; // Not connected
			}
		case SOUTH:
			switch (chestType) {
			case RIGHT:
				return BlockFace.EAST;
			case LEFT:
				return BlockFace.WEST;
			default:
				return null; // Not connected
			}
		case WEST:
			switch (chestType) {
			case RIGHT:
				return BlockFace.SOUTH;
			case LEFT:
				return BlockFace.NORTH;
			default:
				return null; // Not connected
			}
		default:
			return null; // Invalid chest facing
		}
	}
}
