package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.sell.SellingPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.SKPriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Validate;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;

public class SKSellingPlayerShopkeeper extends AbstractPlayerShopkeeper implements SellingPlayerShopkeeper {

	// Contains only one offer for any specific type of item:
	private final List<PriceOffer> offers = new ArrayList<>();
	private final List<? extends PriceOffer> offersView = Collections.unmodifiableList(offers);

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
		List<? extends PriceOffer> offers = SKPriceOffer.loadFromConfig(configSection, "offers", "Shopkeeper " + this.getId());
		List<? extends PriceOffer> migratedOffers = SKPriceOffer.migrateItems(offers, "Shopkeeper " + this.getId());
		if (offers != migratedOffers) {
			Log.debug(DebugOptions.itemMigrations, () -> "Shopkeeper " + this.getId() + ": Migrated items of trade offers.");
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
	public boolean hasTradingRecipes(Player player) {
		return !this.getOffers().isEmpty();
	}

	@Override
	public List<? extends TradingRecipe> getTradingRecipes(Player player) {
		ItemStack[] containerContents = this.getContainerContents(); // Empty if the container is not found
		List<? extends PriceOffer> offers = this.getOffers();
		List<TradingRecipe> recipes = new ArrayList<>(offers.size());
		offers.forEach(offer -> {
			// Both the offer's and the trading recipe's items are immutable. So there is no need to copy the item.
			UnmodifiableItemStack tradedItem = offer.getItem();
			boolean outOfStock = !ItemUtils.containsAtLeast(containerContents, tradedItem, tradedItem.getAmount());
			TradingRecipe recipe = this.createSellingRecipe(tradedItem, offer.getPrice(), outOfStock);
			if (recipe != null) {
				recipes.add(recipe);
			} // Else: Price is invalid (cannot be represented by currency items).
		});
		return Collections.unmodifiableList(recipes);
	}

	// OFFERS:

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
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
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
		Validate.notNull(offer, "Offer is null!");
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
		Validate.notNull(offers, "Offers is null!");
		Validate.noNullElements(offers, "Offers contains null elements!");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(@ReadOnly List<? extends PriceOffer> offers) {
		assert offers != null && !offers.contains(null);
		// This replaces any previous offers for the same items:
		offers.forEach(this::_addOffer);
	}
}
