package com.nisovin.shopkeepers.shopkeeper.player.sell;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.PriceOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;

public class SellingPlayerShopkeeper extends AbstractPlayerShopkeeper {

	protected static class SellingPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected class EditorSetup extends CommonEditorSetup<SellingPlayerShopkeeper, PriceOffer> {

			public EditorSetup(SellingPlayerShopkeeper shopkeeper) {
				super(shopkeeper);
			}

			@Override
			protected List<PriceOffer> getOffers() {
				return shopkeeper.getOffers();
			}

			@Override
			protected List<ItemCount> getItemsFromChest() {
				return shopkeeper.getItemsFromChest();
			}

			@Override
			protected boolean hasOffer(ItemStack itemFromChest) {
				return (shopkeeper.getOffer(itemFromChest) != null);
			}

			@Override
			protected void setupColumnForOffer(Inventory inventory, int column, PriceOffer offer) {
				ItemStack tradedItem = offer.getItem();
				inventory.setItem(column, tradedItem);
				setEditColumnCost(inventory, column, offer.getPrice());
			}

			@Override
			protected void setupColumnForItem(Inventory inventory, int column, ItemStack itemFromChest) {
				inventory.setItem(column, itemFromChest);
				setEditColumnCost(inventory, column, 0);
			}
		}

		protected final EditorSetup setup;

		protected SellingPlayerShopEditorHandler(SellingPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
			this.setup = new EditorSetup(shopkeeper);
		}

		@Override
		public SellingPlayerShopkeeper getShopkeeper() {
			return (SellingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			return setup.openWindow(player);
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			event.setCancelled(true);
			if (event.getRawSlot() >= 0 && event.getRawSlot() < TRADE_COLUMNS) {
				// handle changing sell stack size:
				this.handleUpdateItemAmountOnClick(event, 1);
			} else {
				super.onInventoryClick(event, player);
			}
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			SellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			shopkeeper.clearOffers();
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				ItemStack tradedItem = inventory.getItem(column);
				if (ItemUtils.isEmpty(tradedItem)) continue; // not valid recipe column

				int price = this.getPriceFromColumn(inventory, column);
				if (price <= 0) continue;

				// add offer:
				shopkeeper.addOffer(tradedItem, price);
			}
		}
	}

	protected static class SellingPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected SellingPlayerShopTradingHandler(SellingPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		public SellingPlayerShopkeeper getShopkeeper() {
			return (SellingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean prepareTrade(TradeData tradeData) {
			if (!super.prepareTrade(tradeData)) return false;
			SellingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Player tradingPlayer = tradeData.tradingPlayer;
			TradingRecipe tradingRecipe = tradeData.tradingRecipe;

			// get offer for this type of item:
			ItemStack soldItem = tradingRecipe.getResultItem();
			PriceOffer offer = shopkeeper.getOffer(soldItem);
			if (offer == null) {
				// this should not happen.. because the recipes were created based on the shopkeeper's offers
				this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
				return false;
			}

			// validate the found offer:
			int expectedSoldItemAmount = offer.getItem().getAmount();
			if (expectedSoldItemAmount != soldItem.getAmount()) {
				// this shouldn't happen .. because the recipe was created based on this offer
				this.debugPreventedTrade(tradingPlayer, "The offer doesn't match the trading recipe!");
				return false;
			}

			assert chestInventory != null & newChestContents != null;

			// remove result items from chest contents:
			if (ItemUtils.removeItems(newChestContents, soldItem) != 0) {
				this.debugPreventedTrade(tradingPlayer, "The shop's chest doesn't contain the required items.");
				return false;
			}

			// add earnings to chest contents:
			// TODO maybe add the actual items the trading player gave, instead of creating new currency items?
			int amountAfterTaxes = this.getAmountAfterTaxes(offer.getPrice());
			if (amountAfterTaxes > 0) {
				// TODO always store the currency in the most compressed form possible, regardless of
				// 'highCurrencyMinCost'?
				int remaining = amountAfterTaxes;
				if (Settings.isHighCurrencyEnabled() || remaining > Settings.highCurrencyMinCost) {
					int highCurrencyAmount = (remaining / Settings.highCurrencyValue);
					if (highCurrencyAmount > 0) {
						int remainingHighCurrency = ItemUtils.addItems(newChestContents, Settings.createHighCurrencyItem(highCurrencyAmount));
						remaining -= ((highCurrencyAmount - remainingHighCurrency) * Settings.highCurrencyValue);
					}
				}
				if (remaining > 0) {
					if (ItemUtils.addItems(newChestContents, Settings.createCurrencyItem(remaining)) != 0) {
						this.debugPreventedTrade(tradingPlayer, "The shop's chest cannot hold the traded items.");
						return false;
					}
				}
			}
			return true;
		}
	}

	private static final Filter<ItemStack> ITEM_FILTER = new Filter<ItemStack>() {

		@Override
		public boolean accept(ItemStack item) {
			if (Settings.isCurrencyItem(item) || Settings.isHighCurrencyItem(item)) return false;
			return true;
		}
	};

	// contains only one offer for a specific type of item:
	private final List<PriceOffer> offers = new ArrayList<>();
	private final List<PriceOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link SellingPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected SellingPlayerShopkeeper(int id) {
		super(id);
	}

	protected SellingPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected SellingPlayerShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
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
		// load offers:
		this._clearOffers();
		this._addOffers(PriceOffer.loadFromConfig(configSection, "offers"));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// save offers:
		PriceOffer.saveToConfig(configSection, "offers", this.getOffers());
	}

	@Override
	public SellingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_SELLING();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		List<TradingRecipe> recipes = new ArrayList<>();
		List<ItemCount> chestItems = this.getItemsFromChest();
		for (PriceOffer offer : this.getOffers()) {
			ItemStack tradedItem = offer.getItem();
			int itemAmountInChest = 0;
			ItemCount itemCount = ItemCount.findSimilar(chestItems, tradedItem);
			if (itemCount != null) {
				itemAmountInChest = itemCount.getAmount();
			}
			boolean outOfStock = (itemAmountInChest < tradedItem.getAmount());
			TradingRecipe recipe = this.createSellingRecipe(tradedItem, offer.getPrice(), outOfStock);
			if (recipe != null) {
				recipes.add(recipe);
			}
		}
		return Collections.unmodifiableList(recipes);
	}

	private List<ItemCount> getItemsFromChest() {
		return this.getItemsFromChest(ITEM_FILTER);
	}

	// OFFERS:

	public List<PriceOffer> getOffers() {
		return offersView;
	}

	public PriceOffer getOffer(ItemStack tradedItem) {
		for (PriceOffer offer : this.getOffers()) {
			if (ItemUtils.isSimilar(offer.getItem(), tradedItem)) {
				return offer;
			}
		}
		return null;
	}

	public PriceOffer addOffer(ItemStack tradedItem, int price) {
		// create offer (also handles validation):
		PriceOffer newOffer = new PriceOffer(tradedItem, price);

		// add new offer (replacing any previous offer for the same item):
		this._addOffer(newOffer);
		this.markDirty();
		return newOffer;
	}

	private void _addOffer(PriceOffer offer) {
		assert offer != null;
		// remove previous offer for the same item:
		this.removeOffer(offer.getItem());
		offers.add(offer);
	}

	private void _addOffers(Collection<PriceOffer> offers) {
		assert offers != null;
		for (PriceOffer offer : offers) {
			if (offer == null) continue; // skip invalid entries
			// add new offer (replacing any previous offer for the same item):
			this._addOffer(offer);
		}
	}

	private void _clearOffers() {
		offers.clear();
	}

	public void clearOffers() {
		this._clearOffers();
		this.markDirty();
	}

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
}
