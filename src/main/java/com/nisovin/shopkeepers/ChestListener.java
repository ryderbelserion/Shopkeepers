package com.nisovin.shopkeepers;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

class ChestListener implements Listener {

	private final SKShopkeepersPlugin plugin;

	ChestListener(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onBlockPlace(BlockPlaceEvent event) {
		Block block = event.getBlock();
		if (ItemUtils.isChest(block.getType())) {
			plugin.onChestPlacement(event.getPlayer(), block);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	void onPlayerInteract(PlayerInteractEvent event) {
		// prevent opening shop chests
		if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Block block = event.getClickedBlock();
		if (event.hasBlock() && ItemUtils.isChest(block.getType())) {
			Player player = event.getPlayer();

			// check for protected chest
			if (!Utils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
				if (plugin.getProtectedChests().isChestProtected(block, player)) {
					// TODO always allow access to own shop chests, even if cancelled by other plugins?
					Log.debug("Cancelled chest opening by '" + player.getName() + "' at '"
							+ Utils.getLocationString(block) + "': Protected chest");
					event.setCancelled(true);
				}
			}
		}
	}
}
