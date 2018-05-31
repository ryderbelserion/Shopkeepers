package com.nisovin.shopkeepers.ui.defaults;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public abstract class HiringHandler extends UIHandler {

	protected HiringHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType, shopkeeper);
	}

	@Override
	protected boolean canOpen(Player player) {
		assert player != null;
		// check for hire permission:
		if (!Utils.hasPermission(player, ShopkeepersPlugin.HIRE_PERMISSION)) {
			Log.debug("Blocked hire window opening from " + player.getName() + ": missing hire permission");
			Utils.sendMessage(player, Settings.msgMissingHirePerm);
			return false;
		}
		return true;
	}

	@Override
	public boolean isWindow(Inventory inventory) {
		return inventory != null && inventory.getTitle().equals(Settings.forHireTitle);
	}

	@Override
	protected void onInventoryClose(InventoryCloseEvent event, Player player) {
		// nothing to do by default
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		assert event != null && player != null;
		event.setCancelled(true);
	}
}
