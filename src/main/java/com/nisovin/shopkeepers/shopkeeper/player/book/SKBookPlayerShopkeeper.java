package com.nisovin.shopkeepers.shopkeeper.player.book;

import java.util.ArrayList;
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
import com.nisovin.shopkeepers.api.shopkeeper.offers.BookOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.book.BookPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.SKBookOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public class SKBookPlayerShopkeeper extends AbstractPlayerShopkeeper implements BookPlayerShopkeeper {

	private static final Filter<ItemStack> ITEM_FILTER = (ItemStack item) -> {
		return isCopyableBook(item) && (getBookTitle(item) != null);
	};

	// contains only one offer for a specific book (book title):
	private final List<BookOffer> offers = new ArrayList<>();
	private final List<BookOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link SKBookPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected SKBookPlayerShopkeeper(int id) {
		super(id);
	}

	protected SKBookPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected SKBookPlayerShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
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
		// TODO remove legacy: load offers from old format (bookTitle -> price mapping) (since late MC 1.14.4)
		List<SKBookOffer> legacyOffers = SKBookOffer.loadFromLegacyConfig(configSection, "offers", "Shopkeeper " + this.getId());
		if (!legacyOffers.isEmpty()) {
			Log.info("Shopkeeper " + this.getId() + ": Importing old book offers.");
			this._addOffers(legacyOffers);
			this.markDirty();
		}
		this._addOffers(SKBookOffer.loadFromConfig(configSection, "offers", "Shopkeeper " + this.getId()));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// save offers:
		SKBookOffer.saveToConfig(configSection, "offers", this.getOffers());
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
			if (Objects.equals(getBookTitle(item), title)) {
				return item;
			}
		}
		return null;
	}

	protected static BookMeta getBookMeta(ItemStack item) {
		if (ItemUtils.isEmpty(item)) return null;
		if (item.getType() != Material.WRITTEN_BOOK) return null;
		return (BookMeta) item.getItemMeta(); // can be null
	}

	protected static Generation getBookGeneration(ItemStack item) {
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		if (!meta.hasGeneration()) {
			// if the generation is missing, minecraft treats the book as an original and so do we:
			return Generation.ORIGINAL;
		}
		return meta.getGeneration(); // assert: not null
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

	protected static String getBookTitle(ItemStack item) {
		BookMeta meta = getBookMeta(item);
		if (meta == null) return null;
		if (!meta.hasTitle()) return null;
		String title = meta.getTitle(); // not null, but can be empty!
		// we ignore books with empty titles for now:
		// TODO support them? they can't be created in vanilla
		if (title.isEmpty()) return null;
		return title;
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
		Generation oldGeneration = getBookGeneration(book);
		assert oldGeneration != null;
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

	@Override
	public List<BookOffer> getOffers() {
		return offersView;
	}

	@Override
	public BookOffer getOffer(ItemStack bookItem) {
		String bookTitle = getBookTitle(bookItem);
		return this.getOffer(bookTitle);
	}

	@Override
	public BookOffer getOffer(String bookTitle) {
		for (BookOffer offer : this.getOffers()) {
			if (offer.getBookTitle().equals(bookTitle)) {
				return offer;
			}
		}
		return null;
	}

	@Override
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

	@Override
	public void clearOffers() {
		this._clearOffers();
		this.markDirty();
	}

	private void _clearOffers() {
		offers.clear();
	}

	@Override
	public void setOffers(List<BookOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(List<? extends BookOffer> offers) {
		assert offers != null && !offers.contains(null);
		this._clearOffers();
		this._addOffers(offers);
	}

	@Override
	public void addOffer(BookOffer offer) {
		Validate.notNull(offer, "Offer is null!");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(BookOffer offer) {
		assert offer != null;
		// remove previous offer for the same book:
		this.removeOffer(offer.getBookTitle());
		offers.add(offer);
	}

	@Override
	public void addOffers(List<BookOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(List<? extends BookOffer> offers) {
		assert offers != null && !offers.contains(null);
		for (BookOffer offer : offers) {
			assert offer != null;
			// add new offer; replaces any previous offer for the same book:
			this._addOffer(offer);
		}
	}
}
