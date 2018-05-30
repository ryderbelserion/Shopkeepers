package com.nisovin.shopkeepers.shoptypes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Material;
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

public class BuyingPlayerShopkeeper extends PlayerShopkeeper {

	protected static class BuyingPlayerShopEditorHandler extends PlayerShopEditorHandler {

		protected BuyingPlayerShopEditorHandler(UIType uiType, BuyingPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
		}

		@Override
		public BuyingPlayerShopkeeper getShopkeeper() {
			return (BuyingPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			BuyingPlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// add the shopkeeper's offers:
			List<ItemCount> chestItems = shopkeeper.getItemsFromChest();
			for (int column = 0; column < chestItems.size() && column < TRADE_COLUMNS; column++) {
				ItemCount itemCount = chestItems.get(column);
				ItemStack type = itemCount.getItem(); // this item is already a copy with amount 1
				ItemStack currencyItem = null;
				PriceOffer offer = shopkeeper.getOffer(type);

				if (offer != null) {
					currencyItem = Settings.createCurrencyItem(offer.getPrice());
					int tradedItemAmount = offer.getItem().getAmount();
					type.setAmount(tradedItemAmount);
				} else {
					currencyItem = Settings.createZeroCurrencyItem();
				}
				assert currencyItem != null;

				// add offer to inventory:
				inventory.setItem(column, currencyItem);
				inventory.setItem(column + 18, type);
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
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				ItemStack tradedItem = inventory.getItem(column + 18);
				if (ItemUtils.isEmpty(tradedItem)) continue;

				ItemStack priceItem = inventory.getItem(column);
				if (priceItem != null && priceItem.getType() == Settings.currencyItem && priceItem.getAmount() > 0) {
					shopkeeper.addOffer(tradedItem, priceItem.getAmount());
				} else {
					shopkeeper.removeOffer(tradedItem);
				}
			}
		}
	}

	protected class BuyingPlayerShopTradingHandler extends PlayerShopTradingHandler {

		protected BuyingPlayerShopTradingHandler(UIType uiType, BuyingPlayerShopkeeper shopkeeper) {
			super(uiType, shopkeeper);
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
	private final List<PriceOffer> offers = new ArrayList<PriceOffer>();
	private final List<PriceOffer> offersView = Collections.unmodifiableList(offers);

	/**
	 * For use in extending classes.
	 */
	protected BuyingPlayerShopkeeper() {
	}

	public BuyingPlayerShopkeeper(ConfigurationSection config) throws ShopkeeperCreateException {
		this.initOnLoad(config);
		this.onInitDone();
	}

	public BuyingPlayerShopkeeper(PlayerShopCreationData creationData) throws ShopkeeperCreateException {
		this.initOnCreation(creationData);
		this.onInitDone();
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new BuyingPlayerShopEditorHandler(DefaultUIs.EDITOR_WINDOW, this));
		this.registerUIHandler(new BuyingPlayerShopTradingHandler(DefaultUIs.TRADING_WINDOW, this));
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
	protected void save(ConfigurationSection config) {
		super.save(config);
		// save offers:
		PriceOffer.saveToConfig(config, "offers", this.getOffers());
	}

	@Override
	public ShopType<?> getType() {
		return DefaultShopTypes.PLAYER_BUYING();
	}

	@Override
	public List<TradingRecipe> getTradingRecipes(Player player) {
		List<TradingRecipe> recipes = new ArrayList<TradingRecipe>();
		List<ItemCount> chestItems = this.getItemsFromChest();
		int currencyInChest = this.getCurrencyInChest();
		for (PriceOffer offer : this.getOffers()) {
			ItemStack tradedItem = offer.getItem();
			ItemCount itemCount = ItemCount.findSimilar(chestItems, tradedItem);
			if (itemCount == null) continue;

			if (currencyInChest >= offer.getPrice()) {
				TradingRecipe recipe = this.createBuyingRecipe(tradedItem, offer.getPrice());
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
