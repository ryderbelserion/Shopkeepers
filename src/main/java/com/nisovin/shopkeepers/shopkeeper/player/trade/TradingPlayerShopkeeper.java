package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKDefaultShopTypes;
import com.nisovin.shopkeepers.shopkeeper.offers.TradingOffer;
import com.nisovin.shopkeepers.shopkeeper.player.AbstractPlayerShopkeeper;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;

public class TradingPlayerShopkeeper extends AbstractPlayerShopkeeper {

	protected static class TradingPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected TradingPlayerShopEditorHandler(TradingPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		public TradingPlayerShopkeeper getShopkeeper() {
			return (TradingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			TradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add the shopkeeper's offers:
			List<TradingOffer> offers = shopkeeper.getOffers();
			for (int column = 0; column < offers.size() && column < TRADE_COLUMNS; column++) {
				TradingOffer offer = offers.get(column);
				inventory.setItem(column, offer.getResultItem());
				inventory.setItem(column + 9, offer.getItem1());
				inventory.setItem(column + 18, offer.getItem2()); // can be null
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
			int slot = event.getRawSlot();
			boolean topRow = (slot >= 0 && slot < TRADE_COLUMNS);
			if (topRow || (slot >= 9 && slot <= 16) || (slot >= 18 && slot <= 25)) {
				ItemStack cursor = event.getCursor();
				if (!ItemUtils.isEmpty(cursor)) {
					// place item from cursor:
					Inventory inventory = event.getInventory();
					ItemStack cursorClone = cursor.clone();
					cursorClone.setAmount(1);
					Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
						inventory.setItem(slot, cursorClone);
					});
				} else {
					// changing stack size of clicked item:
					this.handleUpdateItemAmountOnClick(event, topRow ? 1 : 0);
				}
			} else if (slot >= 27) {
				// clicking in player inventory:
				if (event.isShiftClick()) {
					return;
				}
				ItemStack cursor = event.getCursor();
				ItemStack current = event.getCurrentItem();
				if (!ItemUtils.isEmpty(cursor)) {
					if (ItemUtils.isEmpty(current)) {
						// place item from cursor:
						event.setCurrentItem(cursor);
						event.setCursor(null);
					}
				} else if (!ItemUtils.isEmpty(current)) {
					// pick up item to cursor:
					event.setCurrentItem(null);
					event.setCursor(current);
				}
			} else {
				super.onInventoryClick(event, player);
			}
		}

		@Override
		protected void onInventoryDrag(InventoryDragEvent event, Player player) {
			event.setCancelled(true);
			ItemStack cursor = event.getOldCursor();
			// assert: cursor item is already a clone
			if (ItemUtils.isEmpty(cursor)) return;

			Set<Integer> slots = event.getRawSlots();
			if (slots.size() != 1) return;

			int slot = slots.iterator().next();
			if ((slot >= 0 && slot < TRADE_COLUMNS) || (slot >= 9 && slot <= 16) || (slot >= 18 && slot <= 25)) {
				// place item from cursor:
				Inventory inventory = event.getInventory();
				cursor.setAmount(1);
				Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
					inventory.setItem(slot, cursor);
				});
			} else if (slot >= 27) {
				// clicking in player inventory:
				InventoryView view = event.getView();
				// the cancelled drag event resets the cursor afterwards, so we need this delay:
				Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
					// freshly get and check cursor to make sure that players don't abuse this delay:
					ItemStack cursorCurrent = view.getCursor();
					if (ItemUtils.isEmpty(cursorCurrent)) return;
					ItemStack current = view.getItem(slot);
					if (ItemUtils.isEmpty(current)) {
						// place item from cursor:
						view.setItem(slot, cursorCurrent);
						view.setCursor(null);
					}
				});
			}
		}

		@Override
		protected void saveEditor(Inventory inventory, Player player) {
			TradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			shopkeeper.clearOffers();
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				ItemStack resultItem = inventory.getItem(column);
				if (ItemUtils.isEmpty(resultItem)) continue; // not valid recipe column

				ItemStack cost1 = ItemUtils.getNullIfEmpty(inventory.getItem(column + 9));
				ItemStack cost2 = ItemUtils.getNullIfEmpty(inventory.getItem(column + 18));

				// handle cost2 item as cost1 item if there is no cost1 item:
				if (cost1 == null) {
					cost1 = cost2;
					cost2 = null;
				}

				// add offer:
				if (cost1 != null) {
					shopkeeper.addOffer(resultItem, cost1, cost2);
				}
			}
		}
	}

	protected static class TradingPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected TradingPlayerShopTradingHandler(TradingPlayerShopkeeper shopkeeper) {
			super(shopkeeper);
		}

		@Override
		public TradingPlayerShopkeeper getShopkeeper() {
			return (TradingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean prepareTrade(TradeData tradeData) {
			if (!super.prepareTrade(tradeData)) return false;
			TradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Player tradingPlayer = tradeData.tradingPlayer;
			TradingRecipe tradingRecipe = tradeData.tradingRecipe;

			// find offer:
			TradingOffer offer = shopkeeper.getOffer(tradingRecipe);
			if (offer == null) {
				// this should not happen.. because the recipes were created based on the shopkeeper's offers
				this.debugPreventedTrade(tradingPlayer, "Couldn't find the offer corresponding to the trading recipe!");
				return false;
			}

			assert chestInventory != null & newChestContents != null;

			// remove result items from chest contents:
			ItemStack resultItem = tradingRecipe.getResultItem();
			assert resultItem != null;
			if (ItemUtils.removeItems(newChestContents, resultItem) != 0) {
				this.debugPreventedTrade(tradingPlayer, "The shop's chest doesn't contain the required items.");
				return false;
			}

			// add traded items to chest contents:
			if (!this.addItems(newChestContents, tradingRecipe.getItem1(), tradeData.offeredItem1)
					|| !this.addItems(newChestContents, tradingRecipe.getItem2(), tradeData.offeredItem2)) {
				this.debugPreventedTrade(tradingPlayer, "The shop's chest cannot hold the traded items.");
				return false;
			}
			return true;
		}

		// The items the trading player gave might slightly differ from the required items,
		// but are still accepted for the trade, depending on minecraft's item comparison and settings.
		// Therefore we differ between require and offered items here.
		// Returns false, if not all items could be added to the contents:
		private boolean addItems(ItemStack[] contents, ItemStack requiredItem, ItemStack offeredItem) {
			if (ItemUtils.isEmpty(requiredItem)) return true;
			int amountAfterTaxes = this.getAmountAfterTaxes(requiredItem.getAmount());
			if (amountAfterTaxes > 0) {
				ItemStack receivedItem = offeredItem.clone(); // create a copy, just in case
				receivedItem.setAmount(amountAfterTaxes);
				if (ItemUtils.addItems(contents, receivedItem) != 0) {
					// couldn't add all items to the contents:
					return false;
				}
			}
			return true;
		}
	}

	private final List<TradingOffer> offers = new ArrayList<>();
	private final List<TradingOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * Creates a not yet initialized {@link TradingPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected TradingPlayerShopkeeper(int id) {
		super(id);
	}

	protected TradingPlayerShopkeeper(int id, PlayerShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super(id);
		this.initOnCreation(shopCreationData);
	}

	protected TradingPlayerShopkeeper(int id, ConfigurationSection configSection) throws ShopkeeperCreateException {
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
		this._clearOffers();
		this._addOffers(TradingOffer.loadFromConfig(configSection, "offers"));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		// save offers:
		TradingOffer.saveToConfig(configSection, "offers", this.getOffers());
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

	private List<ItemCount> getItemsFromChest() {
		return this.getItemsFromChest(null);
	}

	// OFFERS:

	public List<TradingOffer> getOffers() {
		return offersView;
	}

	public TradingOffer addOffer(ItemStack resultItem, ItemStack item1, ItemStack item2) {
		// create offer (also handles validation):
		TradingOffer newOffer = new TradingOffer(resultItem, item1, item2);

		// add new offer:
		this._addOffer(newOffer);
		this.markDirty();
		return newOffer;
	}

	private void _addOffer(TradingOffer offer) {
		assert offer != null;
		offers.add(offer);
	}

	private void _addOffers(Collection<TradingOffer> offers) {
		assert offers != null;
		for (TradingOffer offer : offers) {
			if (offer == null) continue; // skip invalid entries
			// add new offer:
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

	public TradingOffer getOffer(TradingRecipe tradingRecipe) {
		for (TradingOffer offer : this.getOffers()) {
			if (offer.areItemsEqual(tradingRecipe)) {
				return offer;
			}
		}
		return null;
	}
}
