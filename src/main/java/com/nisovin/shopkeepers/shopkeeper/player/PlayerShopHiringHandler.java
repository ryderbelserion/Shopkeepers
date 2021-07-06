package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerShopkeeperHireEvent;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.playershops.PlayerShopsLimit;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.hiring.HiringHandler;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class PlayerShopHiringHandler extends HiringHandler {

	protected static final int HIRE_COST = 4;
	protected static final int BUTTON_HIRE_1 = 2;
	protected static final int BUTTON_HIRE_2 = 6;

	public PlayerShopHiringHandler(AbstractPlayerShopkeeper shopkeeper) {
		super(SKDefaultUITypes.HIRING(), shopkeeper);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean openWindow(Player player) {
		PlayerShopkeeper shopkeeper = this.getShopkeeper();
		Inventory inventory = Bukkit.createInventory(player, 9, Messages.forHireTitle);

		ItemStack hireItem = Settings.createHireButtonItem();
		inventory.setItem(BUTTON_HIRE_1, hireItem);
		inventory.setItem(BUTTON_HIRE_2, hireItem);

		UnmodifiableItemStack hireCost = shopkeeper.getHireCost();
		if (hireCost == null) return false;
		// Inventory#setItem copies the item, so we do not need to copy it ourselves here.
		inventory.setItem(HIRE_COST, hireCost.asItemStack());

		player.openInventory(inventory);
		return true;
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event, Player player) {
		super.onInventoryClickEarly(event, player);
		if (this.isAutomaticShiftLeftClick()) {
			// Ignore automatically triggered shift left-clicks:
			return;
		}

		PlayerShopkeeper shopkeeper = this.getShopkeeper();
		int slot = event.getRawSlot();
		if (slot == BUTTON_HIRE_1 || slot == BUTTON_HIRE_2) {
			// TODO Prevent hiring own shops?
			// Actually: This feature was originally meant for admins to set up pre-existing shops.
			// Handle hiring:
			// Check if the player can hire (create) this type of shopkeeper:
			if (Settings.hireRequireCreationPermission && (!this.getShopkeeper().getType().hasPermission(player)
					|| !this.getShopkeeper().getShopObject().getType().hasPermission(player))) {
				// Missing permission to hire this type of shopkeeper:
				TextUtils.sendMessage(player, Messages.cannotHireShopType);
				this.getUISession(player).abortDelayed();
				return;
			}

			UnmodifiableItemStack hireCost = shopkeeper.getHireCost();
			if (hireCost == null) {
				// The shopkeeper is no longer for hire.
				// TODO Maybe instead ensure that we always close all hiring UIs when the hiring item changes.
				// TODO Send a feedback message to the player
				this.getUISession(player).abortDelayed();
				return;
			}

			// Check if the player can afford to hire the shopkeeper, and calculate the resulting player inventory:
			PlayerInventory playerInventory = player.getInventory();
			ItemStack[] newPlayerInventoryContents = playerInventory.getContents();
			if (InventoryUtils.removeItems(newPlayerInventoryContents, hireCost) != 0) {
				// The player cannot afford to hire the shopkeeper:
				TextUtils.sendMessage(player, Messages.cannotHire);
				// Close window for this player:
				this.getUISession(player).abortDelayed();
				return;
			}

			// Call event:
			int maxShopsLimit = PlayerShopsLimit.getMaxShopsLimit(player);
			PlayerShopkeeperHireEvent hireEvent = new PlayerShopkeeperHireEvent(shopkeeper, player, newPlayerInventoryContents, maxShopsLimit);
			Bukkit.getPluginManager().callEvent(hireEvent);
			if (hireEvent.isCancelled()) {
				Log.debug("PlayerShopkeeperHireEvent was cancelled!");
				// Close window for this player:
				this.getUISession(player).abortDelayed();
				return;
			}

			// Check max shops limit:
			maxShopsLimit = hireEvent.getMaxShopsLimit();
			if (maxShopsLimit != Integer.MAX_VALUE) {
				int count = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().getPlayerShopkeepersByOwner(player.getUniqueId()).size();
				if (count >= maxShopsLimit) {
					TextUtils.sendMessage(player, Messages.tooManyShops);
					this.getUISession(player).abortDelayed();
					return;
				}
			}

			// Hire the shopkeeper:
			InventoryUtils.setContents(playerInventory, newPlayerInventoryContents); // Apply player inventory changes
			shopkeeper.setForHire(null);
			shopkeeper.setOwner(player);
			shopkeeper.save();
			TextUtils.sendMessage(player, Messages.hired);

			// Close all open windows for the shopkeeper:
			shopkeeper.abortUISessionsDelayed();
		}
	}
}
