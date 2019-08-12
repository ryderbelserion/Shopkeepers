package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.ItemCount;

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
		List<TradingRecipeDraft> recipes = new ArrayList<>();

		// only adding one recipe per book title:
		Set<String> bookTitles = new HashSet<>();

		// add the shopkeeper's offers:
		List<ItemCount> chestItems = shopkeeper.getCopyableBooksFromChest();
		for (BookOffer offer : shopkeeper.getOffers()) {
			String bookTitle = offer.getBookTitle();
			bookTitles.add(bookTitle);
			ItemStack bookItem = shopkeeper.getBookItem(chestItems, bookTitle);
			if (bookItem == null) {
				bookItem = shopkeeper.createDummyBook(bookTitle);
			}
			TradingRecipeDraft recipe = this.createTradingRecipeDraft(bookItem, offer.getPrice());
			recipes.add(recipe);
		}

		// add empty offers for items from the chest:
		for (int chestItemIndex = 0; chestItemIndex < chestItems.size(); chestItemIndex++) {
			ItemCount itemCount = chestItems.get(chestItemIndex);
			ItemStack itemFromChest = itemCount.getItem(); // this item is already a copy with amount 1

			String bookTitle = SKBookPlayerShopkeeper.getTitleOfBook(itemFromChest);
			if (bookTitles.contains(bookTitle)) {
				continue; // already added a recipe for a book with this name
			}
			bookTitles.add(bookTitle);

			// add recipe:
			TradingRecipeDraft recipe = this.createTradingRecipeDraft(itemFromChest, 0);
			recipes.add(recipe);
		}

		return recipes;
	}

	@Override
	protected void clearRecipes() {
		SKBookPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.clearOffers();
	}

	@Override
	protected void addRecipe(Player player, TradingRecipeDraft recipe) {
		assert recipe != null && recipe.isValid();
		ItemStack bookItem = recipe.getResultItem();
		if (!SKBookPlayerShopkeeper.isCopyableOrDummyBook(bookItem)) return;

		String bookTitle = SKBookPlayerShopkeeper.getTitleOfBook(bookItem);
		if (bookTitle == null) return;

		int price = this.getPrice(recipe);
		if (price <= 0) return;

		SKBookPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.addOffer(ShopkeepersAPI.createBookOffer(bookTitle, price));
	}
}
