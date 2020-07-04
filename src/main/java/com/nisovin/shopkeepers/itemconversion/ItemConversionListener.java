package com.nisovin.shopkeepers.itemconversion;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.events.ShopkeeperOpenUIEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;

public class ItemConversionListener implements Listener {

	ItemConversionListener() {
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	void onOpenShopkeeperUI(ShopkeeperOpenUIEvent event) {
		if (!Settings.convertPlayerItems) return;
		Player player = event.getPlayer();
		Shopkeeper shopkeeper = event.getShopkeeper();
		ItemConversions.convertAffectedItems(player, shopkeeper, true);
	}

	// Notes regarding converting items on more circumstances with the goal of fixing item stacking issues:
	// - We cannot use InventoryOpenEvent here because this does not get triggered for when a player opens his own
	// inventory (this is client side only).
	// - It is also not possible to catch all situations in which a player can receive an item (consider for example
	// other plugins or commands directly adding items to the player's inventory).
	// - The item conversion is not cheap performance wise. We therefore don't want to run it too frequently or under
	// too many circumstances.
	//
	// We therefore don't even attempt to achieve fixing item stacking related issues and instead limit ourselves to
	// issues related to shopkeeper trading.
}
