package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.BookMeta.Generation;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;

/**
 * Sells written books.
 */
public class BookPlayerShopkeeper extends AbstractPlayerShopkeeper {

	protected static class BookPlayerShopEditorHandler extends PlayerShopEditorHandler {

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
			protected void setupColumnForOffer(Inventory inventory, int column, BookOffer offer) {
				String bookTitle = offer.getBookTitle();
				ItemStack bookItem = shopkeeper.getBookItem(copyableBookItems, bookTitle);
				if (bookItem == null) {
					bookItem = shopkeeper.createDummyBook(bookTitle);
				}
				int price = offer.getPrice();
				inventory.setItem(column, bookItem);
				setEditColumnCost(inventory, column, price);
			}

			@Override
			protected void setupColumnForItem(Inventory inventory, int column, ItemStack itemFromChest) {
				inventory.setItem(column, itemFromChest);
				setEditColumnCost(inventory, column, 0);
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
			BookPlayerShopkeeper shopkeeper = this.getShopkeeper();
			shopkeeper.clearOffers();
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				ItemStack item = inventory.getItem(column);
				if (!isCopyableOrDummyBook(item)) continue;

				String bookTitle = getTitleOfBook(item);
				if (bookTitle == null) continue;

				int price = this.getPriceFromColumn(inventory, column);
				if (price <= 0) continue;

				// add offer:
				shopkeeper.addOffer(bookTitle, price);
			}
		}
	}

	protected static class BookPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected BookPlayerShopTradingHandler(BookPlayerShopkeeper shopkeeper) {
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
			if (!isValidBookCopy(bookItem)) {
				this.debugPreventedTrade(tradingPlayer, "The traded item is no valid book copy!");
				return false;
			}

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
				if (itemStack.getType() != Material.WRITABLE_BOOK) continue;

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

	private static final Filter<ItemStack> ITEM_FILTER = (item) -> {
		return isCopyableBook(item);
	};

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
			this.registerUIHandler(new BookPlayerShopEditorHandler(this));
		}
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new BookPlayerShopTradingHandler(this));
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
			Log.info("Importing old trading offers for shopkeeper '" + this.getId() + "'.");
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
		boolean hasBlankBooks = this.hasChestBlankBooks();
		List<ItemCount> bookItems = this.getCopyableBooksFromChest();
		for (BookOffer offer : this.getOffers()) {
			String bookTitle = offer.getBookTitle();
			ItemStack bookItem = this.getBookItem(bookItems, bookTitle);
			boolean outOfStock = !hasBlankBooks;
			if (bookItem == null) {
				outOfStock = true;
				bookItem = this.createDummyBook(bookTitle);
			} else {
				// create copy:
				bookItem = this.copyBook(bookItem);
			}

			TradingRecipe recipe = this.createSellingRecipe(bookItem, offer.getPrice(), outOfStock);
			if (recipe != null) {
				recipes.add(recipe);
			}
		}
		return Collections.unmodifiableList(recipes);
	}

	private List<ItemCount> getCopyableBooksFromChest() {
		return this.getItemsFromChest(ITEM_FILTER);
	}

	private ItemStack getBookItem(List<ItemCount> itemCounts, String title) {
		if (itemCounts == null) return null;
		for (ItemCount itemCount : itemCounts) {
			if (itemCount == null) continue;
			ItemStack item = itemCount.getItem(); // note: no additional copy
			if (Objects.equals(getTitleOfBook(item), title)) {
				return item;
			}
		}
		return null;
	}

	private static BookMeta getBookMeta(ItemStack item) {
		if (ItemUtils.isEmpty(item)) return null;
		if (item.getType() != Material.WRITTEN_BOOK) return null;
		if (!item.hasItemMeta()) return null;

		return (BookMeta) item.getItemMeta();
	}

	private static Generation getBookGeneration(ItemStack item) {
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		return meta.getGeneration();
	}

	private static boolean isCopyableBook(ItemStack item) {
		Generation generation = getBookGeneration(item);
		return (generation == Generation.ORIGINAL || generation == Generation.COPY_OF_ORIGINAL);
	}

	private static boolean isCopyableOrDummyBook(ItemStack item) {
		Generation generation = getBookGeneration(item);
		return (generation == Generation.ORIGINAL || generation == Generation.COPY_OF_ORIGINAL || generation == Generation.TATTERED);
	}

	private static boolean isValidBookCopy(ItemStack item) {
		Generation generation = getBookGeneration(item);
		return (generation == Generation.COPY_OF_ORIGINAL || generation == Generation.COPY_OF_COPY);
	}

	private static String getTitleOfBook(ItemStack item) {
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		return meta.getTitle();
	}

	private boolean hasChestBlankBooks() {
		Block chest = this.getChest();
		if (ItemUtils.isChest(chest.getType())) {
			Inventory chestInventory = ((Chest) chest.getState()).getInventory();
			return chestInventory.contains(Material.WRITABLE_BOOK);
		}
		return false;
	}

	private ItemStack createDummyBook(String title) {
		ItemStack item = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta meta = (BookMeta) item.getItemMeta();
		meta.setTitle(title);
		meta.setAuthor(Settings.msgUnknownBookAuthor);
		meta.setGeneration(Generation.TATTERED);
		item.setItemMeta(meta);
		return item;
	}

	private ItemStack copyBook(ItemStack book) {
		assert isCopyableBook(book);
		BookMeta oldMeta = (BookMeta) book.getItemMeta();
		Generation oldGeneration = oldMeta.getGeneration();
		Generation nextGeneration = getNextGeneration(oldGeneration);
		assert nextGeneration != null;

		ItemStack copy = book.clone();
		BookMeta newMeta = (BookMeta) copy.getItemMeta();
		newMeta.setGeneration(nextGeneration);
		copy.setItemMeta(newMeta);
		return copy;
	}

	private static Generation getNextGeneration(Generation generation) {
		assert generation != null;
		switch (generation) {
		case ORIGINAL:
			return Generation.COPY_OF_ORIGINAL;
		case COPY_OF_ORIGINAL:
			return Generation.COPY_OF_COPY;
		default:
			// all other books cannot be copied
			return null;
		}
	}

	// OFFERS:

	public List<BookOffer> getOffers() {
		return offersView;
	}

	public BookOffer getOffer(ItemStack bookItem) {
		String bookTitle = getTitleOfBook(bookItem);
		return this.getOffer(bookTitle);
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
