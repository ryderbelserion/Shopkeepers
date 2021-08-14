package com.nisovin.shopkeepers.ui.trading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.debug.Debug;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.AbstractShopkeeperUIHandler;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.bukkit.MerchantUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.Lazy;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public class TradingHandler extends AbstractShopkeeperUIHandler {

	// Those slot ids match both raw slot ids and regular slot ids for the merchant inventory view with the merchant
	// inventory at the top:
	protected static final int BUY_ITEM_1_SLOT_ID = 0;
	protected static final int BUY_ITEM_2_SLOT_ID = 1;
	protected static final int RESULT_ITEM_SLOT_ID = 2;

	// Counts the trades triggered by the last click-event:
	protected int tradeCounter = 0;

	public TradingHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType, shopkeeper);
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		Validate.notNull(player, "player is null");
		if (!PermissionUtils.hasPermission(player, ShopkeepersPlugin.TRADE_PERMISSION)) {
			if (!silent) {
				Log.debug(() -> "Blocked trade window opening for " + player.getName() + ": Missing trade permission.");
				TextUtils.sendMessage(player, Messages.missingTradePerm);
			}
			return false;
		}
		AbstractShopkeeper shopkeeper = this.getShopkeeper();
		if (!shopkeeper.hasTradingRecipes(player)) {
			if (!silent) {
				Log.debug(() -> "Blocked trade window opening for " + player.getName() + ": Shopkeeper has no offers.");
				TextUtils.sendMessage(player, Messages.cannotTradeNoOffers);

				// If the player can edit the shopkeeper, send instructions on how to open the editor:
				UIHandler editorHandler = shopkeeper.getUIHandler(SKDefaultUITypes.EDITOR());
				if (editorHandler != null && editorHandler.canOpen(player, true)) {
					TextUtils.sendMessage(player, Messages.noOffersOpenEditorDescription);
				}
			}
			return false;
		}
		return true;
	}

	@Override
	protected boolean openWindow(Player player) {
		Validate.notNull(player, "player is null");
		// Create and open the trading window:
		Shopkeeper shopkeeper = this.getShopkeeper();
		String title = this.getInventoryTitle();
		List<? extends TradingRecipe> recipes = shopkeeper.getTradingRecipes(player);
		if (recipes.isEmpty()) {
			Log.debug(() -> "Blocked trade window opening for " + player.getName() + ": Shopkeeper has no offers.");
			TextUtils.sendMessage(player, Messages.cannotTradeNoOffers);
			return false;
		}
		return this.openTradeWindow(title, recipes, player);
	}

	protected boolean openTradeWindow(String title, List<? extends TradingRecipe> recipes, Player player) {
		// Setup merchant:
		Merchant merchant = this.setupMerchant(title, recipes);

		// Increment 'talked-to-villager' statistic when opening trading menu:
		if (Settings.incrementVillagerStatistics) {
			player.incrementStatistic(Statistic.TALKED_TO_VILLAGER);
		}

		// Open merchant:
		return (player.openMerchant(merchant, true) != null);
	}

	protected Merchant setupMerchant(String title, List<? extends TradingRecipe> recipes) {
		// Setup merchant:
		Merchant merchant = Bukkit.createMerchant(title);
		this.setupMerchantRecipes(merchant, recipes);
		return merchant;
	}

	protected void setupMerchantRecipes(Merchant merchant, List<? extends TradingRecipe> recipes) {
		// Create list of merchant recipes:
		List<MerchantRecipe> merchantRecipes = this.createMerchantRecipes(recipes);
		// Set merchant's recipes:
		merchant.setRecipes(merchantRecipes);
	}

	protected List<MerchantRecipe> createMerchantRecipes(List<? extends TradingRecipe> recipes) {
		List<MerchantRecipe> merchantRecipes = new ArrayList<>();
		for (TradingRecipe recipe : recipes) {
			merchantRecipes.add(this.createMerchantRecipe(recipe));
		}
		return merchantRecipes;
	}

	protected MerchantRecipe createMerchantRecipe(TradingRecipe recipe) {
		return MerchantUtils.createMerchantRecipe(recipe); // Default
	}

	protected String getInventoryTitle() {
		String title = this.getShopkeeper().getName(); // Can be empty
		if (title.isEmpty()) {
			title = Messages.tradingTitleDefault;
		}
		return Messages.tradingTitlePrefix + title;
	}

	protected void updateTrades(Player player) {
		// Check if the currently open inventory still corresponds to this UI:
		if (!this.isOpen(player)) return;

		InventoryView openInventory = player.getOpenInventory();
		assert openInventory.getType() == InventoryType.MERCHANT;
		MerchantInventory merchantInventory = (MerchantInventory) openInventory.getTopInventory();
		Merchant merchant = merchantInventory.getMerchant();
		List<MerchantRecipe> oldMerchantRecipes = merchant.getRecipes();

		Shopkeeper shopkeeper = this.getShopkeeper();
		List<? extends TradingRecipe> recipes = shopkeeper.getTradingRecipes(player);
		List<MerchantRecipe> newMerchantRecipes = this.createMerchantRecipes(recipes);
		if (MerchantUtils.MERCHANT_RECIPES_IGNORE_USES_EXCEPT_BLOCKED.equals(oldMerchantRecipes, newMerchantRecipes)) {
			Log.debug(() -> "Trades are still up-to-date for player " + player.getName());
			return; // Recipes did not change
		}
		Log.debug(() -> "Updating trades for player " + player.getName());

		// It is not safe to reduce the number of trading recipes for the player, so we may need to add dummy recipes:
		this.ensureNoFewerRecipes(oldMerchantRecipes, newMerchantRecipes);

		// Set merchant's recipes:
		merchant.setRecipes(newMerchantRecipes);

		// Update recipes for the client:
		NMSManager.getProvider().updateTrades(player);
	}

	// Dynamically modifying trades (eg. their blocked state, or properties such as their items), or adding trades, is
	// fine. But reducing the number of trades is not safe, because the index of the currently selected recipe can end
	// up being out of bounds on the client. There is no way for us to remotely update it into valid bounds.
	// TODO Check if this still applies in MC 1.14+
	// We therefore insert blocked dummy trades to retain the previous recipe count. We could insert empty dummy trades
	// at the end of the recipe list, but that might confuse players since empty trades are rather unusual. Instead we
	// try to (heuristically) determine the recipes that were removed, and then insert blocked variants of these
	// recipes.
	private void ensureNoFewerRecipes(List<MerchantRecipe> oldMerchantRecipes, List<MerchantRecipe> newMerchantRecipes) {
		int oldRecipeCount = oldMerchantRecipes.size();
		int newRecipeCount = newMerchantRecipes.size();
		if (newRecipeCount >= oldRecipeCount) {
			// The new recipe list already contains no fewer recipes than the previous recipe list:
			return;
		}

		// Try to identify the removed recipes in order to insert blocked dummy recipes that likely make sense:
		// In order to keep the computational effort low, this heuristic simply walks through both recipe lists at the
		// same time and matches recipes based on their index and their items: If the items of the recipes at the same
		// index are the same, it is assumed that these recipes correspond to each other.
		// If the items of a recipe changed, or recipes were inserted at positions other than at the end of the list,
		// this heuristic may insert sub-optimal dummy recipes. However, it still ensures that the recipe list does not
		// shrink in size.
		for (int i = 0; i < oldRecipeCount; ++i) {
			MerchantRecipe oldRecipe = oldMerchantRecipes.get(i);
			MerchantRecipe newRecipe;
			if (i < newRecipeCount) {
				newRecipe = newMerchantRecipes.get(i);
			} else {
				newRecipe = null;
			}
			if (!MerchantUtils.MERCHANT_RECIPES_EQUAL_ITEMS.equals(oldRecipe, newRecipe)) {
				// The recipes at this index differ: Insert the old recipe into the new recipe list, but set its max
				// uses to 0 so that it cannot be used.
				oldRecipe.setMaxUses(0); // Block the trade
				newMerchantRecipes.add(i, oldRecipe);
				newRecipeCount++;
				if (newRecipeCount == oldRecipeCount) {
					// Abort the insertion of dummy recipes if we reached our goal of ensuring that the new recipe list
					// contains no fewer recipes than the old recipe list:
					break;
				}
			}
		}
		assert newRecipeCount == oldRecipeCount;
	}

	@Override
	protected boolean isWindow(InventoryView view) {
		return view != null && view.getType() == InventoryType.MERCHANT;
	}

	@Override
	protected void onInventoryClose(Player player, InventoryCloseEvent closeEvent) {
		// Callback for subclasses.
	}

	// TRADE PROCESSING

	// TODO This doesn't work because the client will automatically update the result slot item whenever a slot is
	// changed.
	// @Override
	// protected void onInventoryClickEarly(InventoryClickEvent clickEvent, Player player) {
	// // Clear the result item slot if we use strict item comparison and there is no valid trade:
	// // TODO We also need to do this when the player selects a trading recipe, because that will automatically insert
	// the matching items into the trading view.
	// if (!Settings.useStrictItemComparison) return;
	// if (clickEvent.isCancelled()) return;
	//
	// // This needs to happen after the event has been handled, because Minecraft will set the result slot afterwards:
	// SKUISession uiSession = SKShopkeepersPlugin.getInstance().getUIRegistry().getSession(player);
	// Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
	// if (!uiSession.isValid()) return;
	// if (clickEvent.isCancelled()) return;
	//
	// // Logs if it encounters items that are not strictly matching and then clears the result slot:
	// this.checkForTrade(clickEvent, true, false, false);
	// });
	// }

	// Late processing, so that other plugins can cancel the trading without having to rely on Shopkeepers' API.
	@Override
	protected void onInventoryClickLate(InventoryClickEvent clickEvent, Player player) {
		assert clickEvent != null && player != null;
		// Note: This expects that there are no other click-events while this event is getting processed.
		// Reset trade counter:
		tradeCounter = 0;

		Shopkeeper shopkeeper = this.getShopkeeper();
		String playerName = player.getName();
		if (clickEvent.isCancelled()) {
			Log.debug(() -> "Some plugin has cancelled the click in the trading window for "
					+ playerName + " at " + shopkeeper.getPositionString() + ".");
			return;
		}

		int rawSlot = clickEvent.getRawSlot();
		InventoryAction action = clickEvent.getAction();

		MerchantInventory merchantInventory = (MerchantInventory) clickEvent.getInventory();
		UnmodifiableItemStack resultItem = UnmodifiableItemStack.of(merchantInventory.getItem(RESULT_ITEM_SLOT_ID));
		ItemStack cursor = clickEvent.getCursor();

		// Prevent unsupported types of special clicks:
		if (action == InventoryAction.COLLECT_TO_CURSOR && ItemUtils.isSimilar(resultItem, cursor)) {
			// TODO Might no longer be supported in 1.14 and 1.15, see: https://bugs.mojang.com/browse/MC-148867
			// Weird behavior and buggy, see MC-129515
			// For now: Only allowed if the item on the cursor and inside the result slot are different.
			// TODO Maybe replicate the behavior of this inventory action, but limit its effect to the player's
			// inventory?
			Log.debug(() -> "Prevented unsupported special click in trading window by " + playerName
					+ " at " + shopkeeper.getPositionString() + ": " + action);
			clickEvent.setCancelled(true);
			InventoryUtils.updateInventoryLater(player);
			return;
		}

		// All currently supported inventory actions that might trigger trades involve a click of the result slot:
		if (rawSlot != RESULT_ITEM_SLOT_ID) {
			// Not canceling the event to allow regular inventory interaction inside the player's inventory.
			return;
		}

		// Some clicks on the result slot don't trigger trades:
		if (action == InventoryAction.CLONE_STACK) {
			return;
		}

		// We are handling all types of clicks which might trigger a trade ourselves:
		clickEvent.setCancelled(true);
		InventoryUtils.updateInventoryLater(player);

		// Check for a trade and prepare trade data:
		Trade trade = this.checkForTrade(clickEvent, false);
		if (trade == null) {
			// No trade available.
			return;
		}
		assert trade.tradingRecipe.getResultItem().isSimilar(resultItem);

		PlayerInventory playerInventory = player.getInventory();
		boolean isCursorEmpty = ItemUtils.isEmpty(cursor);

		// Handle trade depending on used inventory action:
		// TODO: In MC 1.15.1 PICKUP_ONE and PICKUP_SOME might get triggered when clicking the result slot (test again:
		// left click, shift+left click, right click, middle click).
		// TODO: Even though this is not available in vanilla Minecraft, maybe add a way to trade as often as possible,
		// using up all the items in the player's inventory (i.e. being able to sell all items with one click)?
		if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF) {
			if (!isCursorEmpty && (!resultItem.isSimilar(cursor) || (cursor.getAmount() + resultItem.getAmount()) > cursor.getMaxStackSize())) {
				Log.debug("Not handling trade: The cursor cannot carry the resulting items.");
				return;
			} else {
				if (this.handleTrade(trade)) {
					// Add result items to cursor:
					ItemStack resultCursor;
					if (isCursorEmpty) {
						resultCursor = resultItem.asItemStack(); // No item copy required here
					} else {
						resultCursor = ItemUtils.increaseItemAmount(cursor, resultItem.getAmount());
					}
					player.setItemOnCursor(resultCursor);

					// Common apply trade:
					this.commonApplyTrade(trade);
				}
				this.updateTrades(player);
			}
		} else if (action == InventoryAction.DROP_ONE_SLOT || action == InventoryAction.DROP_ALL_SLOT) {
			// Not supported for now, since this might be tricky to accurately reproduce.
			// dropItemNaturally is not equivalent to the player himself dropping the item
			// and inventoryView.setItem(-999, item) doesn't set the item's thrower (and there is no API to set that,
			// nor does the inventoryView return a reference to the dropped item).
			/*if (isCursorEmpty) {
				if (this.handleTrade(trade)) {
					// Drop result items:
					ItemStack droppedItem = resultItem.clone(); // todo Copy required?
					// todo Call drop event first
					player.getWorld().dropItemNaturally(player.getEyeLocation(), droppedItem);
				
					// Common apply trade:
					this.commonApplyTrade(trade);
				}
			}*/
		} else if (action == InventoryAction.HOTBAR_SWAP) {
			int hotbarButton = clickEvent.getHotbarButton();
			if (hotbarButton >= 0 && hotbarButton <= 8 && ItemUtils.isEmpty(playerInventory.getItem(hotbarButton))) {
				if (this.handleTrade(trade)) {
					// Set result items to hotbar slot:
					playerInventory.setItem(hotbarButton, resultItem.asItemStack()); // No item copy required here

					// Common apply trade:
					this.commonApplyTrade(trade);
				}
				this.updateTrades(player);
			}
		} else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			// Trades as often as possible (depending on offered items and inventory space) for the current result item:
			// If the current trading recipe is no longer fulfilled, and the currently selected recipe index is 0,
			// it will switch to the next applicable trading recipe, and continue the trading if the new result item is
			// equal to the previous result item.
			// TODO Handling each trade individually, eg. 64 times one item for one other item, can result in the trade
			// to fail if the chest of a player shop is full, even though it would in principal be possible to trade one
			// time 64 items for 64 items (because removing 64 items will clear a slot of the chest, whereas removing
			// only one item at a time may not). However, determining up front how often the trade can be applied would
			// be tricky, especially since the used trading recipe may change mid trading (at least in vanilla
			// Minecraft). Also, usually the situation may dynamically change in-between the individual trades
			// (especially if plugins or the shopkeepers themselves react to the individual trades), and each trade may
			// have other side effects. So trading one time 64 for 64 items may not be equivalent to trading 64 times
			// one item for one item.
			while (true) {
				// Check if there is enough space in the player's inventory:
				ItemStack[] newPlayerContents = playerInventory.getStorageContents();

				// Minecraft is adding items in reverse container order (starting with hotbar slot 9),
				// so we reverse the player contents accordingly before adding items:
				// Changes write through to the original array.
				List<ItemStack> listView = Arrays.asList(newPlayerContents);
				List<ItemStack> hotbarView = listView.subList(0, 9);
				List<ItemStack> contentsView = listView.subList(9, 36);
				Collections.reverse(hotbarView);
				Collections.reverse(contentsView);

				// No item copy required here:
				if (InventoryUtils.addItems(newPlayerContents, resultItem) != 0) {
					// Not enough inventory space, abort trading:
					break;
				}

				if (!this.handleTrade(trade)) {
					// Trade was aborted:
					break;
				}

				// Revert previous reverse:
				Collections.reverse(hotbarView);
				Collections.reverse(contentsView);

				// Apply player inventory changes:
				InventoryUtils.setStorageContents(playerInventory, newPlayerContents);

				// Common apply trade:
				this.commonApplyTrade(trade);

				// Check if we might continue trading:
				trade = this.checkForTrade(clickEvent, true); // Silent
				if (trade == null) {
					// No trade available:
					break;
				}
				// Compare result items:
				UnmodifiableItemStack newResultItem = trade.tradingRecipe.getResultItem();
				if (!newResultItem.isSimilar(resultItem)) {
					// The new result item does not match the previous result item.
					// Abort trading (mimics Minecraft behavior).
					break;
				}
				// Update result item:
				resultItem = newResultItem;
			}
			this.updateTrades(player);
		} else {
			// The inventory action involves the result slot, but doesn't trigger a trade usually, or isn't supported
			// yet.
		}
	}

	private void clearResultSlotForInvalidTrade(MerchantInventory merchantInventory) {
		// TODO This is not working currently. The client updates the result slot contents whenever it receives a slot
		// update from the server.
		// merchantInventory.setItem(RESULT_ITEM_SLOT_ID, null);
		// ItemUtils.updateInventoryLater(merchantInventory);
		// Log.debug("Result slot cleared due to invalid trade.");
	}

	private Trade checkForTrade(InventoryClickEvent clickEvent, boolean silent) {
		return this.checkForTrade(clickEvent, silent, silent, true);
	}

	// Checks for an available trade and does some preparation in case a trade is found.
	// Returns null if no trade could be prepared for some reason.
	private Trade checkForTrade(InventoryClickEvent clickEvent, boolean silent, boolean slientStrictItemComparison, boolean tradingContext) {
		Player tradingPlayer = (Player) clickEvent.getWhoClicked();
		MerchantInventory merchantInventory = (MerchantInventory) clickEvent.getView().getTopInventory();

		// Use null here instead of air for consistent behavior with previous versions:
		ItemStack offeredItem1 = ItemUtils.getNullIfEmpty(merchantInventory.getItem(BUY_ITEM_1_SLOT_ID));
		ItemStack offeredItem2 = ItemUtils.getNullIfEmpty(merchantInventory.getItem(BUY_ITEM_2_SLOT_ID));

		// Check for a result item:
		ItemStack resultItem = merchantInventory.getItem(RESULT_ITEM_SLOT_ID);
		if (ItemUtils.isEmpty(resultItem)) {
			if (!silent) {
				Log.debug("Not handling trade: There is no item in the clicked result slot (no trade available).");
				if (Debug.isDebugging(DebugOptions.emptyTrades)) {
					int selectedRecipeIndex = merchantInventory.getSelectedRecipeIndex();
					Log.debug("Selected trading recipe index: " + selectedRecipeIndex);
					TradingRecipe selectedTradingRecipe = MerchantUtils.getSelectedTradingRecipe(merchantInventory);
					if (selectedTradingRecipe == null) {
						// Can be null if the merchant has no trades at all.
						Log.debug("No trading recipe selected (merchant has no trades).");
					} else {
						debugLogItemStack("recipeItem1", selectedTradingRecipe.getItem1());
						debugLogItemStack("recipeItem2", selectedTradingRecipe.getItem2());
						debugLogItemStack("recipeResultItem", selectedTradingRecipe.getResultItem());
					}
					debugLogItemStack("offeredItem1", offeredItem1);
					debugLogItemStack("offeredItem2", offeredItem2);
				}
			}
			return null; // No trade available
		}

		// Find (and validate) the recipe Minecraft is using for the trade:
		TradingRecipe tradingRecipe = MerchantUtils.getActiveTradingRecipe(merchantInventory);
		if (tradingRecipe == null) {
			// Unexpected, since there is an item inside the result slot.
			if (!silent) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
				Log.debug("Not handling trade: Could not find the active trading recipe!");
			}
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}

		// As a safe-guard, check that the result item of the selected recipe actually matches the result item expected
		// by the player:
		UnmodifiableItemStack recipeResultItem = tradingRecipe.getResultItem();
		if (!recipeResultItem.equals(resultItem)) {
			// Unexpected, but may happen if some other plugin modifies the involved trades or items.
			if (!silent) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
				if (Debug.isDebugging()) {
					Log.debug("Not handling trade: The trade result item does not match the expected item of the active trading recipe!");
					debugLogItemStack("recipeResultItem", recipeResultItem);
					debugLogItemStack("resultItem", resultItem);
				}
			}
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}

		UnmodifiableItemStack requiredItem1 = tradingRecipe.getItem1();
		UnmodifiableItemStack requiredItem2 = tradingRecipe.getItem2();
		assert !ItemUtils.isEmpty(requiredItem1);

		// Minecraft checks both combinations (item1, item2) and (item2, item1) when determining if a trading recipe
		// matches, so we need to determine the used item order for the currently active trading recipe:
		boolean swappedItemOrder = false;
		if (this.matches(offeredItem1, offeredItem2, requiredItem1, requiredItem2)) {
			// Order is as-is.
		} else if (this.matches(offeredItem1, offeredItem2, requiredItem2, requiredItem1)) {
			// Swapped order:
			swappedItemOrder = true;
			ItemStack temp = offeredItem1;
			offeredItem1 = offeredItem2;
			offeredItem2 = temp;
		} else {
			// The used item order couldn't be determined.
			// This should not happen..
			// But this might for example happen if the FailedHandler#matches implementation falls back to using
			// the stricter isSimilar for the item comparison and the involved items are not strictly similar.
			if (!silent) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
				Log.debug("Not handling trade: Could not match the offered items to the active trading recipe!");
			}
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}
		assert offeredItem1 != null;

		if (Settings.useStrictItemComparison) {
			// Verify that the recipe items are perfectly matching (they can still be swapped though):
			boolean item1Similar = ItemUtils.isSimilar(requiredItem1, offeredItem1);
			ItemStack offeredItem2Final = offeredItem2;
			Lazy<Boolean> item2Similar = new Lazy<>(() -> ItemUtils.isSimilar(requiredItem2, offeredItem2Final));
			if (!item1Similar || !item2Similar.get()) {
				if (!slientStrictItemComparison) {
					// Feedback message:
					TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeItemsNotStrictlyMatching);

					// Additional debug output:
					if (Debug.isDebugging()) {
						String errorMsg = "The offered items do not strictly match the required items.";
						if (tradingContext) {
							this.debugPreventedTrade(tradingPlayer, errorMsg);
						} else {
							Log.debug(errorMsg);
						}

						Log.debug("Active trading recipe: " + ItemUtils.getSimpleRecipeInfo(tradingRecipe));
						if (!item1Similar) {
							debugLogItemStack("requiredItem1", requiredItem1);
							debugLogItemStack("offeredItem1", offeredItem1);
						}
						if (!item2Similar.get()) {
							debugLogItemStack("requiredItem2", requiredItem2);
							debugLogItemStack("offeredItem2", offeredItem2);
						}
					}
				}
				this.clearResultSlotForInvalidTrade(merchantInventory);
				return null;
			}
		}

		// Create and setup a new Trade:
		Trade trade = new Trade(clickEvent, merchantInventory, tradingPlayer, tradingRecipe, offeredItem1, offeredItem2, swappedItemOrder);
		this.setupTrade(trade);
		return trade;
	}

	private boolean matches(ItemStack offeredItem1, ItemStack offeredItem2, UnmodifiableItemStack requiredItem1, UnmodifiableItemStack requiredItem2) {
		int offeredItem1Amount = ItemUtils.getItemStackAmount(offeredItem1);
		int offeredItem2Amount = ItemUtils.getItemStackAmount(offeredItem2);
		int requiredItem1Amount = ItemUtils.getItemStackAmount(requiredItem1);
		int requiredItem2Amount = ItemUtils.getItemStackAmount(requiredItem2);
		return (offeredItem1Amount >= requiredItem1Amount
				&& offeredItem2Amount >= requiredItem2Amount
				&& NMSManager.getProvider().matches(offeredItem1, requiredItem1)
				&& NMSManager.getProvider().matches(offeredItem2, requiredItem2));
	}

	protected final void debugPreventedTrade(Player player, String reason) {
		Log.debug(() -> "Prevented trade by " + player.getName() + " with shopkeeper at "
				+ this.getShopkeeper().getPositionString() + ": " + reason);
	}

	/**
	 * This is called for every newly created {@link Trade} and can be used by sub-classes to setup additional metadata
	 * that is relevant for processing the trade.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void setupTrade(Trade trade) {
		// Callback for subclasses.
	}

	// Returns true if the trade was not aborted and is now supposed to get applied.
	private boolean handleTrade(Trade trade) {
		assert trade != null;
		// Increase trade counter:
		tradeCounter++;

		// Check and prepare the trade:
		if (!this.prepareTrade(trade)) {
			// The trade got cancelled for some shopkeeper-specific reason:
			this.onTradeAborted(trade);
			return false;
		}

		// Call trade event, giving other plugins a chance to cancel the trade before it gets applied:
		// Prepare the offered items for the event: Clone and ensure that the stack sizes match the trading recipe.
		TradingRecipe tradingRecipe = trade.tradingRecipe;
		ItemStack eventOfferedItem1 = ItemUtils.copyWithAmount(trade.offeredItem1, tradingRecipe.getItem1().getAmount());
		ItemStack eventOfferedItem2 = ItemUtils.cloneOrNullIfEmpty(trade.offeredItem2);
		if (eventOfferedItem2 != null) {
			// Minecraft disables the trade if there is second offered item but the trade only expects a single item.
			assert tradingRecipe.getItem2() != null;
			eventOfferedItem2.setAmount(tradingRecipe.getItem2().getAmount());
		}

		ShopkeeperTradeEvent tradeEvent = new ShopkeeperTradeEvent(this.getShopkeeper(), trade.tradingPlayer,
				trade.clickEvent, tradingRecipe, UnmodifiableItemStack.of(eventOfferedItem1),
				UnmodifiableItemStack.of(eventOfferedItem2), trade.swappedItemOrder);
		Bukkit.getPluginManager().callEvent(tradeEvent);
		if (tradeEvent.isCancelled()) {
			Log.debug("The trade got cancelled by some other plugin.");
			this.onTradeAborted(trade);
			return false;
		}
		// Making sure that the click event is still cancelled:
		if (!trade.clickEvent.isCancelled()) {
			Log.warning("Some plugin tried to uncancel the click event during trade handling!");
			trade.clickEvent.setCancelled(true);
		}

		// Assert: The click event and the affected inventories should not get modified during the event!

		// We are going to apply the trade now:
		this.preApplyTrade(trade);
		return true;
	}

	private void commonApplyTrade(Trade trade) {
		// Update merchant inventory contents:
		MerchantInventory merchantInventory = trade.merchantInventory;
		merchantInventory.setItem(RESULT_ITEM_SLOT_ID, null); // Clear result slot, just in case

		TradingRecipe tradingRecipe = trade.tradingRecipe;
		ItemStack newOfferedItem1 = ItemUtils.descreaseItemAmount(trade.offeredItem1, ItemUtils.getItemStackAmount(tradingRecipe.getItem1()));
		ItemStack newOfferedItem2 = ItemUtils.descreaseItemAmount(trade.offeredItem2, ItemUtils.getItemStackAmount(tradingRecipe.getItem2()));
		// Inform the merchant inventory about the change (updates the active trading recipe and result item):
		merchantInventory.setItem(trade.swappedItemOrder ? BUY_ITEM_2_SLOT_ID : BUY_ITEM_1_SLOT_ID, newOfferedItem1);
		merchantInventory.setItem(trade.swappedItemOrder ? BUY_ITEM_1_SLOT_ID : BUY_ITEM_2_SLOT_ID, newOfferedItem2);

		// TODO Increase uses of corresponding MerchanRecipe?
		// TODO Add support for exp-rewards?
		// TODO Support modifications to the MerchantRecipe's maxUses?

		// Increment 'traded-with-villager' statistic for every trade:
		if (Settings.incrementVillagerStatistics) {
			Player player = trade.tradingPlayer;
			player.incrementStatistic(Statistic.TRADED_WITH_VILLAGER);
		}

		// Shopkeeper-specific application of the trade:
		this.onTradeApplied(trade);

		// Log trade:
		Log.debug(() -> "Trade (#" + tradeCounter + ") by " + trade.tradingPlayer.getName() + " with shopkeeper at "
				+ this.getShopkeeper().getPositionString() + ": " + ItemUtils.getSimpleRecipeInfo(tradingRecipe));
	}

	/**
	 * Checks whether the trade can be performed and makes any preparations required for applying the trade in case it
	 * actually gets performed.
	 * <p>
	 * This is called for every trade a player triggered through a merchant inventory action. Depending on the inventory
	 * action multiple successive trades (even using different trading recipes) might get triggered by a single
	 * inventory action.
	 * <p>
	 * There should be no changes of the corresponding click event and the involved inventories (player, container) to
	 * be expected between this phase of the trade handling and the actual application of the trade.
	 * 
	 * @param trade
	 *            the trade
	 * @return <code>true</code> to continue trade handling, <code>false</code> to cancel the trade and any successive
	 *         trades triggered by the same inventory click
	 */
	protected boolean prepareTrade(Trade trade) {
		return true;
	}

	/**
	 * Called when a previously already prepared trade got aborted for some reason.
	 * <p>
	 * This is also called if the trade was aborted by {@link #prepareTrade(Trade)} itself.
	 * <p>
	 * This can be used to perform any necessary cleanup.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void onTradeAborted(Trade trade) {
		// Callback for subclasses.
	}

	/**
	 * This is called right before a trade gets applied.
	 * <p>
	 * This can be used to perform any kind of pre-processing which needs to happen first.
	 * <p>
	 * At this phase of the trade handling, the trade should no longer get cancelled. Any conditions which could prevent
	 * a trade from getting successfully applied have to be checked inside {@link #prepareTrade(Trade)} instead.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void preApplyTrade(Trade trade) {
		// Callback for subclasses.
	}

	/**
	 * This is called right after a trade has been applied.
	 * <p>
	 * This can be used to perform any kind of post-processing which needs to happen last. For example any shopkeeper
	 * specific behavior required for applying the trade can happen here.
	 * <p>
	 * At this phase of the trade handling, the trade should no longer get cancelled. Any conditions which could prevent
	 * a trade from getting successfully applied have to be checked inside {@link #prepareTrade(Trade)} instead.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void onTradeApplied(Trade trade) {
		// Callback for subclasses.
	}

	// TODO Ensure a minimum amount of 1?
	// Returns a value >= 0 and <= amount.
	protected int getAmountAfterTaxes(int amount) {
		assert amount >= 0;
		if (Settings.taxRate == 0) return amount;
		int taxes = 0;
		if (Settings.taxRoundUp) {
			taxes = (int) Math.ceil(amount * (Settings.taxRate / 100.0D));
		} else {
			taxes = (int) Math.floor(amount * (Settings.taxRate / 100.0D));
		}
		return Math.max(0, Math.min(amount - taxes, amount));
	}

	private static void debugLogItemStack(String itemStackName, UnmodifiableItemStack itemStack) {
		debugLogItemStack(itemStackName, ItemUtils.asItemStackOrNull(itemStack));
	}

	private static void debugLogItemStack(String itemStackName, ItemStack itemStack) {
		Object itemStackData = (itemStack != null) ? itemStack : "<empty>";
		Log.debug(ConfigUtils.toConfigYamlWithoutTrailingNewline(itemStackName, itemStackData));
	}
}
