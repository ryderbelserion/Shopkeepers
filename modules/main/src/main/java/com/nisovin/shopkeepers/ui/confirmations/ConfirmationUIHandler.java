package com.nisovin.shopkeepers.ui.confirmations;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class ConfirmationUIHandler extends UIHandler {

	private static final int INVENTORY_SIZE = 9;
	private static final int SLOT_CONFIRM = 0;
	private static final int SLOT_CANCEL = 8;

	private final ConfirmationUIConfig config;
	private final Runnable action;
	// This is invoked when the player explicitly presses the 'cancel' button. It is not invoked if the player closes
	// the inventory, either directly or because something else closes it (for example when another inventory is opened
	// for the player).
	private final Runnable onCancelled;
	private boolean playerDecided = false;

	ConfirmationUIHandler(ConfirmationUIConfig config, Runnable action, Runnable onCancelled) {
		super(SKDefaultUITypes.CONFIRMATION());
		assert config != null && action != null && onCancelled != null;
		this.config = config;
		this.action = action;
		this.onCancelled = onCancelled;
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		// Players cannot directly request this UI themselves. It is always opened for them in some context.
		return true;
	}

	@Override
	protected boolean isWindow(InventoryView view) {
		return view != null && view.getType() == InventoryType.CHEST && view.getTopInventory().getSize() == INVENTORY_SIZE;
	}

	@Override
	protected boolean openWindow(Player player) {
		Inventory inventory = Bukkit.createInventory(player, INVENTORY_SIZE, config.getTitle());

		ItemStack confirmItem = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
		confirmItem = ItemUtils.setDisplayNameAndLore(confirmItem, Messages.confirmationUiConfirm, config.getConfirmationLore());
		inventory.setItem(SLOT_CONFIRM, confirmItem);

		ItemStack cancelItem = new ItemStack(Material.RED_STAINED_GLASS_PANE);
		cancelItem = ItemUtils.setDisplayNameAndLore(cancelItem, Messages.confirmationUiCancel, Messages.confirmationUiCancelLore);
		inventory.setItem(SLOT_CANCEL, cancelItem);

		player.openInventory(inventory);
		return true;
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event, Player player) {
		assert event != null && player != null;
		event.setCancelled(true);
		if (this.isAutomaticShiftLeftClick()) {
			// Ignore automatically triggered shift left-clicks:
			return;
		}

		int slot = event.getRawSlot();
		if (slot == SLOT_CONFIRM) {
			playerDecided = true;
			this.getUISession(player).closeDelayedAndRunTask(action);
		} else if (slot == SLOT_CANCEL) {
			playerDecided = true;
			this.getUISession(player).closeDelayedAndRunTask(onCancelled);
		}
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event, Player player) {
		assert event != null && player != null;
		event.setCancelled(true);
	}

	@Override
	protected void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
		assert player != null;
		if (!playerDecided) {
			TextUtils.sendMessage(player, Messages.confirmationUiAborted);
		}
	}
}
