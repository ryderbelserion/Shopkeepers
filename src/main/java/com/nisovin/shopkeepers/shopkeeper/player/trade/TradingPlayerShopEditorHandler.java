package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;

public class TradingPlayerShopEditorHandler extends PlayerShopEditorHandler {

	protected class EditorSetup extends CommonEditorSetup<TradingPlayerShopkeeper, TradingOffer> {

		public EditorSetup(TradingPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		protected List<TradingOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected List<ItemCount> getItemsFromChest() {
			return shopkeeper.getItemsFromChest();
		}

		@Override
		protected boolean hasOffer(ItemStack itemFromChest) {
			return (shopkeeper.getOffer(itemFromChest) != null);
		}

		@Override
		protected TradingRecipeDraft toTradingRecipe(TradingOffer offer) {
			assert offer != null;
			return new TradingRecipeDraft(offer.getResultItem(), offer.getItem1(), offer.getItem2());
		}

		@Override
		protected TradingRecipeDraft toTradingRecipe(ItemStack itemFromChest) {
			return new TradingRecipeDraft(itemFromChest, null, null);
		}

		@Override
		protected void clearOffers() {
			shopkeeper.clearOffers();
		}

		@Override
		protected void addOffer(Player player, TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			shopkeeper.addOffer(recipe.getResultItem(), recipe.getItem1(), recipe.getItem2());
		}
	}

	protected final EditorSetup setup;

	protected TradingPlayerShopEditorHandler(TradingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
		this.setup = new EditorSetup(shopkeeper);
	}

	@Override
	public TradingPlayerShopkeeper getShopkeeper() {
		return (TradingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean openWindow(Player player) {
		return setup.openWindow(player);
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		event.setCancelled(true);
		int slot = event.getRawSlot();
		boolean resultRow = this.isResultRow(slot);
		if (resultRow || this.isItem1Row(slot) || this.isItem2Row(slot)) {
			ItemStack cursor = event.getCursor();
			if (!ItemUtils.isEmpty(cursor)) {
				// place item from cursor:
				Inventory inventory = event.getInventory();
				ItemStack cursorClone = cursor.clone();
				cursorClone.setAmount(1);
				Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
					inventory.setItem(slot, cursorClone);
				});
			} else {
				// changing stack size of clicked item:
				this.handleUpdateItemAmountOnClick(event, resultRow ? 1 : 0);
			}
		} else if (this.isPlayerInventory(event.getView(), event.getSlotType(), slot)) {
			// clicking in player inventory:
			if (event.isShiftClick()) {
				return;
			}
			ItemStack cursor = event.getCursor();
			ItemStack current = event.getCurrentItem();
			if (!ItemUtils.isEmpty(cursor)) {
				if (ItemUtils.isEmpty(current)) {
					// place item from cursor:
					event.setCurrentItem(cursor);
					event.setCursor(null);
				}
			} else if (!ItemUtils.isEmpty(current)) {
				// pick up item to cursor:
				event.setCurrentItem(null);
				event.setCursor(current);
			}
		} else {
			super.onInventoryClick(event, player);
		}
	}

	@Override
	protected void onInventoryDrag(InventoryDragEvent event, Player player) {
		event.setCancelled(true);
		ItemStack cursor = event.getOldCursor();
		// assert: cursor item is already a clone
		if (ItemUtils.isEmpty(cursor)) return;

		Set<Integer> slots = event.getRawSlots();
		if (slots.size() != 1) return;

		int slot = slots.iterator().next();
		if ((slot >= 0 && slot < TRADE_COLUMNS) || (slot >= 9 && slot <= 16) || (slot >= 18 && slot <= 25)) {
			// place item from cursor:
			Inventory inventory = event.getInventory();
			cursor.setAmount(1);
			Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
				inventory.setItem(slot, cursor);
			});
		} else if (slot >= 27) {
			// clicking in player inventory:
			InventoryView view = event.getView();
			// the cancelled drag event resets the cursor afterwards, so we need this delay:
			Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
				// freshly get and check cursor to make sure that players don't abuse this delay:
				ItemStack cursorCurrent = view.getCursor();
				if (ItemUtils.isEmpty(cursorCurrent)) return;
				ItemStack current = view.getItem(slot);
				if (ItemUtils.isEmpty(current)) {
					// place item from cursor:
					view.setItem(slot, cursorCurrent);
					view.setCursor(null);
				}
			});
		}
	}

	@Override
	protected void saveEditor(Inventory inventory, Player player) {
		setup.saveEditor(inventory, player);
	}
}
