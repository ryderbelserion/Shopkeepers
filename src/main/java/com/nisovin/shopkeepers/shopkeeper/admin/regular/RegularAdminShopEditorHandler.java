package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;

public class RegularAdminShopEditorHandler extends EditorHandler {

	private static class TradingRecipesUpdater extends DefaultTradingRecipesAdapter<TradeOffer> {

		private final SKRegularAdminShopkeeper shopkeeper;

		private TradingRecipesUpdater(SKRegularAdminShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<TradingRecipeDraft> getTradingRecipes() {
			// Add the shopkeeper's offers:
			List<? extends TradeOffer> offers = shopkeeper.getOffers();
			List<TradingRecipeDraft> recipes = new ArrayList<>(offers.size());
			offers.forEach(offer -> {
				// The offer returns copies of its items:
				TradingRecipeDraft recipe = new TradingRecipeDraft(offer.getResultItem(), offer.getItem1(), offer.getItem2());
				recipes.add(recipe);
			});
			return recipes;
		}

		@Override
		protected List<? extends TradeOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected void setOffers(List<TradeOffer> newOffers) {
			shopkeeper.setOffers(newOffers);
		}

		@Override
		protected TradeOffer createOffer(TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			ItemStack resultItem = recipe.getResultItem();
			ItemStack item1 = recipe.getItem1();
			ItemStack item2 = recipe.getItem2();
			return ShopkeepersAPI.createTradeOffer(resultItem, item1, item2);
		}

		// TODO Remove this? Maybe handle the trades setup similar to the player trading shop: Copying the selected
		// items into the editor.
		@Override
		protected void handleInvalidTradingRecipe(Player player, TradingRecipeDraft invalidRecipe) {
			// Return unused items to inventory:
			ItemStack resultItem = invalidRecipe.getResultItem();
			ItemStack item1 = invalidRecipe.getItem1();
			ItemStack item2 = invalidRecipe.getItem2();
			PlayerInventory playerInventory = player.getInventory();

			// Note: If the items don't fit the inventory, we ignore them rather then dropping them. This is usually
			// safer than having admins accidentally drop items when they are setting up admin shops.
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

	protected RegularAdminShopEditorHandler(SKRegularAdminShopkeeper shopkeeper) {
		super(SKDefaultUITypes.EDITOR(), shopkeeper, new TradingRecipesUpdater(shopkeeper));
	}

	@Override
	public SKRegularAdminShopkeeper getShopkeeper() {
		return (SKRegularAdminShopkeeper) super.getShopkeeper();
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		if (!super.canOpen(player, silent)) return false;
		return this.getShopkeeper().getType().hasPermission(player);
	}
}
