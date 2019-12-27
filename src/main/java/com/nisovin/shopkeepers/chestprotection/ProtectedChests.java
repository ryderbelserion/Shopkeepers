package com.nisovin.shopkeepers.chestprotection;

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
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * <b>Chest protection:</b>
 * <p>
 * <b>Protected chests:</b><br>
 * A chest directly used by a player shopkeeper is 'directly protected'. Any adjacent chests (N, W, S, E) that are
 * connected to a directly protected chest are protected as well.
 * <p>
 * <b>Bypass:</b><br>
 * The owner of a shop corresponding to a protected chest and players with the bypass permission are not affected by the
 * listed protections. Also, certain protections can be disabled via config settings.
 * <p>
 * <b>Protections:</b><br>
 * <ul>
 * <li>Protected chests cannot be accessed.
 * <li>Protected chests cannot be broken or destroyed by explosions.
 * <li>Protected chests don't transfer items, ex. into or from hoppers, droppers, etc.
 * <li>Chests cannot be placed adjacent to a directly protected chest, if they would connect to it.
 * <li>Droppers cannot be placed adjacent to a protected chest, if they would be able to receive or inject items.
 * <li>Hoppers cannot be placed adjacent to a protected chest, if they would be able to inject items.
 * <li>Rails cannot be placed below a protected chest (to prevent hopper carts stealing items).
 * </ul>
 * Note that the following cases are not protected for:
 * <ul>
 * <li>Even though rails cannot be placed below protected chests, hopper carts are still able to be placed / maneuvered
 * to end up below protected chests. The item movement protection will however still prevent items from being extracted
 * from the chest.
 * <li>Adjacent unconnected chests are not protected. It should however not be possible to access the protected adjacent
 * chest by that.
 * <li>Adjacent or chains of droppers and hoppers are not protected. So if item movement is enabled in the config and
 * the shop owner places those connected to his shop chest, other players will be able to access (or break) them.
 * </ul>
 */
public class ProtectedChests {

	private final SKShopkeepersPlugin plugin;
	private final ChestProtectionListener chestProtectionListener = new ChestProtectionListener(this);
	private final InventoryMoveItemListener inventoryMoveItemListener = new InventoryMoveItemListener(this);
	private final RemoveShopOnChestBreakListener removeShopOnChestBreakListener;
	// player shopkeepers by location key:
	private final Map<String, List<PlayerShopkeeper>> protectedChests = new HashMap<>();

	public ProtectedChests(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		removeShopOnChestBreakListener = new RemoveShopOnChestBreakListener(plugin, this);
	}

	public void enable() {
		if (Settings.protectChests) {
			Bukkit.getPluginManager().registerEvents(chestProtectionListener, plugin);
			if (Settings.preventItemMovement) {
				Bukkit.getPluginManager().registerEvents(inventoryMoveItemListener, plugin);
			}
		}
		if (Settings.deleteShopkeeperOnBreakChest) {
			Bukkit.getPluginManager().registerEvents(removeShopOnChestBreakListener, plugin);
			// TODO let shopkeepers periodically check if their chest is still there, and otherwise delete themselves?
		}
	}

	public void disable() {
		// cleanup:
		HandlerList.unregisterAll(chestProtectionListener);
		HandlerList.unregisterAll(inventoryMoveItemListener);
		HandlerList.unregisterAll(removeShopOnChestBreakListener);
		protectedChests.clear();
	}

	private String getKey(String worldName, int x, int y, int z) {
		return worldName + ";" + x + ";" + y + ";" + z;
	}

	public void addChest(String worldName, int x, int y, int z, PlayerShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper);
		String key = this.getKey(worldName, x, y, z);
		List<PlayerShopkeeper> shopkeepers = protectedChests.get(key);
		if (shopkeepers == null) {
			shopkeepers = new ArrayList<>(1);
			protectedChests.put(key, shopkeepers);
		}
		shopkeepers.add(shopkeeper);
	}

	public void removeChest(String worldName, int x, int y, int z, PlayerShopkeeper shopkeeper) {
		Validate.notNull(shopkeeper);
		String key = this.getKey(worldName, x, y, z);
		List<PlayerShopkeeper> shopkeepers = protectedChests.get(key);
		if (shopkeepers == null) return;
		shopkeepers.remove(shopkeeper);
		if (shopkeepers.isEmpty()) {
			protectedChests.remove(key);
		}
	}

	// gets the shopkeepers which are directly using the chest at the specified location
	private List<PlayerShopkeeper> _getShopkeepers(String worldName, int x, int y, int z) {
		String key = this.getKey(worldName, x, y, z);
		return protectedChests.get(key);
	}

	// gets the shopkeepers which are directly using the specified chest block
	private List<PlayerShopkeeper> _getShopkeepers(Block block) {
		return this._getShopkeepers(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	//

	// gets the shopkeepers which are directly using the chest at the specified location:
	public List<PlayerShopkeeper> getShopkeepers(String worldName, int x, int y, int z) {
		List<PlayerShopkeeper> shopkeepers = this._getShopkeepers(worldName, x, y, z);
		return (shopkeepers == null ? Collections.emptyList() : Collections.unmodifiableList(shopkeepers));
	}

	public List<PlayerShopkeeper> getShopkeepers(Block block) {
		return this.getShopkeepers(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	//

	// checks if this exact block is protected
	public boolean isChestDirectlyProtected(String worldName, int x, int y, int z, Player player) {
		List<PlayerShopkeeper> shopkeepers = this._getShopkeepers(worldName, x, y, z);
		// is there any shopkeeper using this chest?
		if (shopkeepers == null) return false;
		assert !shopkeepers.isEmpty();
		if (player != null) {
			// check whether the player is affected by the protection:
			// note: the bypass permission does not get checked here but needs to be checked separately
			// always allow shop owners to access their shop chest (regardless of other shopkeepers using the same
			// chest):
			for (PlayerShopkeeper shopkeeper : shopkeepers) {
				if (shopkeeper.isOwner(player)) return false;
			}
		}
		// there exists a protection for this chest, and the player doesn't own any shopkeeper using the chest:
		return true;
	}

	public boolean isChestDirectlyProtected(Block block, Player player) {
		return this.isChestDirectlyProtected(block.getWorld().getName(), block.getX(), block.getY(), block.getZ(), player);
	}

	//

	// gets reused by isChestProtected calls:
	private final List<PlayerShopkeeper> tempResultsList = new ArrayList<>();

	/**
	 * Checks if the given chest block is protected.
	 * <p>
	 * The block is protected if either:
	 * <ul>
	 * <li>The block is directly used by a shopkeeper.
	 * <li>The block is a chest that is connected to a block that is directly used by a shopkeeper.
	 * </ul>
	 * 
	 * @param chest
	 *            the chest block (the block might not actually be a chest right now though)
	 * @param player
	 *            the player to check the protection for, or <code>null</code> to check for protection without taking
	 *            shop owners into account
	 * @return <code>true</code> if the block is protected
	 */
	public boolean isChestProtected(Block chest, Player player) {
		Validate.notNull(chest, "Chest block is null!");

		// reuse logic from getShopkeeperOwnersOfChest:
		this.getShopkeepersUsingChest(chest, tempResultsList);
		if (tempResultsList.isEmpty()) {
			// no protection found:
			return false;
		} else {
			// protection found:
			boolean result = true;
			// check if the player is affected by the protection:
			if (player != null) {
				// note: the bypass permission does not get checked here but needs to be checked separately
				// always allow shop owners to access their shop chest (regardless of other shopkeepers using the same
				// chest):
				for (PlayerShopkeeper shopkeeper : tempResultsList) {
					if (shopkeeper.isOwner(player)) {
						result = false;
						break;
					}
				}
			}
			// cleanup temporary results list:
			tempResultsList.clear();
			return result;
		}
	}

	/**
	 * Checks if the given block is a protected chest.
	 * <p>
	 * This makes sure that the specified block is actually a chest and it takes the bypass permission into account.
	 * 
	 * @param block
	 *            the block
	 * @param player
	 *            the player to check the protection for, or <code>null</code> to check for protection without taking
	 *            shop owners and players with bypass permission into account
	 * @return <code>true</code> if the block is a protected chest
	 */
	public boolean isProtectedChest(Block block, Player player) {
		if (block == null || !ItemUtils.isChest(block.getType())) return false;
		if (!this.isChestProtected(block, player)) return false;
		if (player != null && PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) return false;
		return true;
	}

	// gets the shopkeepers which use the chest at the given location (directly or by a connected chest):
	public List<PlayerShopkeeper> getShopkeepersUsingChest(Block chest) {
		return this.getShopkeepersUsingChest(chest, null);
	}

	// gets the shopkeepers which use the chest at the given location (directly or by a connected chest), and adds them
	// to the provided list:
	private List<PlayerShopkeeper> getShopkeepersUsingChest(Block chest, List<PlayerShopkeeper> results) {
		Validate.notNull(chest, "Chest block is null!");
		// create results list if none is provided:
		if (results == null) {
			results = new ArrayList<>();
		}

		// checking if this block is used directly by shopkeepers:
		List<PlayerShopkeeper> shopkeepers = this._getShopkeepers(chest);
		if (shopkeepers != null) {
			assert !shopkeepers.isEmpty();
			results.addAll(shopkeepers);
		}

		// if the block is actually a chest, check for a connected chest:
		Material chestType = chest.getType();
		if (ItemUtils.isChest(chestType)) {
			Chest chestData = (Chest) chest.getBlockData();
			BlockFace chestFacing = chestData.getFacing();
			BlockFace connectedFace = getConnectedBlockFace(chestFacing, chestData.getType());
			if (connectedFace != null) {
				Block connectedChest = chest.getRelative(connectedFace);
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
				return null; // not connected
			}
		case EAST:
			switch (chestType) {
			case RIGHT:
				return BlockFace.NORTH;
			case LEFT:
				return BlockFace.SOUTH;
			default:
				return null; // not connected
			}
		case SOUTH:
			switch (chestType) {
			case RIGHT:
				return BlockFace.EAST;
			case LEFT:
				return BlockFace.WEST;
			default:
				return null; // not connected
			}
		case WEST:
			switch (chestType) {
			case RIGHT:
				return BlockFace.SOUTH;
			case LEFT:
				return BlockFace.NORTH;
			default:
				return null; // not connected
			}
		default:
			return null; // invalid chest facing
		}
	}
}
