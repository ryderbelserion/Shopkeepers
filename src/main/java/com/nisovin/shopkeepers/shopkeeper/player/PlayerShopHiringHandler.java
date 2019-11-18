package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.events.PlayerShopkeeperHireEvent;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.ui.defaults.HiringHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;

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
		Inventory inventory = Bukkit.createInventory(player, 9, Settings.forHireTitle);

		ItemStack hireItem = Settings.createHireButtonItem();
		inventory.setItem(BUTTON_HIRE_1, hireItem);
		inventory.setItem(BUTTON_HIRE_2, hireItem);

		ItemStack hireCost = shopkeeper.getHireCost();
		if (hireCost == null) return false;
		inventory.setItem(HIRE_COST, hireCost);

		player.openInventory(inventory);
		return true;
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event, Player player) {
		super.onInventoryClickEarly(event, player);
		PlayerShopkeeper shopkeeper = this.getShopkeeper();
		int slot = event.getRawSlot();
		if (slot == BUTTON_HIRE_1 || slot == BUTTON_HIRE_2) {
			// TODO prevent hiring own shops?
			// actually: this feature was originally meant for admins to set up pre-existing shops
			// handle hiring:
			// check if the player can hire (create) this type of shopkeeper:
			if (Settings.hireRequireCreationPermission && (!this.getShopkeeper().getType().hasPermission(player)
					|| !this.getShopkeeper().getShopObject().getType().hasPermission(player))) {
				// missing permission to hire this type of shopkeeper:
				TextUtils.sendMessage(player, Settings.msgCantHireShopType);
				this.closeDelayed(player);
				return;
			}

			// check if the player can afford it and calculate the resulting player inventory:
			ItemStack[] newPlayerInventoryContents = player.getInventory().getContents();
			ItemStack hireCost = shopkeeper.getHireCost();
			for (int i = 0; i < newPlayerInventoryContents.length; i++) {
				ItemStack item = newPlayerInventoryContents[i];
				if (item != null && item.isSimilar(hireCost)) {
					if (item.getAmount() > hireCost.getAmount()) {
						ItemStack clonedItem = item.clone();
						newPlayerInventoryContents[i] = clonedItem;
						clonedItem.setAmount(item.getAmount() - hireCost.getAmount());
						hireCost.setAmount(0);
						break;
					} else if (item.getAmount() == hireCost.getAmount()) {
						newPlayerInventoryContents[i] = null;
						hireCost.setAmount(0);
						break;
					} else {
						hireCost.setAmount(hireCost.getAmount() - item.getAmount());
						newPlayerInventoryContents[i] = null;
					}
				}
			}

			if (hireCost.getAmount() != 0) {
				// not enough money:
				TextUtils.sendMessage(player, Settings.msgCantHire);
				// close window for this player:
				this.closeDelayed(player);
				return;
			}

			// call event:
			int maxShops = Settings.getMaxShops(player);
			PlayerShopkeeperHireEvent hireEvent = new PlayerShopkeeperHireEvent(shopkeeper, player, newPlayerInventoryContents, maxShops);
			Bukkit.getPluginManager().callEvent(hireEvent);
			if (hireEvent.isCancelled()) {
				Log.debug("PlayerShopkeeperHireEvent was cancelled!");
				// close window for this player:
				this.closeDelayed(player);
				return;
			}

			// check max shops limit:
			maxShops = hireEvent.getMaxShopsLimit();
			if (maxShops > 0) {
				int count = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().getPlayerShopkeepersByOwner(player.getUniqueId()).size();
				if (count >= maxShops) {
					TextUtils.sendMessage(player, Settings.msgTooManyShops);
					this.closeDelayed(player);
					return;
				}
			}

			// hire the shopkeeper:
			player.getInventory().setContents(newPlayerInventoryContents); // apply inventory changes
			shopkeeper.setForHire(null);
			shopkeeper.setOwner(player);
			shopkeeper.save();
			TextUtils.sendMessage(player, Settings.msgHired);

			// close all open windows for this shopkeeper:
			shopkeeper.closeAllOpenWindows();
		}
	}
}
