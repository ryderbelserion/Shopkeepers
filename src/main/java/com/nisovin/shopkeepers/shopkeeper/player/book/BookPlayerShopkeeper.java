package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.TradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemUtils;

/**
 * Sells written books.
 */
public class BookPlayerShopkeeper extends AbstractPlayerShopkeeper {

	protected static class WrittenBookPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected WrittenBookPlayerShopEditorHandler(BookPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		public BookPlayerShopkeeper getShopkeeper() {
			return (BookPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			BookPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add offers:
			List<ItemStack> books = shopkeeper.getBooksFromChest();
			for (int column = 0; column < books.size() && column < TRADE_COLUMNS; column++) {
				String bookTitle = getTitleOfBook(books.get(column));
				if (bookTitle == null) {
					// allow another book to take its place:
					column--;
					continue;
				}

				// add offer to editor inventory:
				int price = 0;
				BookOffer offer = shopkeeper.getOffer(bookTitle);
				if (offer != null) {
					price = offer.getPrice();
				}
				inventory.setItem(column, books.get(column));
				this.setEditColumnCost(inventory, column, price);
			}

			// add the special buttons:
			this.setActionButtons(inventory);
			// show editing inventory:
			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			super.onInventoryClick(event, player);
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			BookPlayerShopkeeper shopkeeper = this.getShopkeeper();
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				ItemStack item = inventory.getItem(column);
				if (ItemUtils.isEmpty(item) || item.getType() != Material.WRITTEN_BOOK) {
					continue;
				}
				String bookTitle = getTitleOfBook(item);
				if (bookTitle == null) continue;

				int price = this.getPriceFromColumn(inventory, column);
				if (price > 0) {
					shopkeeper.addOffer(bookTitle, price);
				} else {
					shopkeeper.removeOffer(bookTitle);
				}
			}
		}
	}

	protected static class WrittenBookPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected WrittenBookPlayerShopTradingHandler(BookPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		public BookPlayerShopkeeper getShopkeeper() {
			return (BookPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean prepareTrade(TradeData tradeData) {
			if (!super.prepareTrade(tradeData)) return false;
			BookPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Player tradingPlayer = tradeData.tradingPlayer;
			TradingRecipe tradingRecipe = tradeData.tradingRecipe;

			ItemStack bookItem = tradingRecipe.getResultItem();
			String bookTitle = getTitleOfBook(bookItem);
			if (bookTitle == null) {
				// this should not happen.. because the recipes were created based on the shopkeeper's offers
				this.debugPreventedTrade(tradingPlayer, "Couldn't determine the book title of the traded item!");
				return false;
			}

			// get offer for this type of item:
			BookOffer offer = shopkeeper.getOffer(bookTitle);
			if (offer == null) {
				// this should not happen.. because the recipes were created based on the shopkeeper's offers
				this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
				return false;
			}

			assert chestInventory != null & newChestContents != null;

			// remove blank book from chest contents:
			boolean removed = false;
			for (int slot = 0; slot < newChestContents.length; slot++) {
				ItemStack itemStack = newChestContents[slot];
				if (ItemUtils.isEmpty(itemStack)) continue;
				if (itemStack.getType() != Material.BOOK_AND_QUILL) continue;

				int newAmount = itemStack.getAmount() - 1;
				assert newAmount >= 0;
				if (newAmount == 0) {
					newChestContents[slot] = null;
				} else {
					// copy the item before modifying it:
					itemStack = itemStack.clone();
					newChestContents[slot] = itemStack;
					itemStack.setAmount(newAmount);
				}
				removed = true;
				break;
			}
			if (!removed) {
				this.debugPreventedTrade(tradingPlayer, "The shop's chest doesn't contain any book-and-quill items.");
				return false;
			}

			// add earnings to chest contents:
			int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
			if (amountAfterTaxes > 0) {
				int remaining = amountAfterTaxes;
				if (Settings.isHighCurrencyEnabled() || remaining > Settings.highCurrencyMinCost) {
					int highCurrencyAmount = (remaining / Settings.highCurrencyValue);
					if (highCurrencyAmount > 0) {
						int remainingHighCurrency = ItemUtils.addItems(newChestContents, Settings.createHighCurrencyItem(highCurrencyAmount));
						remaining -= ((highCurrencyAmount - remainingHighCurrency) * Settings.highCurrencyValue);
					}
				}
				if (remaining > 0) {
					if (ItemUtils.addItems(newChestContents, Settings.createCurrencyItem(remaining)) != 0) {
						this.debugPreventedTrade(tradingPlayer, "The shop's chest cannot hold the traded items.");
						return false;
					}
				}
			}
			return true;
		}
	}

	// contains only one offer for a specific book (book title):
	private final List<BookOffer> offers = new ArrayList<>();
	private final List<BookOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link BookPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected BookPlayerShopkeeper(int id) {
		super(id);
	}

	protected BookPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected BookPlayerShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(configSection);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new WrittenBookPlayerShopEditorHandler(this));
		}
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new WrittenBookPlayerShopTradingHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		// load offers:
		this._clearOffers();
		// TODO remove legacy: load offers from old costs section (since late MC 1.12.2)
		List<BookOffer> legacyOffers = BookOffer.loadFromConfig(configSection, "costs");
		if (!legacyOffers.isEmpty()) {
			this._addOffers(legacyOffers);
			this.markDirty();
		}
		this._addOffers(BookOffer.loadFromConfig(configSection, "offers"));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// save offers:
		BookOffer.saveToConfig(configSection, "offers", this.getOffers());
	}

	@Override
	public BookPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_BOOK();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		List<TradingRecipe> recipes = new ArrayList<>();
		if (this.hasChestBlankBooks()) {
			List<ItemStack> bookItems = this.getBooksFromChest();
			for (ItemStack bookItem : bookItems) {
				assert !ItemUtils.isEmpty(bookItem);
				String bookTitle = getTitleOfBook(bookItem); // can be null
				BookOffer offer = this.getOffer(bookTitle);
				if (offer == null) continue;

				TradingRecipe recipe = this.createSellingRecipe(bookItem.clone(), offer.getPrice());
				if (recipe != null) {
					recipes.add(recipe);
				}
			}
		}
		return Collections.unmodifiableList(recipes);
	}

	private List<ItemStack> getBooksFromChest() {
		List<ItemStack> list = new ArrayList<>();
		Block chest = this.getChest();
		if (!ItemUtils.isChest(chest.getType())) return list;
		Inventory chestInventory = ((Chest) chest.getState()).getInventory();
		for (ItemStack item : chestInventory.getContents()) {
			if (ItemUtils.isEmpty(item)) continue;
			if (item.getType() == Material.WRITTEN_BOOK && this.isBookAuthoredByShopOwner(item)) {
				list.add(item);
			}
		}
		return list;
	}

	private boolean hasChestBlankBooks() {
		Block chest = this.getChest();
		if (ItemUtils.isChest(chest.getType())) {
			Inventory chestInventory = ((Chest) chest.getState()).getInventory();
			return chestInventory.contains(Material.BOOK_AND_QUILL);
		}
		return false;
	}

	private boolean isBookAuthoredByShopOwner(ItemStack book) {
		assert book.getType() == Material.WRITTEN_BOOK;
		// checking for ownerName might break if the player changes his name but the book metadata doesn't get updated.
		// Also: why do we even filter for only books of the shop owner?
		/*
		 * if (book.hasItemMeta()) {
		 * BookMeta meta = (BookMeta) book.getItemMeta();
		 * if (meta.hasAuthor() && meta.getAuthor().equalsIgnoreCase(ownerName)) {
		 * return true;
		 * }
		 * }
		 * return false;
		 */
		return book.getType() == Material.WRITTEN_BOOK;
	}

	private static String getTitleOfBook(ItemStack book) {
		if (book.getType() == Material.WRITTEN_BOOK && book.hasItemMeta()) {
			BookMeta meta = (BookMeta) book.getItemMeta();
			return meta.getTitle();
		}
		return null;
	}

	// OFFERS:

	public List<BookOffer> getOffers() {
		return offersView;
	}

	public BookOffer getOffer(String bookTitle) {
		for (BookOffer offer : this.getOffers()) {
			if (offer.getBookTitle().equals(bookTitle)) {
				return offer;
			}
		}
		return null;
	}

	public BookOffer addOffer(String bookTitle, int price) {
		// create offer (also handles validation):
		BookOffer newOffer = new BookOffer(bookTitle, price);

		// add new offer (replacing any previous offer for the same book):
		this._addOffer(newOffer);
		this.markDirty();
		return newOffer;
	}

	private void _addOffer(BookOffer offer) {
		assert offer != null;
		// remove previous offer for the same book:
		this.removeOffer(offer.getBookTitle());
		offers.add(offer);
	}

	private void _addOffers(Collection<BookOffer> offers) {
		assert offers != null;
		for (BookOffer offer : offers) {
			if (offer == null) continue; // skip invalid entries
			// add new offer (replacing any previous offer for the same book):
			this._addOffer(offer);
		}
	}

	private void _clearOffers() {
		offers.clear();
	}

	public void clearOffers() {
		this._clearOffers();
		this.markDirty();
	}

	public void removeOffer(String bookTitle) {
		Iterator<BookOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getBookTitle().equals(bookTitle)) {
				iterator.remove();
				this.markDirty();
				break;
			}
		}
	}
}
