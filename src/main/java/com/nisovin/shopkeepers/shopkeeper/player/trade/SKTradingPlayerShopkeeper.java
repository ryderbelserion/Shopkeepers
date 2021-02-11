package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.trade.TradingPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.SKTradingOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public class SKTradingPlayerShopkeeper extends AbstractPlayerShopkeeper implements TradingPlayerShopkeeper {

	private final List<TradingOffer> offers = new ArrayList<>();
	private final List<TradingOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link SKTradingPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected SKTradingPlayerShopkeeper(int id) {
		super(id);
	}

	protected SKTradingPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected SKTradingPlayerShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(configSection);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new TradingPlayerShopEditorHandler(this));
		}
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new TradingPlayerShopTradingHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		// Load offers:
		List<SKTradingOffer> offers = SKTradingOffer.loadFromConfig(configSection, "offers", "Shopkeeper " + this.getId());
		List<SKTradingOffer> migratedOffers = SKTradingOffer.migrateItems(offers, "Shopkeeper " + this.getId());
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
		SKTradingOffer.saveToConfig(configSection, "offers", this.getOffers());
	}

	@Override
	public TradingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_TRADING();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		List<TradingRecipe> recipes = new ArrayList<>();
		List<ItemCount> containerItems = this.getItemsFromContainer();
		for (TradingOffer offer : this.getOffers()) {
			ItemStack resultItem = offer.getResultItem();
			assert !ItemUtils.isEmpty(resultItem);
			int itemAmountInContainer = 0;
			ItemCount itemCount = ItemCount.findSimilar(containerItems, resultItem);
			if (itemCount != null) {
				itemAmountInContainer = itemCount.getAmount();
			}
			boolean outOfStock = (itemAmountInContainer < resultItem.getAmount());
			TradingRecipe recipe = ShopkeepersAPI.createTradingRecipe(resultItem, offer.getItem1(), offer.getItem2(), outOfStock);
			if (recipe != null) {
				recipes.add(recipe);
			}
		}
		return Collections.unmodifiableList(recipes);
	}

	protected List<ItemCount> getItemsFromContainer() {
		return this.getItemsFromContainer(null);
	}

	// OFFERS:

	@Override
	public List<TradingOffer> getOffers() {
		return offersView;
	}

	// Note: There might be multiple offers involving this item, this only returns the first one it finds.
	public TradingOffer getOffer(ItemStack tradedItem) {
		for (TradingOffer offer : this.getOffers()) {
			if (ItemUtils.isSimilar(offer.getResultItem(), tradedItem)) {
				return offer;
			}
		}
		return null;
	}

	public TradingOffer getOffer(TradingRecipe tradingRecipe) {
		for (TradingOffer offer : this.getOffers()) {
			if (offer.areItemsEqual(tradingRecipe)) {
				return offer;
			}
		}
		return null;
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
	public void setOffers(List<TradingOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(List<? extends TradingOffer> offers) {
		assert offers != null && !offers.contains(null);
		this._clearOffers();
		this._addOffers(offers);
	}

	@Override
	public void addOffer(TradingOffer offer) {
		Validate.notNull(offer, "Offer is null!");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(TradingOffer offer) {
		assert offer != null;
		offers.add(offer);
	}

	@Override
	public void addOffers(List<TradingOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(List<? extends TradingOffer> offers) {
		assert offers != null && !offers.contains(null);
		offers.forEach(this::_addOffer);
	}
}
