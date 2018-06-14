package com.nisovin.shopkeepers.chestprotection;

import java.util.Iterator;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.InventoryHolder;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Provides enhanced chest protection (can be disabled via a config setting).
 */
class ChestProtectionListener implements Listener {

	private final ProtectedChests protectedChests;

	ChestProtectionListener(ProtectedChests protectedChests) {
		this.protectedChests = protectedChests;
	}

	@EventHandler(ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (!ItemUtils.isChest(block.getType())) return;
		Player player = event.getPlayer();
		if (Utils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) return;

		if (protectedChests.isChestProtected(block, player)) {
			Log.debug("Cancelled breaking of chest block by '" + player.getName() + "' at '"
					+ Utils.getLocationString(block) + "': Protected chest");
			event.setCancelled(true);
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		Material type = block.getType();
		Player player = event.getPlayer();
		if (ItemUtils.isChest(type)) {
			if (protectedChests.isChestProtected(block, player)) {
				Log.debug("Cancelled placing of chest block by '" + player.getName() + "' at '"
						+ Utils.getLocationString(block) + "': Protected chest nearby");
				event.setCancelled(true);
			}
		} else if (type == Material.HOPPER) {
			if (protectedChests.isProtectedChestAroundHopper(block, player)) {
				Log.debug("Cancelled placing of hopper block by '" + player.getName() + "' at '"
						+ Utils.getLocationString(block) + "': Protected chest nearby");
				event.setCancelled(true);
			}
		} else if (type == Material.RAILS || type == Material.POWERED_RAIL || type == Material.DETECTOR_RAIL || type == Material.ACTIVATOR_RAIL) {
			Block upperBlock = block.getRelative(BlockFace.UP);
			if (ItemUtils.isChest(upperBlock.getType()) && protectedChests.isChestProtected(upperBlock, player)) {
				Log.debug("Cancelled placing of rail block by '" + player.getName() + "' at '"
						+ Utils.getLocationString(block) + "': Protected chest nearby");
				event.setCancelled(true);
				return;
			}
		}
	}

	@EventHandler(ignoreCancelled = true)
	void onInventoryMoveItem(InventoryMoveItemEvent event) {
		if (event.getSource() != null) {
			InventoryHolder holder = event.getSource().getHolder();
			if (holder != null && holder instanceof Chest) {
				Block block = ((Chest) holder).getBlock();
				if (protectedChests.isChestProtected(block, null)) {
					event.setCancelled(true);
				}
			}
		}
	}

	// TODO also listen to spigot's BlockExplodeEvent in 1.8.4 (R3)?
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onExplosion(EntityExplodeEvent event) {
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();
			if (ItemUtils.isChest(block.getType()) && protectedChests.isChestProtected(block, null)) {
				iter.remove();
			}
		}
	}
}
