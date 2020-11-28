package com.nisovin.shopkeepers.shopobjects.sign;

import java.util.Iterator;
import java.util.UUID;

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
import com.nisovin.shopkeepers.util.TestPlayerInteractEvent;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Utils;

class SignShopListener implements Listener {

	// Local copy as array (allows for very performant iteration):
	private static final BlockFace[] BLOCK_SIDES = BlockFaceUtils.getBlockSides().toArray(new BlockFace[0]);

	private final SignShops signShops;

	private static class ModifiableBlockPos {

		private UUID worldId = null; // Null indicates cleared block pos
		private int x;
		private int y;
		private int z;

		// Null to clear
		public void set(Block block) {
			if (block == null) {
				// Clear:
				worldId = null;
			} else {
				worldId = block.getWorld().getUID();
				x = block.getX();
				y = block.getY();
				z = block.getZ();
			}
		}

		public boolean matches(UUID otherWorldId, int otherX, int otherY, int otherZ) {
			assert otherWorldId != null;
			// Comparing world ids by identity should work, since all world ids are retrieved from the same source.
			// See also CraftWorld#equals(Object).
			return this.worldId == otherWorldId && this.x == otherX && this.y == otherY && this.z == otherZ;
		}
	}

	private final ModifiableBlockPos cancelNextBlockPhysics = new ModifiableBlockPos();

	SignShopListener(SignShops signShops) {
		this.signShops = signShops;
	}

	void cancelNextBlockPhysics(Block block) {
		cancelNextBlockPhysics.set(block); // Null to clear
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

		AbstractShopkeeper shopkeeper = signShops.getSignShop(block);
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
		if (ItemUtils.isSign(block.getType()) && signShops.isSignShop(block)) {
			return true;
		}
		for (BlockFace blockFace : BLOCK_SIDES) {
			Block adjacentBlock = block.getRelative(blockFace);
			Shopkeeper shopkeeper = signShops.getSignShop(adjacentBlock);
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
		if (signShops.isSignShop(block)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	void onBlockPhysics(BlockPhysicsEvent event) {
		Block block = event.getBlock();
		World world = block.getWorld();
		String worldName = world.getName();
		UUID worldId = world.getUID();
		int blockX = block.getX();
		int blockY = block.getY();
		int blockZ = block.getZ();
		if (this.checkCancelPhysics(worldName, worldId, blockX, blockY, blockZ)) {
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
			if (this.checkCancelPhysics(worldName, worldId, adjacentX, adjacentY, adjacentZ)) {
				event.setCancelled(true);
				return;
			}
		}
	}

	private boolean checkCancelPhysics(String worldName, UUID worldId, int blockX, int blockY, int blockZ) {
		if (cancelNextBlockPhysics.matches(worldId, blockX, blockY, blockZ)) {
			return true;
		} else if (signShops.isSignShop(worldName, blockX, blockY, blockZ)) {
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
