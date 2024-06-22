package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.api.shopkeeper.player.buy.BuyingPlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.shopkeeper.migration.Migration;
import com.nisovin.shopkeepers.shopkeeper.migration.MigrationPhase;
import com.nisovin.shopkeepers.shopkeeper.migration.ShopkeeperDataMigrator;
import com.nisovin.shopkeepers.shopkeeper.offers.SKPriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.annotations.ReadOnly;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class SKBuyingPlayerShopkeeper
		extends AbstractPlayerShopkeeper implements BuyingPlayerShopkeeper {

	// Contains only one offer for any specific type of item:
	private final List<@NonNull PriceOffer> offers = new ArrayList<>();
	private final List<? extends @NonNull PriceOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a new and not yet initialized {@link SKBuyingPlayerShopkeeper}.
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 */
	protected SKBuyingPlayerShopkeeper() {
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
	public void saveDynamicState(ShopkeeperData shopkeeperData, boolean saveAll) {
		super.saveDynamicState(shopkeeperData, saveAll);
		this.saveOffers(shopkeeperData);
	}

	@Override
	public BuyingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_BUYING();
	}

	@Override
	public boolean hasTradingRecipes(@Nullable Player player) {
		return !this.getOffers().isEmpty();
	}

	@Override
	public List<? extends @NonNull TradingRecipe> getTradingRecipes(@Nullable Player player) {
		int currencyInContainer = this.getCurrencyInContainer();
		List<? extends @NonNull PriceOffer> offers = this.getOffers();
		List<@NonNull TradingRecipe> recipes = new ArrayList<>(offers.size());
		offers.forEach(offer -> {
			// Both the offer's and the trading recipe's items are immutable. So there is no need to
			// copy the item.
			UnmodifiableItemStack tradedItem = offer.getItem();
			boolean outOfStock = (currencyInContainer < offer.getPrice());
			TradingRecipe recipe = this.createBuyingRecipe(
					tradedItem,
					offer.getPrice(),
					outOfStock
			);
			if (recipe != null) {
				recipes.add(recipe);
			} // Else: Price is invalid (cannot be represented by currency items).
		});
		return Collections.unmodifiableList(recipes);
	}

	// OFFERS

	private static final String DATA_KEY_OFFERS = "offers";
	public static final Property<@NonNull List<? extends @NonNull PriceOffer>> OFFERS = new BasicProperty<@NonNull List<? extends @NonNull PriceOffer>>()
			.dataKeyAccessor(DATA_KEY_OFFERS, SKPriceOffer.LIST_SERIALIZER)
			.useDefaultIfMissing()
			.defaultValue(Collections.emptyList())
			.build();

	static {
		// Register shopkeeper data migrations:
		ShopkeeperDataMigrator.registerMigration(new Migration(
				"buying-offers",
				MigrationPhase.ofShopkeeperClass(SKBuyingPlayerShopkeeper.class)
		) {
			@Override
			public boolean migrate(
					ShopkeeperData shopkeeperData,
					String logPrefix
			) throws InvalidDataException {
				return SKPriceOffer.migrateOffers(
						shopkeeperData.getDataValue(DATA_KEY_OFFERS),
						logPrefix
				);
			}
		});
	}

	private void loadOffers(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		this._setOffers(shopkeeperData.get(OFFERS));
	}

	private void saveOffers(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set(OFFERS, this.getOffers());
	}

	@Override
	public List<? extends @NonNull PriceOffer> getOffers() {
		return offersView;
	}

	@Override
	public @Nullable PriceOffer getOffer(@ReadOnly ItemStack tradedItem) {
		Validate.notNull(tradedItem, "tradedItem is null");
		for (PriceOffer offer : this.getOffers()) {
			if (offer.getItem().isSimilar(tradedItem)) {
				return offer;
			}
		}
		return null;
	}

	@Override
	public @Nullable PriceOffer getOffer(UnmodifiableItemStack tradedItem) {
		Validate.notNull(tradedItem, "tradedItem is null");
		return this.getOffer(ItemUtils.asItemStack(tradedItem));
	}

	@Override
	public void removeOffer(@ReadOnly ItemStack tradedItem) {
		Validate.notNull(tradedItem, "tradedItem is null");
		Iterator<? extends @NonNull PriceOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			PriceOffer offer = iterator.next();
			if (offer.getItem().isSimilar(tradedItem)) {
				iterator.remove();
				this.markDirty();
				break;
			}
		}
	}

	@Override
	public void removeOffer(UnmodifiableItemStack tradedItem) {
		Validate.notNull(tradedItem, "tradedItem is null");
		this.removeOffer(ItemUtils.asItemStack(tradedItem));
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
	public void setOffers(@ReadOnly List<? extends @NonNull PriceOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._setOffers(offers);
		this.markDirty();
	}

	private void _setOffers(@ReadOnly List<? extends @NonNull PriceOffer> offers) {
		assert offers != null && !CollectionUtils.containsNull(offers);
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
	public void addOffers(@ReadOnly List<? extends @NonNull PriceOffer> offers) {
		Validate.notNull(offers, "offers is null");
		Validate.noNullElements(offers, "offers contains null");
		this._addOffers(offers);
		this.markDirty();
	}

	private void _addOffers(@ReadOnly List<? extends @NonNull PriceOffer> offers) {
		assert offers != null && !CollectionUtils.containsNull(offers);
		// This replaces any previous offers for the same items:
		offers.forEach(this::_addOffer);
	}
}
