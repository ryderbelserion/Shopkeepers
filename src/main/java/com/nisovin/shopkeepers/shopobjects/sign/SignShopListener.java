package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Iterator;

import org.bukkit.World;
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

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.BlockFaceUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.MutableBlockLocation;
import com.nisovin.shopkeepers.util.TestPlayerInteractEvent;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Utils;

class SignShopListener implements Listener {

	// Local copy as array (enables a very high-performance iteration):
	private static final BlockFace[] BLOCK_SIDES = BlockFaceUtils.getBlockSides().toArray(new BlockFace[0]);

	private final SKSignShopObjectType signShopObjectType;

	private final MutableBlockLocation cancelNextBlockPhysics = new MutableBlockLocation();

	SignShopListener(SignShops signShops) {
		this.signShopObjectType = signShops.getSignShopObjectType();
	}

	// Null to clear
	void cancelNextBlockPhysics(Block block) {
		if (block == null) {
			cancelNextBlockPhysics.unsetWorldName();
		} else {
			cancelNextBlockPhysics.set(block);
		}
	}

	// See LivingEntityShopListener for a reasoning behind using event priority LOWEST.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	void onPlayerInteract(PlayerInteractEvent event) {
		// Ignore our own fake interact event:
		if (event instanceof TestPlayerInteractEvent) return;

		// Check for sign shop interaction:
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;

		Block block = event.getClickedBlock();
		if (!ItemUtils.isSign(block.getType())) return;

		Player player = event.getPlayer();
		Log.debug(() -> "Player " + player.getName() + " is interacting (" + (event.getHand()) + ") with sign at " + TextUtils.getLocationString(block));

		AbstractShopkeeper shopkeeper = signShopObjectType.getShopkeeper(block);
		if (shopkeeper == null) {
			Log.debug("  Non-shopkeeper");
			return;
		}

		// Keep track of the previous interaction result:
		boolean useInteractedBlock = (event.useInteractedBlock() != Result.DENY);

		// Always cancel interactions with shopkeepers, to prevent any default behavior:
		event.setCancelled(true); // Also cancels the item interaction
		// Update inventory in case the interaction would trigger an item action normally:
		player.updateInventory();

		// Ignore if already cancelled. Resolves conflicts with other event handlers running at LOWEST priority (eg.
		// Shopkeepers' shop creation item listener acts on LOWEST priority as well).
		if (!useInteractedBlock) {
			Log.debug("  Ignoring already cancelled block interaction");
			return;
		}

		// Only trigger shopkeeper interaction for main-hand events:
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

		// Handle interaction:
		shopkeeper.onPlayerInteraction(player);
	}

	// Protect sign block:

	private boolean isProtectedBlock(Block block) {
		// Not protected if the sign shop is not active (if the block is not a sign currently):
		if (ItemUtils.isSign(block.getType()) && signShopObjectType.isShopkeeper(block)) {
			return true;
		}
		for (BlockFace blockFace : BLOCK_SIDES) {
			Block adjacentBlock = block.getRelative(blockFace);
			Shopkeeper shopkeeper = signShopObjectType.getShopkeeper(adjacentBlock);
			if (shopkeeper != null) {
				SKSignShopObject signObject = (SKSignShopObject) shopkeeper.getShopObject();
				BlockFace attachedFace = BlockFace.UP; // In case of sign post
				if (signObject.isWallSign()) {
					attachedFace = signObject.getSignFacing();
				}
				if (blockFace == attachedFace) {
					// Sign is (supposed to be) / might be attached to the given block:
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
		if (signShopObjectType.isShopkeeper(block)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockPhysics(BlockPhysicsEvent event) {
		Block block = event.getBlock();
		World world = block.getWorld();
		String worldName = world.getName();
		int blockX = block.getX();
		int blockY = block.getY();
		int blockZ = block.getZ();
		if (this.checkCancelPhysics(worldName, blockX, blockY, blockZ)) {
			event.setCancelled(true);
			return;
		}
		// Spigot changed the behavior of this event in MC 1.13 to reduce the number of event calls:
		// Related: https://hub.spigotmc.org/jira/browse/SPIGOT-4256
		for (BlockFace blockFace : BLOCK_SIDES) {
			// Note: Avoiding getting the adjacent block slightly improves performance of handling this event.
			int adjacentX = blockX + blockFace.getModX();
			int adjacentY = blockY + blockFace.getModY();
			int adjacentZ = blockZ + blockFace.getModZ();
			if (this.checkCancelPhysics(worldName, adjacentX, adjacentY, adjacentZ)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	private boolean checkCancelPhysics(String worldName, int blockX, int blockY, int blockZ) {
		if (cancelNextBlockPhysics.equals(worldName, blockX, blockY, blockZ)) {
			return true;
		} else if (signShopObjectType.isShopkeeper(worldName, blockX, blockY, blockZ)) {
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
