package com.nisovin.shopkeepers.shopkeeper.admin.regular;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

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
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKRegularAdminShopkeeper extends AbstractAdminShopkeeper implements RegularAdminShopkeeper {

	// There can be multiple different offers for the same kind of item:
	private final List<SKTradeOffer> offers = new ArrayList<>();
	private final List<? extends SKTradeOffer> offersView = Collections.unmodifiableList(offers);

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

	protected SKRegularAdminShopkeeper(int id, ConfigurationSection shopkeeperData) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(shopkeeperData);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new RegularAdminShopEditorHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection shopkeeperData) throws ShopkeeperCreateException {
		super.loadFromSaveData(shopkeeperData);
		// Load offers:
		List<? extends TradeOffer> offers = SKTradeOffer.loadFromConfig(shopkeeperData, "recipes", this.getLogPrefix());
		List<? extends TradeOffer> migratedOffers = SKTradeOffer.migrateItems(offers, this.getLogPrefix());
		if (offers != migratedOffers) {
			Log.debug(DebugOptions.itemMigrations, () -> this.getLogPrefix() + "Migrated items of trade offers.");
			this.markDirty();
		}
		this._setOffers(migratedOffers);
	}

	@Override
	public void save(ConfigurationSection shopkeeperData) {
		super.save(shopkeeperData);
		// Save offers:
		SKTradeOffer.saveToConfig(shopkeeperData, "recipes", this.getOffers());
	}

	@Override
	public RegularAdminShopType getType() {
		return SKDefaultShopTypes.ADMIN_REGULAR();
	}

	@Override
	public boolean hasTradingRecipes(Player player) {
		return !this.getOffers().isEmpty();
	}

	@Override
	public List<? extends TradingRecipe> getTradingRecipes(Player player) {
		// SKTradeOffer extends SKTradingRecipe and reports to not be out-of-stock. Both SKTradeOffer and TradingRecipe
		// are immutable. We can therefore reuse the offers as trading recipes, and don't have to create new trading
		// recipes for them.
		return offersView;
	}

	// OFFERS:

	@Override
	public List<? extends TradeOffer> getOffers() {
		return offersView;
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
	public void setOffers(List<? extends TradeOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
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
		Validate.notNull(offer, "offer is null");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(TradeOffer offer) {
		assert offer != null;
		Validate.isTrue(offer instanceof SKTradeOffer, "offer is not of type SKTradeOffer");
		SKTradeOffer skOffer = (SKTradeOffer) offer;

		// Add the new offer:
		offers.add(skOffer);
	}

	@Override
	public void addOffers(List<? extends TradeOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(List<? extends TradeOffer> offers) {
		assert offers != null && !offers.contains(null);
		offers.forEach(this::_addOffer);
	}
}
