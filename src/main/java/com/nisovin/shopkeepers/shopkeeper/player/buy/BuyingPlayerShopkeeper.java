package com.nisovin.shopkeepers.shopkeeper.player.buy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Material;
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

public class BuyingPlayerShopkeeper extends AbstractPlayerShopkeeper {

	protected static class BuyingPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected static class EditorSetup extends CommonEditorSetup<BuyingPlayerShopkeeper, PriceOffer> {

			public EditorSetup(BuyingPlayerShopkeeper shopkeeper, BuyingPlayerShopEditorHandler editorHandler) {
				super(shopkeeper, editorHandler);
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
				ItemStack currencyItem = Settings.createCurrencyItem(offer.getPrice());

				inventory.setItem(column, currencyItem);
				inventory.setItem(column + 18, tradedItem);
			}

			@Override
			protected void setupColumnForItem(Inventory inventory, int column, ItemStack itemFromChest) {
				ItemStack currencyItem = Settings.createZeroCurrencyItem();
				inventory.setItem(column, currencyItem);
				inventory.setItem(column + 18, itemFromChest);
			}
		}

		protected final EditorSetup setup;

		protected BuyingPlayerShopEditorHandler(BuyingPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
			this.setup = new EditorSetup(shopkeeper, this);
		}

		@Override
		public BuyingPlayerShopkeeper getShopkeeper() {
			return (BuyingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			return setup.openWindow(player);
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			event.setCancelled(true);
			int rawSlot = event.getRawSlot();
			if (rawSlot >= 0 && rawSlot < TRADE_COLUMNS) {
				// modifying cost:
				ItemStack tradedItem = event.getInventory().getItem(rawSlot + 18);
				if (ItemUtils.isEmpty(tradedItem)) return;
				this.handleUpdateTradeCostItemOnClick(event, Settings.createCurrencyItem(1), Settings.createZeroCurrencyItem());
			} else if (rawSlot >= 18 && rawSlot <= 25) {
				// modifying bought item quantity:
				this.handleUpdateItemAmountOnClick(event, 1);
			} else if (rawSlot >= 9 && rawSlot <= 16) {
			} else {
				super.onInventoryClick(event, player);
			}
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			BuyingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			shopkeeper.clearOffers();
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				ItemStack tradedItem = inventory.getItem(column + 18);
				if (ItemUtils.isEmpty(tradedItem)) continue; // not valid recipe column

				ItemStack priceItem = inventory.getItem(column);
				if (ItemUtils.isEmpty(priceItem)) continue;
				if (priceItem.getType() != Settings.currencyItem) continue;

				// add offer:
				shopkeeper.addOffer(tradedItem, priceItem.getAmount());
			}
		}
	}

	protected class BuyingPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected BuyingPlayerShopTradingHandler(BuyingPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		public BuyingPlayerShopkeeper getShopkeeper() {
			return (BuyingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean prepareTrade(TradeData tradeData) {
			if (!super.prepareTrade(tradeData)) return false;
			BuyingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Player tradingPlayer = tradeData.tradingPlayer;
			TradingRecipe tradingRecipe = tradeData.tradingRecipe;

			// get offer for the bought item:
			ItemStack boughtItem = tradingRecipe.getItem1();
			PriceOffer offer = shopkeeper.getOffer(boughtItem);
			if (offer == null) {
				// this should not happen.. because the recipes were created based on the shopkeeper's offers
				this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
				return false;
			}

			// validate the found offer:
			int expectedBoughtItemAmount = offer.getItem().getAmount();
			if (expectedBoughtItemAmount > boughtItem.getAmount()) {
				// this shouldn't happen .. because the recipe was created based on this offer
				this.debugPreventedTrade(tradingPlayer, "The offer doesn't match the trading recipe!");
				return false;
			}

			assert chestInventory != null & newChestContents != null;

			// remove currency items from chest contents:
			int remaining = this.removeCurrency(newChestContents, offer.getPrice());
			if (remaining > 0) {
				this.debugPreventedTrade(tradingPlayer, "The shop's chest doesn't contain enough currency.");
				return false;
			} else if (remaining < 0) {
				this.debugPreventedTrade(tradingPlayer, "The shop's chest does not have enough space to split large currency items.");
				return false;
			}

			// add bought items to chest contents:
			int amountAfterTaxes = this.getAmountAfterTaxes(expectedBoughtItemAmount);
			if (amountAfterTaxes > 0) {
				// the item the trading player gave might slightly differ from the required item,
				// but is still accepted, depending on the used item comparison logic and settings:
				ItemStack receivedItem = tradeData.offeredItem1.clone(); // create a copy, just in case
				receivedItem.setAmount(amountAfterTaxes);
				if (ItemUtils.addItems(newChestContents, receivedItem) != 0) {
					this.debugPreventedTrade(tradingPlayer, "The shop's chest cannot hold the traded items.");
					return false;
				}
			}
			return true;
		}

		// TODO simplify this? Maybe by separating into different, general utility functions
		// TODO support iterating in reverse order, for nicer looking chest contents?
		// returns the amount of currency that couldn't be removed, 0 on full success, negative if too much was removed
		protected int removeCurrency(ItemStack[] contents, int amount) {
			Validate.notNull(contents);
			Validate.isTrue(amount >= 0, "Amount cannot be negative!");
			if (amount == 0) return 0;
			int remaining = amount;

			// first pass: remove as much low currency as available from partial stacks
			// second pass: remove as much low currency as available from full stacks
			for (int k = 0; k < 2; k++) {
				for (int slot = 0; slot < contents.length; slot++) {
					ItemStack itemStack = contents[slot];
					if (!Settings.isCurrencyItem(itemStack)) continue;

					// second pass, or the itemstack is a partial one:
					int itemAmount = itemStack.getAmount();
					if (k == 1 || itemAmount < itemStack.getMaxStackSize()) {
						int newAmount = (itemAmount - remaining);
						if (newAmount > 0) {
							// copy the item before modifying it:
							itemStack = itemStack.clone();
							contents[slot] = itemStack;
							itemStack.setAmount(newAmount);
							remaining = 0;
							break;
						} else {
							contents[slot] = null;
							remaining = -newAmount;
							if (newAmount == 0) {
								break;
							}
						}
					}
				}
				if (remaining == 0) break;
			}
			if (remaining == 0) return 0;

			if (!Settings.isHighCurrencyEnabled()) {
				// we couldn't remove all currency:
				return remaining;
			}

			int remainingHigh = (int) Math.ceil((double) remaining / Settings.highCurrencyValue);
			// we rounded the high currency up, so if this is negative now, it represents the remaining change which
			// needs to be added back:
			remaining -= (remainingHigh * Settings.highCurrencyValue);
			assert remaining <= 0;

			// first pass: remove high currency from partial stacks
			// second pass: remove high currency from full stacks
			for (int k = 0; k < 2; k++) {
				for (int slot = 0; slot < contents.length; slot++) {
					ItemStack itemStack = contents[slot];
					if (!Settings.isHighCurrencyItem(itemStack)) continue;

					// second pass, or the itemstack is a partial one:
					int itemAmount = itemStack.getAmount();
					if (k == 1 || itemAmount < itemStack.getMaxStackSize()) {
						int newAmount = (itemAmount - remainingHigh);
						if (newAmount > 0) {
							// copy the item before modifying it:
							itemStack = itemStack.clone();
							contents[slot] = itemStack;
							itemStack.setAmount(newAmount);
							remainingHigh = 0;
							break;
						} else {
							contents[slot] = null;
							remainingHigh = -newAmount;
							if (newAmount == 0) {
								break;
							}
						}
					}
				}
				if (remainingHigh == 0) break;
			}

			remaining += (remainingHigh * Settings.highCurrencyValue);
			if (remaining >= 0) {
				return remaining;
			}
			assert remaining < 0; // we have some change left
			remaining = -remaining; // the change is now represented as positive value

			// add the remaining change into empty slots (all partial slots have already been cleared above):
			// TODO this could probably be replaced with Utils.addItems
			int maxStackSize = Settings.currencyItem.getMaxStackSize();
			for (int slot = 0; slot < contents.length; slot++) {
				ItemStack itemStack = contents[slot];
				if (!ItemUtils.isEmpty(itemStack)) continue;

				int stackSize = Math.min(remaining, maxStackSize);
				contents[slot] = Settings.createCurrencyItem(stackSize);
				remaining -= stackSize;
				if (remaining == 0) break;
			}
			// we removed too much, represent as negative value:
			remaining = -remaining;
			return remaining;
		}
	}

	private static final Filter<ItemStack> ITEM_FILTER = new Filter<ItemStack>() {

		@Override
		public boolean accept(ItemStack item) {
			if (Settings.isCurrencyItem(item) || Settings.isHighCurrencyItem(item)) return false;
			if (item.getType() == Material.WRITTEN_BOOK) return false;
			if (!item.getEnchantments().isEmpty()) return false; // TODO why don't allow buying of enchanted items?
			return true;
		}
	};

	// contains only one offer for a specific type of item:
	private final List<PriceOffer> offers = new ArrayList<>();
	private final List<PriceOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link BuyingPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected BuyingPlayerShopkeeper(int id) {
		super(id);
	}

	protected BuyingPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected BuyingPlayerShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
		super(id);
		this.initOnLoad(configSection);
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
	public BuyingPlayerShopType getType() {
		return SKDefaultShopTypes.PLAYER_BUYING();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		List<TradingRecipe> recipes = new ArrayList<>();
		int currencyInChest = this.getCurrencyInChest();
		for (PriceOffer offer : this.getOffers()) {
			ItemStack tradedItem = offer.getItem();
			boolean outOfStock = (currencyInChest < offer.getPrice());
			TradingRecipe recipe = this.createBuyingRecipe(tradedItem, offer.getPrice(), outOfStock);
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
