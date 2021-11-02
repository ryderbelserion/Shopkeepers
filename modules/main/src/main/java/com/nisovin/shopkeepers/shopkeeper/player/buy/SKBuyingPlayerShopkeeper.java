package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.buy.BuyingPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.offers.SKPriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class SKBuyingPlayerShopkeeper extends AbstractPlayerShopkeeper implements BuyingPlayerShopkeeper {

	// Contains only one offer for any specific type of item:
	private final List<PriceOffer> offers = new ArrayList<>();
	private final List<? extends PriceOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link SKBuyingPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 */
	protected SKBuyingPlayerShopkeeper() {
	}

	protected SKBuyingPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		this.initOnCreation(id, shopCreationData);
	}

	protected SKBuyingPlayerShopkeeper(ShopkeeperData shopkeeperData) throws ShopkeeperCreateException {
		this.initOnLoad(shopkeeperData);
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.EDITOR()) == null) {
			this.registerUIHandler(new BuyingPlayerShopEditorHandler(this));
		}
		if (this.getUIHandler(DefaultUITypes.TRADING()) == null) {
			this.registerUIHandler(new BuyingPlayerShopTradingHandler(this));
		}
		super.setup();
	}

	@Override
	public void loadDynamicState(ShopkeeperData shopkeeperData) throws InvalidDataException {
		super.loadDynamicState(shopkeeperData);
		this.loadOffers(shopkeeperData);
	}

	@Override
	public void saveDynamicState(ShopkeeperData shopkeeperData) {
		super.saveDynamicState(shopkeeperData);
		this.saveOffers(shopkeeperData);
	}

	@Override
	public BuyingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_BUYING();
	}

	@Override
	public boolean hasTradingRecipes(Player player) {
		return !this.getOffers().isEmpty();
	}

	@Override
	public List<? extends TradingRecipe> getTradingRecipes(Player player) {
		int currencyInContainer = this.getCurrencyInContainer();
		List<? extends PriceOffer> offers = this.getOffers();
		List<TradingRecipe> recipes = new ArrayList<>(offers.size());
		offers.forEach(offer -> {
			// Both the offer's and the trading recipe's items are immutable. So there is no need to copy the item.
			UnmodifiableItemStack tradedItem = offer.getItem();
			boolean outOfStock = (currencyInContainer < offer.getPrice());
			TradingRecipe recipe = this.createBuyingRecipe(tradedItem, offer.getPrice(), outOfStock);
			if (recipe != null) {
				recipes.add(recipe);
			} // Else: Price is invalid (cannot be represented by currency items).
		});
		return Collections.unmodifiableList(recipes);
	}

	// OFFERS

	private static final String DATA_KEY_OFFERS = "offers";
	public static final Property<List<? extends PriceOffer>> OFFERS = new BasicProperty<List<? extends PriceOffer>>()
			.dataKeyAccessor(DATA_KEY_OFFERS, SKPriceOffer.LIST_SERIALIZER)
			.useDefaultIfMissing()
			.defaultValue(Collections.emptyList())
			.build();

	private void loadOffers(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setOffers(shopkeeperData.get(OFFERS));
	}

	private void saveOffers(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(OFFERS, this.getOffers());
	}

	@Override
	public List<? extends PriceOffer> getOffers() {
		return offersView;
	}

	@Override
	public PriceOffer getOffer(@ReadOnly ItemStack tradedItem) {
		Validate.notNull(tradedItem, "tradedItem is null");
		for (PriceOffer offer : this.getOffers()) {
			if (offer.getItem().isSimilar(tradedItem)) {
				return offer;
			}
		}
		return null;
	}

	@Override
	public PriceOffer getOffer(UnmodifiableItemStack tradedItem) {
		return this.getOffer(ItemUtils.asItemStackOrNull(tradedItem));
	}

	@Override
	public void removeOffer(@ReadOnly ItemStack tradedItem) {
		Validate.notNull(tradedItem, "tradedItem is null");
		Iterator<? extends PriceOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().getItem().isSimilar(tradedItem)) {
				iterator.remove();
				this.markDirty();
				break;
			}
		}
	}

	@Override
	public void removeOffer(UnmodifiableItemStack tradedItem) {
		this.removeOffer(ItemUtils.asItemStackOrNull(tradedItem));
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
	public void setOffers(@ReadOnly List<? extends PriceOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(@ReadOnly List<? extends PriceOffer> offers) {
		assert offers != null && !offers.contains(null);
		this._clearOffers();
		this._addOffers(offers);
	}

	@Override
	public void addOffer(PriceOffer offer) {
		Validate.notNull(offer, "offer is null");
		this._addOffer(offer);
		this.markDirty();
	}

	private void _addOffer(PriceOffer offer) {
		assert offer != null;
		Validate.isTrue(offer instanceof SKPriceOffer, "offer is not of type SKPriceOffer");
		SKPriceOffer skOffer = (SKPriceOffer) offer;

		// Remove any previous offer for the same item:
		this.removeOffer(skOffer.getItem());

		// Add the new offer:
		offers.add(skOffer);
	}

	@Override
	public void addOffers(@ReadOnly List<? extends PriceOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(@ReadOnly List<? extends PriceOffer> offers) {
		assert offers != null && !offers.contains(null);
		// This replaces any previous offers for the same items:
		offers.forEach(this::_addOffer);
	}
}
