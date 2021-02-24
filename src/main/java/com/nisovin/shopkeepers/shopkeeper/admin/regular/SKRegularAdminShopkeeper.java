package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.admin.regular.RegularAdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.admin.AbstractAdminShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.offers.SKTradeOffer;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;

public class SKRegularAdminShopkeeper extends AbstractAdminShopkeeper implements RegularAdminShopkeeper {

	// There can be multiple different offers for the same kind of item:
	private final List<TradeOffer> offers = new ArrayList<>();
	private final List<TradeOffer> offersView = Collections.unmodifiableList(offers);

	// Kept in sync with offers:
	private final List<TradingRecipe> recipes = new ArrayList<>();
	private final List<TradingRecipe> recipesView = Collections.unmodifiableList(recipes);

	/**
	 * Creates a not yet initialized {@link SKRegularAdminShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected SKRegularAdminShopkeeper(int id) {
		super(id);
	}

	protected SKRegularAdminShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected SKRegularAdminShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(configSection);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new RegularAdminShopEditorHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		// Load offers:
		List<SKTradeOffer> offers = SKTradeOffer.loadFromConfig(configSection, "recipes", "Shopkeeper " + this.getId());
		List<SKTradeOffer> migratedOffers = SKTradeOffer.migrateItems(offers, "Shopkeeper " + this.getId());
		if (offers != migratedOffers) {
			Log.debug(DebugOptions.itemMigrations, () -> "Shopkeeper " + this.getId() + ": Migrated trade offer items.");
			this.markDirty();
		}
		this._setOffers(migratedOffers);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// Save offers:
		SKTradeOffer.saveToConfig(configSection, "recipes", this.getOffers());
	}

	@Override
	public RegularAdminShopType getType() {
		return SKDefaultShopTypes.ADMIN();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		return recipesView;
	}

	// OFFERS:

	@Override
	public List<TradeOffer> getOffers() {
		return offersView;
	}

	@Override
	public void clearOffers() {
		this._clearOffers();
		this.markDirty();
	}

	private void _clearOffers() {
		offers.clear();
		recipes.clear();
	}

	@Override
	public void setOffers(List<TradeOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(List<? extends TradeOffer> offers) {
		assert offers != null && !offers.contains(null);
		this._clearOffers();
		this._addOffers(offers);
	}

	@Override
	public void addOffer(TradeOffer offer) {
		Validate.notNull(offer, "Offer is null!");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(TradeOffer offer) {
		assert offer != null;
		offers.add(offer);
		if (offer instanceof TradingRecipe) {
			// SKTradeOffer extends SKTradingRecipe and reports to not be out-of-stock.
			recipes.add((TradingRecipe) offer);
		} else {
			recipes.add(ShopkeepersAPI.createTradingRecipe(offer.getResultItem(), offer.getItem1(), offer.getItem2(), false));
		}
	}

	@Override
	public void addOffers(List<TradeOffer> offers) {
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(List<? extends TradeOffer> offers) {
		assert offers != null && !offers.contains(null);
		offers.forEach(this::_addOffer);
	}
}
