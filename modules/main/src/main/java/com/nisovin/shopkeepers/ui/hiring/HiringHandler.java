package com.nisovin.shopkeepers.ui.hiring;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.InventoryView;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ui.UISession;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.AbstractShopkeeperUIHandler;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class HiringHandler extends AbstractShopkeeperUIHandler {

	protected HiringHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType, shopkeeper);
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		Validate.notNull(player, "player is null");
		// Check for hire permission:
		if (!PermissionUtils.hasPermission(player, ShopkeepersPlugin.HIRE_PERMISSION)) {
			if (!silent) {
				this.debugNotOpeningUI(player, "Player is missing the hire permission.");
				TextUtils.sendMessage(player, Messages.missingHirePerm);
			}
			return false;
		}
		return true;
	}

	@Override
	protected boolean isWindow(InventoryView view) {
		return view != null && view.getTitle().equals(Messages.forHireTitle);
	}

	@Override
	protected void onInventoryClose(UISession uiSession, InventoryCloseEvent closeEvent) {
		// Nothing to do by default.
	}

	@Override
	protected void onInventoryClickEarly(UISession uiSession, InventoryClickEvent event) {
		assert uiSession != null && event != null;
		event.setCancelled(true);
	}

	@Override
	protected void onInventoryDragEarly(UISession uiSession, InventoryDragEvent event) {
		assert uiSession != null && event != null;
		event.setCancelled(true);
	}
}
