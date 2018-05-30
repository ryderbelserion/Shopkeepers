package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.ShopType;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.TradingRecipe;
import com.nisovin.shopkeepers.shoptypes.offers.PriceOffer;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.ui.defaults.DefaultUIs;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;

public class NormalPlayerShopkeeper extends PlayerShopkeeper {

	protected static class NormalPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected NormalPlayerShopEditorHandler(UIType uiType, NormalPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		public NormalPlayerShopkeeper getShopkeeper() {
			return (NormalPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			NormalPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add offers:
			List<ItemCount> chestItems = shopkeeper.getItemsFromChest();
			for (int column = 0; column < chestItems.size() && column < TRADE_COLUMNS; column++) {
				ItemCount itemCount = chestItems.get(column);
				ItemStack item = itemCount.getItem(); // this item is already a copy with amount 1
				int price = 0;
				PriceOffer offer = shopkeeper.getOffer(item);
				if (offer != null) {
					price = offer.getPrice();
					item.setAmount(offer.getItem().getAmount());
				}

				// add offer to editor inventory:
				inventory.setItem(column, item);
				this.setEditColumnCost(inventory, column, price);
			}

			// add the special buttons:
			this.setActionButtons(inventory);
			// show editing inventory:
			player.openInventory(inventory);
			return true;
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
			NormalPlayerShopkeeper shopkeeper = this.getShopkeeper();
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				ItemStack tradedItem = inventory.getItem(column);
				if (!ItemUtils.isEmpty(tradedItem)) {
					int price = this.getPriceFromColumn(inventory, column);
					if (price > 0) {
						// replaces the previous offer for this item:
						shopkeeper.addOffer(tradedItem, price);
					} else {
						shopkeeper.removeOffer(tradedItem);
					}
				}
			}
		}
	}

	protected static class NormalPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected NormalPlayerShopTradingHandler(UIType uiManager, NormalPlayerShopkeeper shopkeeper) {
			super(uiManager, shopkeeper);
		}

		@Override
		public NormalPlayerShopkeeper getShopkeeper() {
			return (NormalPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean prepareTrade(TradeData tradeData) {
			if (!super.prepareTrade(tradeData)) return false;
			NormalPlayerShopkeeper shopkeeper = this.getShopkeeper();
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
	private final List<PriceOffer> offers = new ArrayList<PriceOffer>();
	private final List<PriceOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * For use in extending classes.
	 */
	protected NormalPlayerShopkeeper() {
	}

	public NormalPlayerShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.initOnLoad(config);
		this.onInitDone();
	}

	public NormalPlayerShopkeeper(PlayerShopCreationData creationData) throws ShopkeeperCreateException {
		this.initOnCreation(creationData);
		this.onInitDone();
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new NormalPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new NormalPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
	}

	@Override
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		super.load(config);
		// load offers:
		this.clearOffers();
		// TODO remove legacy: load offers from old costs section
		this.addOffers(PriceOffer.loadFromConfigOld(config, "costs"));
		this.addOffers(PriceOffer.loadFromConfig(config, "offers"));
	}

	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		// save offers:
		PriceOffer.saveToConfig(config, "offers", this.getOffers());
	}

	@Override
	public ShopType<?> getType() {
		return DefaultShopTypes.PLAYER_NORMAL();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		List<TradingRecipe> recipes = new ArrayList<TradingRecipe>();
		List<ItemCount> chestItems = this.getItemsFromChest();
		for (PriceOffer offer : this.getOffers()) {
			ItemStack tradedItem = offer.getItem();
			ItemCount itemCount = ItemCount.findSimilar(chestItems, tradedItem);
			if (itemCount == null) continue;

			int itemAmountInChest = itemCount.getAmount();
			if (itemAmountInChest >= offer.getItem().getAmount()) {
				TradingRecipe recipe = this.createSellingRecipe(tradedItem, offer.getPrice());
				if (recipe != null) {
					recipes.add(recipe);
				}
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
		this.addOffer(newOffer);
		return newOffer;
	}

	private void addOffer(PriceOffer offer) {
		assert offer != null;
		// remove previous offer for the same item:
		this.removeOffer(offer.getItem());
		offers.add(offer);
	}

	private void addOffers(Collection<PriceOffer> offers) {
		assert offers != null;
		for (PriceOffer offer : offers) {
			if (offer == null) continue; // skip invalid entries
			// add new offer (replacing any previous offer for the same item):
			this.addOffer(offer);
		}
	}

	public void clearOffers() {
		offers.clear();
	}

	public void removeOffer(ItemStack tradedItem) {
		Iterator<PriceOffer> iterator = offers.iterator();
		while (iterator.hasNext()) {
			if (ItemUtils.isSimilar(iterator.next().getItem(), tradedItem)) {
				iterator.remove();
				break;
			}
		}
	}
}
