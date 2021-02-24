package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.BookItems;
import com.nisovin.shopkeepers.util.ItemUtils;

public class BookPlayerShopEditorHandler extends PlayerShopEditorHandler {

	protected BookPlayerShopEditorHandler(SKBookPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKBookPlayerShopkeeper getShopkeeper() {
		return (SKBookPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected List<TradingRecipeDraft> getTradingRecipes() {
		SKBookPlayerShopkeeper shopkeeper = this.getShopkeeper();

		// We only add one recipe per book title:
		Set<String> bookTitles = new HashSet<>();

		// Add the shopkeeper's offers:
		List<BookOffer> offers = shopkeeper.getOffers();
		Map<String, ItemStack> containerBooksByTitle = shopkeeper.getCopyableBooksFromContainer();
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
			TradingRecipeDraft recipe = this.createTradingRecipeDraft(bookItem, bookOffer.getPrice());
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
			TradingRecipeDraft recipe = this.createTradingRecipeDraft(bookItem, 0);
			recipes.add(recipe);
		});

		return recipes;
	}

	@Override
	protected void clearRecipes() {
		SKBookPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.clearOffers();
	}

	@Override
	protected void addRecipe(TradingRecipeDraft recipe) {
		assert recipe != null && recipe.isValid();
		ItemStack bookItem = recipe.getResultItem();
		BookMeta bookMeta = BookItems.getBookMeta(bookItem);
		if (bookMeta == null) return; // Not a written book (unexpected)
		if (!SKBookPlayerShopkeeper.isDummyBook(bookMeta) && !BookItems.isCopyable(bookMeta)) return;

		// Note: The dummy books provide the original book title as well.
		String bookTitle = BookItems.getTitle(bookMeta);
		if (bookTitle == null) return;

		int price = this.getPrice(recipe);
		if (price <= 0) return;

		SKBookPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.addOffer(ShopkeepersAPI.createBookOffer(bookTitle, price));
	}
}
