package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Iterator;

import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPhysicsEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

class SignShopListener implements Listener {

	private final SKShopkeepersPlugin plugin;

	private Block cancelNextBlockPhysics = null;

	SignShopListener(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	void cancelNextBlockPhysics(Block block) {
		cancelNextBlockPhysics = block;
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Block block = event.getClickedBlock();

		// check for sign shop
		if (event.getAction() == Action.RIGHT_CLICK_BLOCK && ItemUtils.isSign(block.getType())) {
			AbstractShopkeeper shopkeeper = plugin.getShopkeeperRegistry().getShopkeeperByBlock(block);
			if (shopkeeper != null) {
				// only trigger shopkeeper interaction for main-hand events:
				if (event.getHand() == EquipmentSlot.HAND) {
					Log.debug("Player " + player.getName() + " is interacting with sign shopkeeper at " + block.getWorld().getName() + "," + block.getX() + "," + block.getY() + "," + block.getZ());
					if (event.useInteractedBlock() == Result.DENY) {
						Log.debug("  Cancelled by another plugin");
					} else {
						shopkeeper.onPlayerInteraction(player);
					}
				}

				// always cancel interactions with shopkeepers, to prevent any default behavior:
				event.setCancelled(true);
			}
		}
	}

	// protect sign block:

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (ItemUtils.isSign(block.getType())) {
			if (plugin.getShopkeeperRegistry().getShopkeeperByBlock(block) != null) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockPhysics(BlockPhysicsEvent event) {
		Block block = event.getBlock();
		if (this.checkCancelPhysics(block)) {
			event.setCancelled(true);
			return;
		}
		// Spigot changed the behavior of this event in MC 1.13 to reduce the number of event calls:
		// Related: https://hub.spigotmc.org/jira/browse/SPIGOT-4256
		for (BlockFace blockFace : Utils.getBlockSides()) {
			Block adjacentBlock = block.getRelative(blockFace);
			if (this.checkCancelPhysics(adjacentBlock)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	private boolean checkCancelPhysics(Block block) {
		if (cancelNextBlockPhysics != null && cancelNextBlockPhysics.equals(block)) {
			return true;
		} else if (ItemUtils.isSign(block.getType()) && plugin.getShopkeeperRegistry().getShopkeeperByBlock(block) != null) {
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onEntityExplosion(EntityExplodeEvent event) {
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();
			if (ItemUtils.isSign(block.getType()) && plugin.getShopkeeperRegistry().getShopkeeperByBlock(block) != null) {
				iter.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onBlockExplosion(BlockExplodeEvent event) {
		Iterator<Block> iter = event.blockList().iterator();
		while (iter.hasNext()) {
			Block block = iter.next();
			if (ItemUtils.isSign(block.getType()) && plugin.getShopkeeperRegistry().getShopkeeperByBlock(block) != null) {
				iter.remove();
			}
		}
	}
}
