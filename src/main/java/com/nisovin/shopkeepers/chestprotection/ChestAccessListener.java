package com.nisovin.shopkeepers.chestprotection;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

/**
 * Prevents unauthorized opening of protected chests.
 * TODO combine this with ChestProtectionListener? Is there a need for this if chest protection is disabled?
 */
class ChestAccessListener implements Listener {

	private final ProtectedChests protectedChests;

	ChestAccessListener(ProtectedChests protectedChests) {
		this.protectedChests = protectedChests;
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
				if (protectedChests.isChestProtected(block, player)) {
					// TODO always allow access to own shop chests, even if cancelled by other plugins?
					Log.debug("Cancelled chest opening by '" + player.getName() + "' at '"
							+ Utils.getLocationString(block) + "': Protected chest");
					event.setCancelled(true);
				}
			}
		}
	}
}
