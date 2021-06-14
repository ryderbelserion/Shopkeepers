package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.BookItems;
import com.nisovin.shopkeepers.util.ItemUtils;

public class BookPlayerShopEditorHandler extends PlayerShopEditorHandler {

	private static class TradingRecipesAdapter extends DefaultTradingRecipesAdapter<BookOffer> {

		private final SKBookPlayerShopkeeper shopkeeper;

		private TradingRecipesAdapter(SKBookPlayerShopkeeper shopkeeper) {
			assert shopkeeper != null;
			this.shopkeeper = shopkeeper;
		}

		@Override
		public List<TradingRecipeDraft> getTradingRecipes() {
			// We only add one recipe per book title:
			Set<String> bookTitles = new HashSet<>();

			// Add the shopkeeper's offers:
			Map<String, ItemStack> containerBooksByTitle = shopkeeper.getCopyableBooksFromContainer();
			List<? extends BookOffer> offers = shopkeeper.getOffers();
			List<TradingRecipeDraft> recipes = new ArrayList<>(Math.max(offers.size(), containerBooksByTitle.size()));
			offers.forEach(bookOffer -> {
				String bookTitle = bookOffer.getBookTitle();
				bookTitles.add(bookTitle);
				ItemStack bookItem = containerBooksByTitle.get(bookTitle);
				if (bookItem == null) {
					bookItem = shopkeeper.createDummyBook(bookTitle);
				} else {
					bookItem = ItemUtils.copySingleItem(bookItem); // Also ensures a stack size of 1
				}
				TradingRecipeDraft recipe = createTradingRecipeDraft(bookItem, bookOffer.getPrice());
				recipes.add(recipe);
			});

			// Add new empty recipe drafts for book items from the container without existing offer:
			containerBooksByTitle.entrySet().forEach(bookEntry -> {
				String bookTitle = bookEntry.getKey();
				assert bookTitle != null;
				if (!bookTitles.add(bookTitle)) {
					// We already added a recipe for a book with this title.
					return;
				}

				// Add new empty recipe:
				ItemStack bookItem = ItemUtils.copySingleItem(bookEntry.getValue()); // Ensures a stack size of 1
				TradingRecipeDraft recipe = createTradingRecipeDraft(bookItem, 0);
				recipes.add(recipe);
			});

			return recipes;
		}

		@Override
		protected List<? extends BookOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected void setOffers(List<BookOffer> newOffers) {
			shopkeeper.setOffers(newOffers);
		}

		@Override
		protected BookOffer createOffer(TradingRecipeDraft recipe) {
			assert recipe != null && recipe.isValid();
			// We can reuse the trading recipe draft's items without copying them first.
			UnmodifiableItemStack bookItem = recipe.getResultItem();
			BookMeta bookMeta = BookItems.getBookMeta(bookItem);
			if (bookMeta == null) return null; // Invalid recipe (not a written book, unexpected)
			if (!SKBookPlayerShopkeeper.isDummyBook(bookMeta) && !BookItems.isCopyable(bookMeta)) {
				return null; // Invalid recipe
			}

			// Note: The dummy books provide the original book title as well.
			String bookTitle = BookItems.getTitle(bookMeta);
			if (bookTitle == null) return null; // Invalid recipe

			int price = getPrice(recipe);
			if (price <= 0) return null; // Invalid recipe

			return BookOffer.create(bookTitle, price);
		}
	}

	protected BookPlayerShopEditorHandler(SKBookPlayerShopkeeper shopkeeper) {
		super(shopkeeper, new TradingRecipesAdapter(shopkeeper));
	}

	@Override
	public SKBookPlayerShopkeeper getShopkeeper() {
		return (SKBookPlayerShopkeeper) super.getShopkeeper();
	}
}
