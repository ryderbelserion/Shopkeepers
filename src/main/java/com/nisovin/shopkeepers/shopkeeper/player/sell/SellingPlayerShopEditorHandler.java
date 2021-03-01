package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.offers.SKPriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class SellingPlayerShopEditorHandler extends PlayerShopEditorHandler {

	protected SellingPlayerShopEditorHandler(SKSellingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKSellingPlayerShopkeeper getShopkeeper() {
		return (SKSellingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected List<TradingRecipeDraft> getTradingRecipes() {
		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeper();

		// Add the shopkeeper's offers:
		List<SKPriceOffer> offers = shopkeeper.getOffers();
		List<TradingRecipeDraft> recipes = new ArrayList<>(offers.size() + 8); // Heuristic initial capacity
		offers.forEach(offer -> {
			ItemStack tradedItem = offer.getItem(); // Copy
			TradingRecipeDraft recipe = this.createTradingRecipeDraft(tradedItem, offer.getPrice());
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

			if (ItemUtils.contains(newRecipes, containerItem)) {
				// We already added a new recipe for this item:
				continue;
			}

			// Add new empty recipe:
			containerItem = ItemUtils.copySingleItem(containerItem); // Ensures a stack size of 1
			TradingRecipeDraft recipe = this.createTradingRecipeDraft(containerItem, 0);
			recipes.add(recipe);
			newRecipes.add(containerItem);
		}

		return recipes;
	}

	@Override
	protected void clearRecipes() {
		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.clearOffers();
	}

	@Override
	protected void addRecipe(TradingRecipeDraft recipe) {
		assert recipe != null && recipe.isValid();
		int price = this.getPrice(recipe);
		if (price <= 0) return;

		ItemStack resultItem = recipe.getResultItem();
		// Replace placeholder item, if this is one:
		// Note: We also replace placeholder items in selling shopkeepers, because this allows the setup of trades
		// before the player has all of the required items.
		resultItem = PlaceholderItems.replace(resultItem);

		SKSellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.addOffer(ShopkeepersAPI.createPriceOffer(resultItem, price));
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
