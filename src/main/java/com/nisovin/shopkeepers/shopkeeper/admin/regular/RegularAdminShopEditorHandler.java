package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.Collections;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.util.ItemCount;

public class RegularAdminShopEditorHandler extends EditorHandler {

	protected class EditorSetup extends CommonEditorSetup<RegularAdminShopkeeper, TradingOffer> {

		public EditorSetup(RegularAdminShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		protected List<TradingOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected List<ItemCount> getItemsFromChest() {
			return Collections.emptyList();
		}

		@Override
		protected boolean hasOffer(ItemStack itemFromChest) {
			return false;
		}

		@Override
		protected TradingRecipeDraft toTradingRecipe(TradingOffer offer) {
			assert offer != null;
			return new TradingRecipeDraft(offer.getResultItem(), offer.getItem1(), offer.getItem2());
		}

		@Override
		protected TradingRecipeDraft toTradingRecipe(ItemStack itemFromChest) {
			return null;
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

		@Override
		protected void handleInvalidRecipeDraft(Player player, TradingRecipeDraft recipe) {
			super.handleInvalidRecipeDraft(player, recipe);

			// return unused items to inventory:
			ItemStack resultItem = recipe.getResultItem();
			ItemStack item1 = recipe.getItem1();
			ItemStack item2 = recipe.getItem2();
			PlayerInventory playerInventory = player.getInventory();

			if (item1 != null) {
				playerInventory.addItem(item1);
			}
			if (item2 != null) {
				playerInventory.addItem(item2);
			}
			if (resultItem != null) {
				playerInventory.addItem(resultItem);
			}
		}
	}

	protected final EditorSetup setup;

	protected RegularAdminShopEditorHandler(RegularAdminShopkeeper shopkeeper) {
		super(SKDefaultUITypes.EDITOR(), shopkeeper);
		this.setup = new EditorSetup(shopkeeper);
	}

	@Override
	public RegularAdminShopkeeper getShopkeeper() {
		return (RegularAdminShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean canOpen(Player player) {
		assert player != null;
		return super.canOpen(player) && this.getShopkeeper().getType().hasPermission(player);
	}

	@Override
	protected boolean openWindow(Player player) {
		return setup.openWindow(player);
	}

	@Override
	protected void saveEditor(Inventory inventory, Player player) {
		setup.saveEditor(inventory, player);
	}
}