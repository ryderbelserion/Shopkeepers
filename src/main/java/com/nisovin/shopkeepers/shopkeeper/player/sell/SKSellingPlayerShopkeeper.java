package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.sell.SellingPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.SKPriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public class SKSellingPlayerShopkeeper extends AbstractPlayerShopkeeper implements SellingPlayerShopkeeper {

	private static final Predicate<ItemStack> ITEM_FILTER = (ItemStack item) -> {
		if (Settings.isCurrencyItem(item) || Settings.isHighCurrencyItem(item)) return false;
		return true;
	};

	// Contains only one offer for a specific type of item:
	private final List<PriceOffer> offers = new ArrayList<>();
	private final List<PriceOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link SKSellingPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected SKSellingPlayerShopkeeper(int id) {
		super(id);
	}

	protected SKSellingPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected SKSellingPlayerShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(configSection);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new SellingPlayerShopEditorHandler(this));
		}
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new SellingPlayerShopTradingHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		// Load offers:
		List<SKPriceOffer> offers = SKPriceOffer.loadFromConfig(configSection, "offers", "Shopkeeper " + this.getId());
		List<SKPriceOffer> migratedOffers = SKPriceOffer.migrateItems(offers, "Shopkeeper " + this.getId());
		if (offers != migratedOffers) {
			Log.debug(DebugOptions.itemMigrations,
					() -> "Shopkeeper " + this.getId() + ": Migrated trading offer items."
			);
			this.markDirty();
		}
		this._setOffers(migratedOffers);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// Save offers:
		SKPriceOffer.saveToConfig(configSection, "offers", this.getOffers());
	}

	@Override
	public SellingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_SELLING();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		List<TradingRecipe> recipes = new ArrayList<>();
		List<ItemCount> containerItems = this.getItemsFromContainer();
		for (PriceOffer offer : this.getOffers()) {
			ItemStack tradedItem = offer.getItem();
			int itemAmountInContainer = 0;
			ItemCount itemCount = ItemCount.findSimilar(containerItems, tradedItem);
			if (itemCount != null) {
				itemAmountInContainer = itemCount.getAmount();
			}
			boolean outOfStock = (itemAmountInContainer < tradedItem.getAmount());
			TradingRecipe recipe = this.createSellingRecipe(tradedItem, offer.getPrice(), outOfStock);
			if (recipe != null) {
				recipes.add(recipe);
			}
		}
		return Collections.unmodifiableList(recipes);
	}

	protected List<ItemCount> getItemsFromContainer() {
		return this.getItemsFromContainer(ITEM_FILTER);
	}

	// OFFERS:

	@Override
	public List<PriceOffer> getOffers() {
		return offersView;
	}

	@Override
	public PriceOffer getOffer(ItemStack tradedItem) {
		for (PriceOffer offer : this.getOffers()) {
			if (ItemUtils.isSimilar(offer.getItem(), tradedItem)) {
				return offer;
			}
		}
		return null;
	}

	@Override
	public void removeOffer(ItemStack tradedItem) {
		Iterator<PriceOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (ItemUtils.isSimilar(iterator.next().getItem(), tradedItem)) {
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
	public void setOffers(List<PriceOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(List<? extends PriceOffer> offers) {
		assert offers != null && !offers.contains(null);
		this._clearOffers();
		this._addOffers(offers);
	}

	@Override
	public void addOffer(PriceOffer offer) {
		Validate.notNull(offer, "Offer is null!");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(PriceOffer offer) {
		assert offer != null;
		// Remove previous offer for the same item:
		this.removeOffer(offer.getItem());
		offers.add(offer);
	}

	@Override
	public void addOffers(List<PriceOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(List<? extends PriceOffer> offers) {
		assert offers != null && !offers.contains(null);
		for (PriceOffer offer : offers) {
			assert offer != null;
			// Add new offer: This replaces any previous offer for the same item.
			this._addOffer(offer);
		}
	}
}
