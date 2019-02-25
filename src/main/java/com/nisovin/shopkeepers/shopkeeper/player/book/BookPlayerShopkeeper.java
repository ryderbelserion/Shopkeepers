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

	private static final Filter<ItemStack> ITEM_FILTER = (ItemStack item) -> {
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

	protected List<ItemCount> getCopyableBooksFromChest() {
		return this.getItemsFromChest(ITEM_FILTER);
	}

	protected ItemStack getBookItem(List<ItemCount> itemCounts, String title) {
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

	protected static BookMeta getBookMeta(ItemStack item) {
		if (ItemUtils.isEmpty(item)) return null;
		if (item.getType() != Material.WRITTEN_BOOK) return null;
		if (!item.hasItemMeta()) return null;

		return (BookMeta) item.getItemMeta();
	}

	protected static Generation getBookGeneration(ItemStack item) {
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		return meta.getGeneration();
	}

	protected static boolean isCopyableBook(ItemStack item) {
		Generation generation = getBookGeneration(item);
		return (generation == Generation.ORIGINAL || generation == Generation.COPY_OF_ORIGINAL);
	}

	protected static boolean isCopyableOrDummyBook(ItemStack item) {
		Generation generation = getBookGeneration(item);
		return (generation == Generation.ORIGINAL || generation == Generation.COPY_OF_ORIGINAL || generation == Generation.TATTERED);
	}

	protected static boolean isValidBookCopy(ItemStack item) {
		Generation generation = getBookGeneration(item);
		return (generation == Generation.COPY_OF_ORIGINAL || generation == Generation.COPY_OF_COPY);
	}

	protected static String getTitleOfBook(ItemStack item) {
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		return meta.getTitle();
	}

	protected boolean hasChestBlankBooks() {
		Block chest = this.getChest();
		if (ItemUtils.isChest(chest.getType())) {
			Inventory chestInventory = ((Chest) chest.getState()).getInventory();
			return chestInventory.contains(Material.WRITABLE_BOOK);
		}
		return false;
	}

	protected ItemStack createDummyBook(String title) {
		ItemStack item = new ItemStack(Material.WRITTEN_BOOK, 1);
		BookMeta meta = (BookMeta) item.getItemMeta();
		meta.setTitle(title);
		meta.setAuthor(Settings.msgUnknownBookAuthor);
		meta.setGeneration(Generation.TATTERED);
		item.setItemMeta(meta);
		return item;
	}

	protected ItemStack copyBook(ItemStack book) {
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

	protected static Generation getNextGeneration(Generation generation) {
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
