package com.nisovin.shopkeepers.shopkeeper.player;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.PermissionUtils;

public abstract class PlayerShopEditorHandler extends EditorHandler {

	// Note: In the editor item1 is representing the low cost item and item2 the high cost item, but in the
	// corresponding trading recipe they will be swapped if they are both present.

	protected PlayerShopEditorHandler(AbstractPlayerShopkeeper shopkeeper) {
		super(SKDefaultUITypes.EDITOR(), shopkeeper);
	}

	@Override
	public AbstractPlayerShopkeeper getShopkeeper() {
		return (AbstractPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		if (!super.canOpen(player, silent)) return false;
		return (this.getShopkeeper().isOwner(player) || PermissionUtils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION));
	}

	@Override
	protected void setupShopkeeperButtons() {
		super.setupShopkeeperButtons();
		this.addButtonOrIgnore(this.createContainerButton());
	}

	protected Button createContainerButton() {
		if (!Settings.enableContainerOptionOnPlayerShop) {
			return null;
		}
		return new Button() {
			@Override
			public ItemStack getIcon(Session session) {
				return Settings.createContainerButtonItem();
			}

			@Override
			protected void onClick(InventoryClickEvent clickEvent, Player player) {
				// Closing the UI also triggers a save of the current editor state:
				getUISession(player).closeDelayedAndRunTask(() -> {
					// Open the shop container inventory:
					PlayerShopkeeper shopkeeper = getShopkeeper();
					if (!player.isValid() || !shopkeeper.isValid()) return;
					shopkeeper.openContainerWindow(player);
				});
			}
		};
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event, Player player) {
		// Cancel all inventory clicks and handle everything on our own:
		// TODO Maybe allow certain inventory actions which only affect the player's inventory?
		event.setCancelled(true);
		super.onInventoryDragEarly(event, player);
	}

	@Override
	protected void onInventoryClickEarly(InventoryClickEvent event, Player player) {
		// Cancel all inventory clicks and handle everything on our own:
		// TODO Maybe allow certain inventory actions which only affect the player's inventory (like moving items
		// around)?
		event.setCancelled(true);
		super.onInventoryClickEarly(event, player);
	}

	@Override
	protected void handleTradesClick(Session session, InventoryClickEvent event) {
		super.handleTradesClick(session, event);
		int rawSlot = event.getRawSlot();
		if (this.isItem1Row(rawSlot)) {
			// Change low cost:
			int column = rawSlot - ITEM_1_OFFSET;
			ItemStack item = event.getInventory().getItem(column);
			if (ItemUtils.isEmpty(item)) return;
			this.handleUpdateTradeCostItemOnClick(event, Settings.createCurrencyItem(1), Settings.createZeroCurrencyItem());
		} else if (this.isItem2Row(rawSlot)) {
			// Change high cost:
			int column = rawSlot - ITEM_2_OFFSET;
			ItemStack item = event.getInventory().getItem(column);
			if (ItemUtils.isEmpty(item)) return;
			this.handleUpdateTradeCostItemOnClick(event, Settings.createHighCurrencyItem(1), Settings.createZeroHighCurrencyItem());
		}
	}

	protected void handleUpdateItemAmountOnClick(InventoryClickEvent event, int minAmount) {
		assert event.isCancelled();
		// Ignore in certain situations:
		ItemStack clickedItem = event.getCurrentItem();
		if (ItemUtils.isEmpty(clickedItem)) return;

		// Get new item amount:
		int currentItemAmount = clickedItem.getAmount();
		if (minAmount <= 0) minAmount = 0;
		int newItemAmount = this.getNewAmountAfterEditorClick(event, currentItemAmount, minAmount, clickedItem.getMaxStackSize());
		assert newItemAmount >= minAmount;
		assert newItemAmount <= clickedItem.getMaxStackSize();

		// Update item in inventory:
		if (newItemAmount == 0) {
			// Empty item slot:
			event.setCurrentItem(null);
		} else {
			clickedItem.setAmount(newItemAmount);
		}
	}

	protected void handleUpdateTradeCostItemOnClick(InventoryClickEvent event, ItemStack currencyItem, ItemStack zeroCurrencyItem) {
		assert event.isCancelled();
		// Ignore in certain situations:
		if (ItemUtils.isEmpty(currencyItem)) return;

		// Get new item amount:
		ItemStack clickedItem = event.getCurrentItem(); // Can be null
		int currentItemAmount = 0;
		boolean isCurrencyItem = ItemUtils.isSimilar(clickedItem, currencyItem);
		if (isCurrencyItem) {
			assert clickedItem != null;
			currentItemAmount = clickedItem.getAmount();
		}
		int newItemAmount = this.getNewAmountAfterEditorClick(event, currentItemAmount, 0, currencyItem.getMaxStackSize());
		assert newItemAmount >= 0;
		assert newItemAmount <= currencyItem.getMaxStackSize();

		// Update item in inventory:
		if (newItemAmount == 0) {
			// Place zero-currency item:
			event.setCurrentItem(zeroCurrencyItem);
		} else {
			if (isCurrencyItem) {
				// Only update item amount of already existing currency item:
				clickedItem.setAmount(newItemAmount);
			} else {
				// Place currency item with new amount:
				currencyItem.setAmount(newItemAmount);
				event.setCurrentItem(currencyItem);
			}
		}
	}

	// Note: In case the cost is too large to represent, it sets the cost to zero.
	// (So opening and closing the editor window will remove the offer, instead of setting the costs to a lower
	// value than what was previously somehow specified)
	protected TradingRecipeDraft createTradingRecipeDraft(ItemStack resultItem, int cost) {
		ItemStack highCostItem = null;
		ItemStack lowCostItem = null;

		int remainingCost = cost;
		if (Settings.isHighCurrencyEnabled()) {
			int highCost = 0;
			if (remainingCost > Settings.highCurrencyMinCost) {
				highCost = Math.min((remainingCost / Settings.highCurrencyValue), Settings.highCurrencyItem.getType().getMaxStackSize());
			}
			if (highCost > 0) {
				remainingCost -= (highCost * Settings.highCurrencyValue);
				highCostItem = Settings.createHighCurrencyItem(highCost);
			} else {
				highCostItem = Settings.createZeroHighCurrencyItem();
			}
		}
		if (remainingCost > 0) {
			if (remainingCost <= Settings.currencyItem.getType().getMaxStackSize()) {
				lowCostItem = Settings.createCurrencyItem(remainingCost);
			} else {
				// Cost is to large to represent: reset cost to zero:
				lowCostItem = Settings.createZeroCurrencyItem();
				if (Settings.isHighCurrencyEnabled()) {
					highCostItem = Settings.createZeroHighCurrencyItem();
				}
			}
		} else {
			lowCostItem = Settings.createZeroCurrencyItem();
		}

		return new TradingRecipeDraft(resultItem, lowCostItem, highCostItem);
	}

	protected int getPrice(TradingRecipeDraft recipe) {
		if (recipe == null) return 0;
		ItemStack lowCostItem = recipe.getItem1();
		ItemStack highCostItem = recipe.getItem2();
		int price = 0;
		if (lowCostItem != null && lowCostItem.getType() == Settings.currencyItem.getType() && lowCostItem.getAmount() > 0) {
			price += lowCostItem.getAmount();
		}
		if (Settings.isHighCurrencyEnabled() && highCostItem != null && highCostItem.getType() == Settings.highCurrencyItem.getType() && highCostItem.getAmount() > 0) {
			price += (highCostItem.getAmount() * Settings.highCurrencyValue);
		}
		return price;
	}
}
