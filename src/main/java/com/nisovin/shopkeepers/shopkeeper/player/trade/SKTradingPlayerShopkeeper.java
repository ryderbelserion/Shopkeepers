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
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.SKTradingOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
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
		// load offers:
		this._setOffers(SKTradingOffer.loadFromConfig(configSection, "offers", "Shopkeeper " + this.getId()));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// save offers:
		SKTradingOffer.saveToConfig(configSection, "offers", this.getOffers());
	}

	@Override
	public TradingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_TRADING();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		List<TradingRecipe> recipes = new ArrayList<>();
		List<ItemCount> chestItems = this.getItemsFromChest();
		for (TradingOffer offer : this.getOffers()) {
			ItemStack resultItem = offer.getResultItem();
			assert !ItemUtils.isEmpty(resultItem);
			int itemAmountInChest = 0;
			ItemCount itemCount = ItemCount.findSimilar(chestItems, resultItem);
			if (itemCount != null) {
				itemAmountInChest = itemCount.getAmount();
			}
			boolean outOfStock = (itemAmountInChest < resultItem.getAmount());
			TradingRecipe recipe = ShopkeepersAPI.createTradingRecipe(resultItem, offer.getItem1(), offer.getItem2(), outOfStock);
			if (recipe != null) {
				recipes.add(recipe);
			}
		}
		return Collections.unmodifiableList(recipes);
	}

	protected List<ItemCount> getItemsFromChest() {
		return this.getItemsFromChest(null);
	}

	// OFFERS:

	@Override
	public List<TradingOffer> getOffers() {
		return offersView;
	}

	// note: there might be multiple offers involving this item, this only returns the first one it finds
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
		for (TradingOffer offer : offers) {
			assert offer != null;
			this._addOffer(offer);
		}
	}
}
