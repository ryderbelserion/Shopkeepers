package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.ItemCount;

public class SellingPlayerShopEditorHandler extends PlayerShopEditorHandler {

	protected class EditorSetup extends CommonEditorSetup<SellingPlayerShopkeeper, PriceOffer> {

		public EditorSetup(SellingPlayerShopkeeper shopkeeper) {
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
			return createTradingRecipeDraft(offer.getItem(), offer.getPrice());
		}

		@Override
		protected TradingRecipeDraft toTradingRecipe(ItemStack itemFromChest) {
			return createTradingRecipeDraft(itemFromChest, 0);
		}

		@Override
		protected void clearOffers() {
			shopkeeper.clearOffers();
		}

		@Override
		protected void addOffer(Player player, TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			int price = getPrice(recipe);
			if (price <= 0) return;
			shopkeeper.addOffer(recipe.getResultItem(), price);
		}
	}

	protected final EditorSetup setup;

	protected SellingPlayerShopEditorHandler(SellingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
		this.setup = new EditorSetup(shopkeeper);
	}

	@Override
	public SellingPlayerShopkeeper getShopkeeper() {
		return (SellingPlayerShopkeeper) super.getShopkeeper();
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
			// handle changing sell stack size:
			this.handleUpdateItemAmountOnClick(event, 1);
		} else {
			super.onInventoryClick(event, player);
		}
	}

	@Override
	protected void saveEditor(Inventory inventory, Player player) {
		setup.saveEditor(inventory, player);
	}
}
