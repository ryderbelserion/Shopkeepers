package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class SellingPlayerShopEditorHandler extends PlayerShopEditorHandler {

	private static class TradingRecipesAdapter extends DefaultTradingRecipesAdapter<PriceOffer> {

		private final SKSellingPlayerShopkeeper shopkeeper;

		private TradingRecipesAdapter(SKSellingPlayerShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<TradingRecipeDraft> getTradingRecipes() {
			// Add the shopkeeper's offers:
			List<? extends PriceOffer> offers = shopkeeper.getOffers();
			List<TradingRecipeDraft> recipes = new ArrayList<>(offers.size() + 8); // Heuristic initial capacity
			offers.forEach(offer -> {
				ItemStack tradedItem = offer.getItem().asItemStack();
				TradingRecipeDraft recipe = createTradingRecipeDraft(tradedItem, offer.getPrice());
				recipes.add(recipe);
			});

			// Add new empty recipe drafts for items from the container without existing offer:
			// We only add one recipe per similar item:
			List<ItemStack> newRecipes = new ArrayList<>();
			ItemStack[] containerContents = shopkeeper.getContainerContents(); // Empty if the container is not found
			for (ItemStack containerItem : containerContents) {
				if (ItemUtils.isEmpty(containerItem)) continue; // Ignore empty ItemStacks

				// Replace placeholder item, if this is one:
				containerItem = PlaceholderItems.replace(containerItem);

				if (Settings.isAnyCurrencyItem(containerItem)) continue; // Ignore currency items

				if (shopkeeper.getOffer(containerItem) != null) {
					// There is already a recipe for this item:
					continue;
				}

				if (InventoryUtils.contains(newRecipes, containerItem)) {
					// We already added a new recipe for this item:
					continue;
				}

				// Add new empty recipe:
				containerItem = ItemUtils.copySingleItem(containerItem); // Ensures a stack size of 1
				TradingRecipeDraft recipe = createTradingRecipeDraft(containerItem, 0);
				recipes.add(recipe);
				newRecipes.add(containerItem);
			}

			return recipes;
		}

		@Override
		protected List<? extends PriceOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected void setOffers(List<PriceOffer> newOffers) {
			shopkeeper.setOffers(newOffers);
		}

		@Override
		protected PriceOffer createOffer(TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			int price = getPrice(recipe);
			if (price <= 0) return null; // Invalid recipe

			// We can reuse the trading recipe draft's items without copying them first.
			UnmodifiableItemStack resultItem = recipe.getResultItem();
			// Replace placeholder item, if this is one:
			// Note: We also replace placeholder items in selling shopkeepers, because this allows the setup of trades
			// before the player has all of the required items.
			resultItem = PlaceholderItems.replace(resultItem);

			return PriceOffer.create(resultItem, price);
		}
	}

	protected SellingPlayerShopEditorHandler(SKSellingPlayerShopkeeper shopkeeper) {
		super(shopkeeper, new TradingRecipesAdapter(shopkeeper));
	}

	@Override
	public SKSellingPlayerShopkeeper getShopkeeper() {
		return (SKSellingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected void handleTradesClick(Session session, InventoryClickEvent event) {
		assert this.isTradesArea(event.getRawSlot());
		int rawSlot = event.getRawSlot();
		if (this.isResultRow(rawSlot)) {
			// Handle changing sell stack size:
			this.handleUpdateItemAmountOnClick(event, 1);
		} else {
			super.handleTradesClick(session, event);
		}
	}
}
