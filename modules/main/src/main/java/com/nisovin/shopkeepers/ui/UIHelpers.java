package com.nisovin.shopkeepers.ui;

import org.bukkit.Bukkit;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

/**
 * Common user interface helpers and behaviors.
 */
public final class UIHelpers {

	// The delay is for example required during the handling of inventory drag events, because the
	// cancelled drag event resets the cursor afterwards.
	public static void placeOrPickCursorDelayed(InventoryView view, int rawSlot) {
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
			// Check that the player still has the same view open and then freshly get and check the
			// involved items to make sure that players don't abuse this delay:
			if (view.getPlayer().getOpenInventory() != view) return;

			placeOrPickCursor(view, rawSlot);
		});
	}

	// Assumes that any involved inventory event was cancelled.
	public static void placeOrPickCursor(InventoryView view, int rawSlot) {
		ItemStack cursor = view.getCursor();
		ItemStack current = view.getItem(rawSlot);
		if (!ItemUtils.isEmpty(cursor)) {
			if (ItemUtils.isEmpty(current)) {
				// Place item from cursor:
				view.setItem(rawSlot, cursor);
				view.setCursor(null);
			}
		} else if (!ItemUtils.isEmpty(current)) {
			// Pick up item to cursor:
			view.setItem(rawSlot, null);
			view.setCursor(current);
		}
	}

	private UIHelpers() {
	}
}
