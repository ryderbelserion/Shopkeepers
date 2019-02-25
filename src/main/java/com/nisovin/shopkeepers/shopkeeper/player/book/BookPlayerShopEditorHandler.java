package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.ItemCount;

public class BookPlayerShopEditorHandler extends PlayerShopEditorHandler {

	protected class EditorSetup extends CommonEditorSetup<BookPlayerShopkeeper, BookOffer> {

		private List<ItemCount> copyableBookItems = null; // gets set right before setup and reset afterwards

		public EditorSetup(BookPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		protected List<BookOffer> getOffers() {
			return shopkeeper.getOffers();
		}

		@Override
		protected List<ItemCount> getItemsFromChest() {
			return shopkeeper.getCopyableBooksFromChest();
		}

		@Override
		protected boolean hasOffer(ItemStack itemFromChest) {
			return (shopkeeper.getOffer(itemFromChest) != null);
		}

		@Override
		protected TradingRecipeDraft toTradingRecipe(BookOffer offer) {
			assert offer != null;
			String bookTitle = offer.getBookTitle();
			ItemStack bookItem = shopkeeper.getBookItem(copyableBookItems, bookTitle);
			if (bookItem == null) {
				bookItem = shopkeeper.createDummyBook(bookTitle);
			}
			return createTradingRecipeDraft(bookItem, offer.getPrice());
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
			ItemStack bookItem = recipe.getResultItem();
			if (!BookPlayerShopkeeper.isCopyableOrDummyBook(bookItem)) return;

			String bookTitle = BookPlayerShopkeeper.getTitleOfBook(bookItem);
			if (bookTitle == null) return;

			int price = getPrice(recipe);
			if (price <= 0) return;
			shopkeeper.addOffer(bookTitle, price);
		}
	}

	protected final EditorSetup setup;

	protected BookPlayerShopEditorHandler(BookPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
		this.setup = new EditorSetup(shopkeeper);
	}

	@Override
	public BookPlayerShopkeeper getShopkeeper() {
		return (BookPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected boolean openWindow(Player player) {
		setup.copyableBookItems = this.getShopkeeper().getCopyableBooksFromChest();
		boolean result = setup.openWindow(player);
		setup.copyableBookItems = null;
		return result;
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		super.onInventoryClick(event, player);
	}

	@Override
	protected void saveEditor(Inventory inventory, Player player) {
		setup.saveEditor(inventory, player);
	}
}
