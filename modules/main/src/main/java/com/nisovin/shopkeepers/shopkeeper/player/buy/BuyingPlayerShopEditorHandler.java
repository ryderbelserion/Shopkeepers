package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.currency.Currencies;
import com.nisovin.shopkeepers.currency.Currency;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlaceholderItems;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.ui.editor.DefaultTradingRecipesAdapter;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.logging.Log;

public class BuyingPlayerShopEditorHandler extends PlayerShopEditorHandler {

	private static class TradingRecipesAdapter
			extends DefaultTradingRecipesAdapter<@NonNull PriceOffer> {

		private final SKBuyingPlayerShopkeeper shopkeeper;

		private TradingRecipesAdapter(SKBuyingPlayerShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<@NonNull TradingRecipeDraft> getTradingRecipes() {
			// Add the shopkeeper's offers:
			List<? extends @NonNull PriceOffer> offers = shopkeeper.getOffers();
			// With heuristic initial capacity:
			List<@NonNull TradingRecipeDraft> recipes = new ArrayList<>(offers.size() + 8);
			Currency baseCurrency = Currencies.getBase();
			offers.forEach(offer -> {
				UnmodifiableItemStack tradedItem = offer.getItem();
				UnmodifiableItemStack currencyItem = baseCurrency.getItemData().createUnmodifiableItemStack(offer.getPrice());
				TradingRecipeDraft recipe = new TradingRecipeDraft(currencyItem, tradedItem, null);
				recipes.add(recipe);
			});

			// Add new empty recipe drafts for items from the container without existing offer:
			// We only add one recipe per similar item:
			List<@NonNull ItemStack> newRecipes = new ArrayList<>();
			// Empty if the container is not found:
			@Nullable ItemStack[] containerContents = shopkeeper.getContainerContents();
			for (ItemStack containerItem : containerContents) {
				// Ignore empty ItemStacks:
				if (containerItem == null) continue;
				if (ItemUtils.isEmpty(containerItem)) continue;

				// Replace placeholder item, if this is one:
				containerItem = PlaceholderItems.replaceNonNull(containerItem);

				// Ignore currency items:
				if (Currencies.matchesAny(containerItem)) {
					continue;
				}

				if (shopkeeper.getOffer(containerItem) != null) {
					// There is already a recipe for this item:
					continue;
				}

				if (InventoryUtils.contains(newRecipes, containerItem)) {
					// We already added a new recipe for this item:
					continue;
				}

				// Add a new empty recipe:
				containerItem = ItemUtils.copySingleItem(containerItem);
				TradingRecipeDraft recipe = new TradingRecipeDraft(null, containerItem, null);
				recipes.add(recipe);
				newRecipes.add(containerItem);
			}
			return recipes;
		}

		@Override
		protected List<? extends @NonNull PriceOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected void setOffers(List<? extends @NonNull PriceOffer> newOffers) {
			shopkeeper.setOffers(newOffers);
		}

		@Override
		protected @Nullable PriceOffer createOffer(TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			assert recipe.getItem2() == null; // Cannot be set via the editor

			// We can reuse the trading recipe draft's items without copying them first.
			UnmodifiableItemStack priceItem = Unsafe.assertNonNull(recipe.getResultItem());
			// Make sure that the item is actually currency, just in case:
			if (!Currencies.getBase().getItemData().matches(priceItem)) {
				// Unexpected.
				Log.debug(shopkeeper.getLogPrefix()
						+ "Price item does not match the base currency!");
				return null; // Ignore invalid recipe
			}
			assert priceItem.getAmount() > 0;
			int price = priceItem.getAmount();

			UnmodifiableItemStack tradedItem = Unsafe.assertNonNull(recipe.getItem1());
			// Replace placeholder item, if this is one:
			tradedItem = PlaceholderItems.replaceNonNull(tradedItem);

			return PriceOffer.create(tradedItem, price);
		}
	}

	protected BuyingPlayerShopEditorHandler(SKBuyingPlayerShopkeeper shopkeeper) {
		super(shopkeeper, new TradingRecipesAdapter(shopkeeper));
	}

	@Override
	public SKBuyingPlayerShopkeeper getShopkeeper() {
		return (SKBuyingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected TradingRecipeDraft getEmptyTrade() {
		return DerivedSettings.buyingEmptyTrade;
	}

	@Override
	protected TradingRecipeDraft getEmptyTradeSlotItems() {
		return DerivedSettings.buyingEmptyTradeSlotItems;
	}

	@Override
	protected void handleTradesClick(EditorSession editorSession, InventoryClickEvent event) {
		assert this.isTradesArea(event.getRawSlot());
		Inventory inventory = editorSession.getInventory();
		int rawSlot = event.getRawSlot();
		if (this.isResultRow(rawSlot)) {
			// Modify the cost, if this column contains a trade:
			ItemStack tradedItem = this.getTradeItem1(inventory, this.getTradeColumn(rawSlot));
			if (tradedItem == null) return;

			UnmodifiableItemStack emptySlotItem = this.getEmptyTradeSlotItems().getResultItem();
			this.updateTradeCostItemOnClick(event, Currencies.getBase(), emptySlotItem);
		} else if (this.isItem1Row(rawSlot)) {
			// Modify the bought item quantity, if this column contains a trade:
			UnmodifiableItemStack emptySlotItem = this.getEmptyTradeSlotItems().getItem1();
			this.updateItemAmountOnClick(event, 1, emptySlotItem);
		}
		// Item2 row: Not used by the buying shop.
	}
}
