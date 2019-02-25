package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;

public class BuyingPlayerShopEditorHandler extends PlayerShopEditorHandler {

	protected class EditorSetup extends CommonEditorSetup<BuyingPlayerShopkeeper, PriceOffer> {

		public EditorSetup(BuyingPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		protected List<PriceOffer> getOffers() {
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
		protected TradingRecipeDraft toTradingRecipe(PriceOffer offer) {
			assert offer != null;
			ItemStack currencyItem = Settings.createCurrencyItem(offer.getPrice());
			return new TradingRecipeDraft(currencyItem, offer.getItem(), null);
		}

		@Override
		protected TradingRecipeDraft toTradingRecipe(ItemStack itemFromChest) {
			ItemStack currencyItem = Settings.createZeroCurrencyItem();
			return new TradingRecipeDraft(currencyItem, itemFromChest, null);
		}

		@Override
		protected void clearOffers() {
			shopkeeper.clearOffers();
		}

		@Override
		protected void addOffer(Player player, TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			assert recipe.getItem2() == null;

			ItemStack tradedItem = recipe.getItem1();
			assert tradedItem != null;

			ItemStack priceItem = recipe.getResultItem();
			assert priceItem != null;
			if (priceItem.getType() != Settings.currencyItem) return; // checking this just in case
			assert priceItem.getAmount() > 0;

			shopkeeper.addOffer(tradedItem, priceItem.getAmount());
		}
	}

	protected final EditorSetup setup;

	protected BuyingPlayerShopEditorHandler(BuyingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
		this.setup = new EditorSetup(shopkeeper);
	}

	@Override
	public BuyingPlayerShopkeeper getShopkeeper() {
		return (BuyingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean openWindow(Player player) {
		return setup.openWindow(player);
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		event.setCancelled(true);
		int rawSlot = event.getRawSlot();
		if (this.isResultRow(rawSlot)) {
			// modifying cost:
			int column = rawSlot - RESULT_ITEM_OFFSET;
			ItemStack tradedItem = event.getInventory().getItem(column + ITEM_1_OFFSET);
			if (ItemUtils.isEmpty(tradedItem)) return;
			this.handleUpdateTradeCostItemOnClick(event, Settings.createCurrencyItem(1), Settings.createZeroCurrencyItem());
		} else if (this.isItem1Row(rawSlot)) {
			// modifying bought item quantity:
			this.handleUpdateItemAmountOnClick(event, 1);
		} else if (this.isItem2Row(rawSlot)) {
			// not used by the buying shopkeeper
		} else {
			super.onInventoryClick(event, player);
		}
	}

	@Override
	protected void saveEditor(Inventory inventory, Player player) {
		setup.saveEditor(inventory, player);
	}
}
