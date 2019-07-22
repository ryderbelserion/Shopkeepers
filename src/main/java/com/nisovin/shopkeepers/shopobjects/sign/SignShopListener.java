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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TestPlayerInteractEvent;
import com.nisovin.shopkeepers.util.Utils;

class SignShopListener implements Listener {

	// local copy as array (allows very performant iteration):
	private static final BlockFace[] BLOCK_SIDES = Utils.getBlockSides().toArray(new BlockFace[0]);

	private final SignShops signShops;

	private Block cancelNextBlockPhysics = null;

	SignShopListener(SignShops signShops) {
		this.signShops = signShops;
	}

	void cancelNextBlockPhysics(Block block) {
		cancelNextBlockPhysics = block;
	}

	// See LivingEntityShopListener for a reasoning behind using event priority LOWEST.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		// ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEvent) return;

		// check for sign shop interaction:
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Block block = event.getClickedBlock();
		if (!ItemUtils.isSign(block.getType())) return;

		Player player = event.getPlayer();
		Log.debug("Player " + player.getName() + " is interacting (" + (event.getHand()) + ") with sign at " + Utils.getLocationString(block));

		AbstractShopkeeper shopkeeper = signShops.getSignShop(block);
		if (shopkeeper == null) {
			Log.debug("  Non-shopkeeper");
			return;
		}

		// keep track of the previous interaction result:
		boolean useInteractedBlock = (event.useInteractedBlock() != Result.DENY);

		// always cancel interactions with shopkeepers, to prevent any default behavior:
		event.setCancelled(true); // also cancels the item interaction
		// update inventory in case the interaction would trigger an item action normally:
		player.updateInventory();

		// Ignore if already cancelled. Resolves conflicts with other event handlers running at LOWEST priority (eg.
		// Shopkeepers' shop creation item listener acts on LOWEST priority as well).
		if (!useInteractedBlock) {
			Log.debug("  Ignoring already cancelled block interaction");
			return;
		}

		// only trigger shopkeeper interaction for main-hand events:
		if (event.getHand() != EquipmentSlot.HAND) {
			Log.debug("  Ignoring off-hand interaction");
			return;
		}

		// Check the sign interaction result by calling another interact event:
		if (Settings.checkShopInteractionResult) {
			if (!Utils.checkBlockInteract(player, block)) {
				Log.debug("  Cancelled by another plugin");
				return;
			}
		}

		// handle interaction:
		shopkeeper.onPlayerInteraction(player);
	}

	// protect sign block:

	private boolean isProtectedBlock(Block block) {
		// not protected if the sign shop is not active (if the block is not a sign currently):
		if (ItemUtils.isSign(block.getType()) && signShops.isSignShop(block)) {
			return true;
		}
		for (BlockFace blockFace : BLOCK_SIDES) {
			Block adjacentBlock = block.getRelative(blockFace);
			Shopkeeper shopkeeper = signShops.getSignShop(adjacentBlock);
			if (shopkeeper != null) {
				SKSignShopObject signObject = (SKSignShopObject) shopkeeper.getShopObject();
				BlockFace attachedFace = BlockFace.UP; // in case of sign post
				if (signObject.isWallSign()) {
					attachedFace = signObject.getSignFacing();
				}
				if (blockFace == attachedFace) {
					// sign is (supposed to be) / might be attached to the given block:
					return true;
				}
			}
		}
		return false;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockBreak(BlockBreakEvent event) {
		Block block = event.getBlock();
		if (this.isProtectedBlock(block)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (signShops.isSignShop(block)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockPhysics(BlockPhysicsEvent event) {
		Block block = event.getBlock();
		if (this.checkCancelPhysics(block)) {
			event.setCancelled(true);
			return;
		}
		// Spigot changed the behavior of this event in MC 1.13 to reduce the number of event calls:
		// Related: https://hub.spigotmc.org/jira/browse/SPIGOT-4256
		for (BlockFace blockFace : BLOCK_SIDES) {
			Block adjacentBlock = block.getRelative(blockFace.getModX(), blockFace.getModY(), blockFace.getModZ());
			if (this.checkCancelPhysics(adjacentBlock)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	private boolean checkCancelPhysics(Block block) {
		if (cancelNextBlockPhysics != null && cancelNextBlockPhysics.equals(block)) {
			return true;
		} else if (signShops.isSignShop(block)) {
			return true;
		}
		return false;
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onEntityExplosion(EntityExplodeEvent event) {
		Iterator<Block> iterator = event.blockList().iterator();
		while (iterator.hasNext()) {
			Block block = iterator.next();
			if (this.isProtectedBlock(block)) {
				iterator.remove();
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockExplosion(BlockExplodeEvent event) {
		Iterator<Block> iterator = event.blockList().iterator();
		while (iterator.hasNext()) {
			Block block = iterator.next();
			if (this.isProtectedBlock(block)) {
				iterator.remove();
			}
		}
	}
}
