package com.nisovin.shopkeepers.chestprotection;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Handles chest protection. Can be disabled via a config setting.
 */
class ChestProtectionListener implements Listener {

	private final ProtectedChests protectedChests;

	ChestProtectionListener(ProtectedChests protectedChests) {
		this.protectedChests = protectedChests;
	}

	/**
	 * Prevents unauthorized opening of protected chests.
	 */
	@EventHandler(priority = EventPriority.LOW)
	void onPlayerInteract(PlayerInteractEvent event) {
		// prevent opening shop chests
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
		Player player = event.getPlayer();
		if (protectedChests.isProtectedChest(block, player)) {
			// TODO always allow access to own shop chests, even if cancelled by other plugins?
			Log.debug("Cancelled chest opening by '" + player.getName() + "' at '"
					+ Utils.getLocationString(block) + "': Protected chest");
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		Player player = event.getPlayer();
		if (protectedChests.isProtectedChest(block, player)) {
			Log.debug("Cancelled breaking of chest block by '" + player.getName() + "' at '"
					+ Utils.getLocationString(block) + "': Protected chest");
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		Material type = block.getType();
		Player player = event.getPlayer();

		if (ItemUtils.isChest(type)) {
			// note: unconnected chests can be placed
			if (protectedChests.isProtectedChest(block, player)) {
				Log.debug("Cancelled placing of chest block by '" + player.getName() + "' at '"
						+ Utils.getLocationString(block) + "': Protected chest nearby");
				event.setCancelled(true);
			}
		} else if (type == Material.HOPPER) {
			// prevent placement of hoppers that could be used to extract or inject items from/into a protected chest:
			Block upperBlock = block.getRelative(BlockFace.UP);
			if (protectedChests.isProtectedChest(upperBlock, player) || protectedChests.isProtectedChest(this.getFacedBlock(block), player)) {
				Log.debug("Cancelled placing of hopper block by '" + player.getName() + "' at '"
						+ Utils.getLocationString(block) + "': Protected chest nearby");
				event.setCancelled(true);
			}
		} else if (type == Material.DROPPER) {
			// prevent placement of droppers that could be used to inject items into a protected chest:
			if (protectedChests.isProtectedChest(this.getFacedBlock(block), player)) {
				Log.debug("Cancelled placing of dropper block by '" + player.getName() + "' at '"
						+ Utils.getLocationString(block) + "': Protected chest nearby");
				event.setCancelled(true);
			}
		} else if (type == Material.RAIL || type == Material.POWERED_RAIL || type == Material.DETECTOR_RAIL || type == Material.ACTIVATOR_RAIL) {
			Block upperBlock = block.getRelative(BlockFace.UP);
			if (protectedChests.isProtectedChest(upperBlock, player)) {
				Log.debug("Cancelled placing of rail block by '" + player.getName() + "' at '"
						+ Utils.getLocationString(block) + "': Protected chest nearby");
				event.setCancelled(true);
			}
		}
	}

	private Block getFacedBlock(Block directionalBlock) {
		Directional directionalData = (Directional) directionalBlock.getBlockData();
		BlockFace facing = directionalData.getFacing();
		return directionalBlock.getRelative(facing);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onEntityExplosion(EntityExplodeEvent event) {
		this.removeProtectedChests(event.blockList());
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockExplosion(BlockExplodeEvent event) {
		this.removeProtectedChests(event.blockList());
	}

	// block list has to be modifiable
	private void removeProtectedChests(List<Block> blockList) {
		Iterator<Block> iterator = blockList.iterator();
		while (iterator.hasNext()) {
			Block block = iterator.next();
			if (protectedChests.isProtectedChest(block, null)) {
				iterator.remove();
			}
		}
	}
}
