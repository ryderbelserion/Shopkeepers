package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.TradeOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.trade.TradingPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.SKTradeOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class SKTradingPlayerShopkeeper extends AbstractPlayerShopkeeper implements TradingPlayerShopkeeper {

	// There can be multiple different offers for the same kind of item:
	private final List<TradeOffer> offers = new ArrayList<>();
	private final List<? extends TradeOffer> offersView = Collections.unmodifiableList(offers);

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

	protected SKTradingPlayerShopkeeper(int id, ConfigurationSection shopkeeperData) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(shopkeeperData);
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
	protected void loadFromSaveData(ConfigurationSection shopkeeperData) throws ShopkeeperCreateException {
		super.loadFromSaveData(shopkeeperData);
		// Load offers:
		List<? extends TradeOffer> offers = SKTradeOffer.loadFromConfig(shopkeeperData, "offers", this.getLogPrefix());
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
		SKTradeOffer.saveToConfig(shopkeeperData, "offers", this.getOffers());
	}

	@Override
	public TradingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_TRADING();
	}

	@Override
	public boolean hasTradingRecipes(Player player) {
		return !this.getOffers().isEmpty();
	}

	@Override
	public List<? extends TradingRecipe> getTradingRecipes(Player player) {
		ItemStack[] containerContents = this.getContainerContents(); // Empty if the container is not found
		List<? extends TradeOffer> offers = this.getOffers();
		List<TradingRecipe> recipes = new ArrayList<>(offers.size());
		offers.forEach(offer -> {
			UnmodifiableItemStack resultItem = offer.getResultItem();
			boolean outOfStock = !InventoryUtils.containsAtLeast(containerContents, resultItem, resultItem.getAmount());
			TradingRecipe recipe = SKTradeOffer.toTradingRecipe(offer, outOfStock);
			recipes.add(recipe);
		});
		return Collections.unmodifiableList(recipes);
	}

	// OFFERS:

	@Override
	public List<? extends TradeOffer> getOffers() {
		return offersView;
	}

	public boolean hasOffer(ItemStack resultItem) {
		Validate.notNull(resultItem, "resultItem is null");
		for (TradeOffer offer : this.getOffers()) {
			if (offer.getResultItem().isSimilar(resultItem)) {
				return true;
			}
		}
		return false;
	}

	public TradeOffer getOffer(TradingRecipe tradingRecipe) {
		for (TradeOffer offer : this.getOffers()) {
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
