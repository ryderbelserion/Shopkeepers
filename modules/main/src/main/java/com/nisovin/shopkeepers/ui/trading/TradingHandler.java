package com.nisovin.shopkeepers.ui.trading;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.TradeSelectEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.PlayerInventory;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperTradeEvent;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.ui.UISession;
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
import com.nisovin.shopkeepers.ui.state.UIState;
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

	private static final Set<? extends @NonNull Class<? extends @NonNull InventoryEvent>> ADDITIONAL_INVENTORY_EVENTS = Collections.singleton(
			TradeSelectEvent.class
	);

	// Those slot ids match both raw slot ids and regular slot ids for the merchant inventory view
	// with the merchant inventory at the top:
	protected static final int BUY_ITEM_1_SLOT_ID = 0;
	protected static final int BUY_ITEM_2_SLOT_ID = 1;
	protected static final int RESULT_ITEM_SLOT_ID = 2;

	private final List<@NonNull TradingListener> tradingListeners = new ArrayList<>();

	public TradingHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType, shopkeeper);
	}

	@Override
	protected Set<? extends @NonNull Class<? extends @NonNull InventoryEvent>> getAdditionalInventoryEvents() {
		return ADDITIONAL_INVENTORY_EVENTS;
	}

	/**
	 * Registers the given {@link TradingListener}.
	 * 
	 * @param listener
	 *            the listener, not <code>null</code>
	 */
	public final void addListener(TradingListener listener) {
		Validate.notNull(listener, "listener is null");
		tradingListeners.add(listener);
	}

	@Override
	public boolean canOpen(Player player, boolean silent) {
		Validate.notNull(player, "player is null");
		if (!PermissionUtils.hasPermission(player, ShopkeepersPlugin.TRADE_PERMISSION)) {
			if (!silent) {
				this.debugNotOpeningUI(player, "Player is missing trade permission.");
				TextUtils.sendMessage(player, Messages.missingTradePerm);
			}
			return false;
		}
		AbstractShopkeeper shopkeeper = this.getShopkeeper();
		if (!shopkeeper.hasTradingRecipes(player)) {
			if (!silent) {
				this.debugNotOpeningUI(player, "Shopkeeper has no offers.");
				TextUtils.sendMessage(player, Messages.cannotTradeNoOffers);

				// If the player can edit the shopkeeper, send instructions on how to open the
				// editor:
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
	protected boolean openWindow(UISession uiSession, UIState uiState) {
		Validate.notNull(uiSession, "uiSession is null");
		this.validateState(uiState);

		// Create and open the trading window:
		Player player = uiSession.getPlayer();
		Shopkeeper shopkeeper = this.getShopkeeper();
		String title = this.getInventoryTitle();
		List<? extends @NonNull TradingRecipe> recipes = shopkeeper.getTradingRecipes(player);
		if (recipes.isEmpty()) {
			this.debugNotOpeningUI(player, "Shopkeeper has no offers.");
			TextUtils.sendMessage(player, Messages.cannotTradeNoOffers);
			return false;
		}
		return this.openTradeWindow(title, recipes, player);
	}

	protected boolean openTradeWindow(
			String title,
			List<? extends @NonNull TradingRecipe> recipes,
			Player player
	) {
		// Set up merchant:
		Merchant merchant = this.setupMerchant(title, recipes);

		// Increment 'talked-to-villager' statistic when opening trading menu:
		if (Settings.incrementVillagerStatistics) {
			player.incrementStatistic(Statistic.TALKED_TO_VILLAGER);
		}

		// Open merchant:
		return (player.openMerchant(merchant, true) != null);
	}

	protected Merchant setupMerchant(String title, List<? extends @NonNull TradingRecipe> recipes) {
		Merchant merchant = Bukkit.createMerchant(title);
		this.setupMerchantRecipes(merchant, recipes);
		return merchant;
	}

	protected void setupMerchantRecipes(
			Merchant merchant,
			List<? extends @NonNull TradingRecipe> recipes
	) {
		// Create list of merchant recipes:
		List<@NonNull MerchantRecipe> merchantRecipes = this.createMerchantRecipes(recipes);
		// Set merchant's recipes:
		merchant.setRecipes(Unsafe.cast(merchantRecipes));
	}

	protected List<@NonNull MerchantRecipe> createMerchantRecipes(
			List<? extends @NonNull TradingRecipe> recipes
	) {
		List<@NonNull MerchantRecipe> merchantRecipes = new ArrayList<>();
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
		@NonNull List<@NonNull MerchantRecipe> oldMerchantRecipes = Unsafe.cast(merchant.getRecipes());

		Shopkeeper shopkeeper = this.getShopkeeper();
		List<? extends @NonNull TradingRecipe> recipes = shopkeeper.getTradingRecipes(player);
		List<@NonNull MerchantRecipe> newMerchantRecipes = this.createMerchantRecipes(recipes);
		if (MerchantUtils.MERCHANT_RECIPES_IGNORE_USES_EXCEPT_BLOCKED.equals(
				oldMerchantRecipes,
				newMerchantRecipes
		)) {
			Log.debug(() -> shopkeeper.getLogPrefix() + "Trades are still up-to-date for player "
					+ player.getName());
			return; // Recipes did not change
		}
		Log.debug(() -> shopkeeper.getLogPrefix() + "Updating trades for player "
				+ player.getName());

		// It is not safe to reduce the number of trading recipes for the player, so we may need to
		// add dummy recipes:
		this.ensureNoFewerRecipes(oldMerchantRecipes, newMerchantRecipes);

		// Set merchant's recipes:
		merchant.setRecipes(Unsafe.cast(newMerchantRecipes));

		// Update recipes for the client:
		NMSManager.getProvider().updateTrades(player);
	}

	// Dynamically modifying trades (e.g. their blocked state, or properties such as their items),
	// or adding trades, is fine. But reducing the number of trades is not safe, because the index
	// of the currently selected recipe can end up being out of bounds on the client. There is no
	// way for us to remotely update it into valid bounds.
	// TODO Check if this still applies in MC 1.14+
	// We therefore insert blocked dummy trades to retain the previous recipe count. We could insert
	// empty dummy trades at the end of the recipe list, but that might confuse players since empty
	// trades are rather unusual. Instead we try to (heuristically) determine the recipes that were
	// removed, and then insert blocked variants of these recipes.
	private void ensureNoFewerRecipes(
			List<? extends @NonNull MerchantRecipe> oldMerchantRecipes,
			List<@NonNull MerchantRecipe> newMerchantRecipes
	) {
		int oldRecipeCount = oldMerchantRecipes.size();
		int newRecipeCount = newMerchantRecipes.size();
		if (newRecipeCount >= oldRecipeCount) {
			// The new recipe list already contains no fewer recipes than the previous recipe list:
			return;
		}

		// Try to identify the removed recipes in order to insert blocked dummy recipes that likely
		// make sense:
		// In order to keep the computational effort low, this heuristic simply walks through both
		// recipe lists at the same time and matches recipes based on their index and their items:
		// If the items of the recipes at the same index are the same, it is assumed that these
		// recipes correspond to each other.
		// If the items of a recipe changed, or recipes were inserted at positions other than at the
		// end of the list, this heuristic may insert sub-optimal dummy recipes. However, it still
		// ensures that the recipe list does not shrink in size.
		for (int i = 0; i < oldRecipeCount; ++i) {
			MerchantRecipe oldRecipe = oldMerchantRecipes.get(i);
			MerchantRecipe newRecipe;
			if (i < newRecipeCount) {
				newRecipe = newMerchantRecipes.get(i);
			} else {
				newRecipe = null;
			}
			if (!MerchantUtils.MERCHANT_RECIPES_EQUAL_ITEMS.equals(oldRecipe, newRecipe)) {
				// The recipes at this index differ: Insert the old recipe into the new recipe list,
				// but set its max uses to 0 so that it cannot be used.
				oldRecipe.setMaxUses(0); // Block the trade
				newMerchantRecipes.add(i, oldRecipe);
				newRecipeCount++;
				if (newRecipeCount == oldRecipeCount) {
					// Abort the insertion of dummy recipes if we reached our goal of ensuring that
					// the new recipe list contains no fewer recipes than the old recipe list:
					break;
				}
			}
		}
		assert newRecipeCount == oldRecipeCount;
	}

	@Override
	protected boolean isWindow(InventoryView view) {
		Validate.notNull(view, "view is null");
		return view.getType() == InventoryType.MERCHANT;
	}

	@Override
	protected void onInventoryClose(UISession uiSession, @Nullable InventoryCloseEvent closeEvent) {
		// Callback for subclasses.
	}

	// TRADE PROCESSING

	// TODO This doesn't work because the client will automatically update the result slot item
	// whenever a slot is changed.
	/*@Override
	protected void onInventoryClickEarly(InventoryClickEvent clickEvent, Player player) {
		// Clear the result item slot if we use strict item comparison and there is no valid trade:
		// TODO We also need to do this when the player selects a trading recipe, because that will
		// automatically insert the matching items into the trading view.
		if (!Settings.useStrictItemComparison) return;
		if (clickEvent.isCancelled()) return;
		// This needs to happen after the event has been handled, because Minecraft will set the
		// result slot afterwards:
		SKUISession uiSession = SKShopkeepersPlugin.getInstance().getUIRegistry().getSession(player);
		Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
			if (!uiSession.isValid()) return;
			if (clickEvent.isCancelled()) return;
			// Logs if it encounters items that are not strictly matching and then clears the result
			// slot:
			this.checkForTrade(clickEvent, true, false, false);
		});
	}*/

	@Override
	protected void onInventoryEventEarly(UISession uiSession, InventoryEvent event) {
		if (event instanceof TradeSelectEvent) {
			// Inform listeners:
			tradingListeners.forEach(listener -> {
				listener.onTradeSelect(uiSession, (TradeSelectEvent) event);
			});
		}
	}

	private boolean canSlotHoldItemStack(@Nullable ItemStack slotItem, ItemStack itemStack) {
		if (ItemUtils.isEmpty(slotItem)) return true;
		assert slotItem != null;
		Unsafe.assertNonNull(slotItem);
		if (!itemStack.isSimilar(slotItem)) return false;
		return slotItem.getAmount() + itemStack.getAmount() <= itemStack.getMaxStackSize();
	}

	// Late processing, so that other plugins can cancel the trading without having to rely on
	// Shopkeepers' API.
	@Override
	protected void onInventoryClickLate(UISession uiSession, InventoryClickEvent clickEvent) {
		assert uiSession != null && clickEvent != null;

		// Inform listeners:
		tradingListeners.forEach(listener -> listener.onInventoryClick(uiSession, clickEvent));

		Player player = uiSession.getPlayer();
		Shopkeeper shopkeeper = this.getShopkeeper();

		if (clickEvent.isCancelled()) {
			Log.debug(() -> shopkeeper.getLogPrefix()
					+ "Some plugin has cancelled the trading UI click of player "
					+ player.getName());
			return;
		}

		int rawSlot = clickEvent.getRawSlot();
		InventoryAction action = clickEvent.getAction();

		MerchantInventory merchantInventory = (MerchantInventory) clickEvent.getInventory();
		UnmodifiableItemStack resultItem = UnmodifiableItemStack.of(
				merchantInventory.getItem(RESULT_ITEM_SLOT_ID)
		);
		ItemStack cursor = clickEvent.getCursor();

		// Prevent unsupported types of special clicks:
		if (action == InventoryAction.COLLECT_TO_CURSOR
				&& ItemUtils.isSimilar(resultItem, cursor)) {
			// MC-129515: In the past, the behavior of this inventory action was rather weird and
			// buggy if the clicked item matches the trade result item. We therefore cancel and
			// ignore it if the cursor item matches the result item.
			// MC-148867: Since MC 1.14, Mojang fully disabled this inventory action inside the
			// trading UI, so this case should no longer be reached. We still explicitly cancel it,
			// just in case.
			Log.debug(() -> shopkeeper.getLogPrefix()
					+ "Prevented unsupported type of trading UI click by player " + player.getName()
					+ ": " + action);
			clickEvent.setCancelled(true);
			InventoryUtils.updateInventoryLater(player);
			return;
		}

		// All currently supported inventory actions that might trigger trades involve a click of
		// the result slot:
		if (rawSlot != RESULT_ITEM_SLOT_ID) {
			// Not canceling the event to allow regular inventory interaction inside the player's
			// inventory.
			return;
		}

		// Some clicks on the result slot don't trigger trades:
		if (action == InventoryAction.CLONE_STACK) {
			return;
		}

		// We are handling all types of clicks which might trigger a trade ourselves:
		clickEvent.setCancelled(true);
		InventoryUtils.updateInventoryLater(player);

		// Set up a new TradingContext:
		TradingContext tradingContext = new TradingContext(shopkeeper, clickEvent);
		this.setupTradingContext(tradingContext);

		// Check for a trade:
		Trade trade = this.checkForTrade(tradingContext, false);
		if (trade == null) {
			// No trade available.
			return;
		}
		resultItem = Unsafe.assertNonNull(resultItem);
		assert trade.getTradingRecipe().getResultItem().isSimilar(resultItem);

		PlayerInventory playerInventory = player.getInventory();
		boolean isCursorEmpty = ItemUtils.isEmpty(cursor);

		// Handle trade depending on used inventory action:
		// TODO: In MC 1.15.1 PICKUP_ONE and PICKUP_SOME might get triggered when clicking the
		// result slot (test again: left click, shift+left click, right click, middle click).
		// TODO: Even though this is not available in vanilla Minecraft, maybe add a way to trade as
		// often as possible, using up all the items in the player's inventory (i.e. being able to
		// sell all items with one click)?
		if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_HALF) {
			if (!canSlotHoldItemStack(cursor, resultItem.asItemStack())) {
				Log.debug(() -> shopkeeper.getLogPrefix()
						+ "Not handling trade: The cursor cannot carry the resulting items.");
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
			// dropItemNaturally is not equivalent to the player himself dropping the item and
			// inventoryView.setItem(-999, item) doesn't set the item's thrower (and there is no API
			// to set that, nor does the inventoryView return a reference to the dropped item).
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
			if (hotbarButton >= 0 && hotbarButton <= 8
					&& ItemUtils.isEmpty(playerInventory.getItem(hotbarButton))) {
				if (this.handleTrade(trade)) {
					// Set result items to hotbar slot:
					// No item copy required here:
					playerInventory.setItem(hotbarButton, resultItem.asItemStack());

					// Common apply trade:
					this.commonApplyTrade(trade);
				}
				this.updateTrades(player);
			}
		} else if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
			// Trades as often as possible (depending on offered items and inventory space) for the
			// current result item:
			// If the current trading recipe is no longer fulfilled, and the currently selected
			// recipe index is 0, it will switch to the next applicable trading recipe, and continue
			// the trading if the new result item is equal to the previous result item.
			// TODO Handling each trade individually, e.g. 64 times one item for one other item, can
			// result in the trade to fail if the chest of a player shop is full, even though it
			// would in principle be possible to trade one time 64 items for 64 items (because
			// removing 64 items will clear a slot of the chest, whereas removing only one item at a
			// time may not). However, determining up front how often the trade can be applied would
			// be tricky, especially since the used trading recipe may change mid trading (at least
			// in vanilla Minecraft). Also, usually the situation may dynamically change in-between
			// the individual trades (especially if plugins or the shopkeepers themselves react to
			// the individual trades), and each trade may have other side effects. So trading one
			// time 64 for 64 items may not be equivalent to trading 64 times one item for one item.
			while (true) {
				// Check if there is enough space in the player's inventory:
				ItemStack[] newPlayerContents = playerInventory.getStorageContents();

				// Minecraft is adding items in reverse container order (starting with hotbar slot
				// 9), so we reverse the player contents accordingly before adding items:
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

				// Check if we can continue trading:
				trade = this.checkForTrade(tradingContext, true); // Silent
				if (trade == null) {
					// No trade available:
					break;
				}
				// Compare result items:
				UnmodifiableItemStack newResultItem = trade.getTradingRecipe().getResultItem();
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
			// The inventory action involves the result slot, but does not usually trigger a trade,
			// or is not supported yet.
		}
	}

	private void clearResultSlotForInvalidTrade(MerchantInventory merchantInventory) {
		// TODO This is not working currently. The client updates the result slot contents whenever
		// it receives a slot update from the server.
		/*merchantInventory.setItem(RESULT_ITEM_SLOT_ID, null);
		ItemUtils.updateInventoryLater(merchantInventory);
		Log.debug("Result slot cleared due to invalid trade.");*/
	}

	private @Nullable Trade checkForTrade(TradingContext tradingContext, boolean silent) {
		return this.checkForTrade(tradingContext, silent, silent, true);
	}

	// Checks for an available trade and does some preparation in case a trade is found.
	// Returns null if no trade could be prepared for some reason.
	private @Nullable Trade checkForTrade(
			TradingContext tradingContext,
			boolean silent,
			boolean slientStrictItemComparison,
			boolean isInTradingContext
	) {
		// Start the processing of a new trade attempt:
		tradingContext.startNewTrade();

		Shopkeeper shopkeeper = tradingContext.getShopkeeper();
		Player tradingPlayer = tradingContext.getTradingPlayer();
		MerchantInventory merchantInventory = tradingContext.getMerchantInventory();

		// Use null here instead of air for consistent behavior with previous versions:
		ItemStack offeredItem1 = ItemUtils.getNullIfEmpty(
				merchantInventory.getItem(BUY_ITEM_1_SLOT_ID)
		);
		ItemStack offeredItem2 = ItemUtils.getNullIfEmpty(
				merchantInventory.getItem(BUY_ITEM_2_SLOT_ID)
		);

		// Check for a result item:
		ItemStack resultItem = merchantInventory.getItem(RESULT_ITEM_SLOT_ID);
		if (ItemUtils.isEmpty(resultItem)) {
			if (!silent) {
				Log.debug(() -> shopkeeper.getLogPrefix() + "Not handling trade: "
						+ "There is no item in the clicked result slot (no trade available).");
				if (Debug.isDebugging(DebugOptions.emptyTrades)) {
					int selectedRecipeIndex = merchantInventory.getSelectedRecipeIndex();
					Log.debug("Selected trading recipe index: " + selectedRecipeIndex);
					TradingRecipe selectedTradingRecipe = MerchantUtils.getSelectedTradingRecipe(
							merchantInventory
					);
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
				Log.debug(() -> shopkeeper.getLogPrefix()
						+ "Not handling trade: Could not find the active trading recipe!");
			}
			this.onTradeAborted(tradingContext, silent);
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}

		// As a safe-guard, check that the result item of the selected recipe actually matches the
		// result item expected by the player:
		UnmodifiableItemStack recipeResultItem = tradingRecipe.getResultItem();
		if (!recipeResultItem.equals(resultItem)) {
			// Unexpected, but may happen if some other plugin modifies the involved trades or
			// items.
			if (!silent) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
				if (Debug.isDebugging()) {
					Log.debug(shopkeeper.getLogPrefix() + "Not handling trade: "
							+ "The trade result item does not match the expected item of the "
							+ "active trading recipe!");
					debugLogItemStack("recipeResultItem", recipeResultItem);
					debugLogItemStack("resultItem", resultItem);
				}
			}
			this.onTradeAborted(tradingContext, silent);
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}

		UnmodifiableItemStack requiredItem1 = tradingRecipe.getItem1();
		UnmodifiableItemStack requiredItem2 = tradingRecipe.getItem2();
		assert !ItemUtils.isEmpty(requiredItem1);

		// Minecraft checks both combinations (item1, item2) and (item2, item1) when determining if
		// a trading recipe matches, so we need to determine the used item order for the currently
		// active trading recipe:
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
			// The used item order could not be determined.
			// This should not happen. But this might for example occur if the FailedHandler#matches
			// implementation falls back to using the stricter isSimilar for the item comparison and
			// the involved items are not strictly similar.
			if (!silent) {
				TextUtils.sendMessage(tradingPlayer, Messages.cannotTradeUnexpectedTrade);
				Log.debug(() -> shopkeeper.getLogPrefix() + "Not handling trade: "
						+ "Could not match the offered items to the active trading recipe!");
			}
			this.onTradeAborted(tradingContext, silent);
			this.clearResultSlotForInvalidTrade(merchantInventory);
			return null;
		}
		offeredItem1 = Unsafe.assertNonNull(offeredItem1);

		if (Settings.useStrictItemComparison) {
			// Verify that the recipe items are perfectly matching (they can still be swapped
			// though):
			boolean item1Similar = ItemUtils.isSimilar(requiredItem1, offeredItem1);
			ItemStack offeredItem2Final = offeredItem2;
			Lazy<@NonNull Boolean> item2Similar = new Lazy<>(
					() -> ItemUtils.isSimilar(requiredItem2, offeredItem2Final)
			);
			if (!item1Similar || !item2Similar.get()) {
				if (!slientStrictItemComparison) {
					// Feedback message:
					TextUtils.sendMessage(
							tradingPlayer,
							Messages.cannotTradeItemsNotStrictlyMatching
					);

					// Additional debug output:
					if (Debug.isDebugging()) {
						String errorMsg = "The offered items do not strictly match the required items.";
						if (isInTradingContext) {
							this.debugPreventedTrade(tradingPlayer, errorMsg);
						} else {
							Log.debug(shopkeeper.getLogPrefix() + errorMsg);
						}

						Log.debug("Active trading recipe: "
								+ ItemUtils.getSimpleRecipeInfo(tradingRecipe));
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
				this.onTradeAborted(tradingContext, slientStrictItemComparison);
				this.clearResultSlotForInvalidTrade(merchantInventory);
				return null;
			}
		}

		// Create and set up a new Trade:
		Trade trade = new Trade(
				tradingContext,
				tradingContext.getTradeCount(),
				tradingRecipe,
				offeredItem1,
				offeredItem2,
				swappedItemOrder
		);
		this.setupTrade(trade);
		tradingContext.setCurrentTrade(trade);
		return trade;
	}

	private boolean matches(
			@Nullable ItemStack offeredItem1,
			@Nullable ItemStack offeredItem2,
			@Nullable UnmodifiableItemStack requiredItem1,
			@Nullable UnmodifiableItemStack requiredItem2
	) {
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
		Log.debug(() -> this.getShopkeeper().getLogPrefix() + "Prevented trade by "
				+ player.getName() + ": " + reason);
	}

	/**
	 * This is called for every newly created {@link TradingContext} and can be used by sub-classes
	 * to set up additional metdata that is relevant for processing any subsequent trades.
	 * 
	 * @param tradingContext
	 *            the trading context, not <code>null</code>
	 */
	protected void setupTradingContext(TradingContext tradingContext) {
		// Callback for subclasses.
	}

	/**
	 * This is called for every newly created {@link Trade} and can be used by sub-classes to set up
	 * additional metadata that is relevant for processing the trade.
	 * 
	 * @param trade
	 *            the trade, not <code>null</code>
	 */
	protected void setupTrade(Trade trade) {
		// Callback for subclasses.
	}

	// Returns true if the trade was not aborted and is now supposed to get applied.
	private boolean handleTrade(Trade trade) {
		assert trade != null;
		// Check and prepare the trade:
		if (!this.prepareTrade(trade)) {
			// The trade got cancelled for some shopkeeper-specific reason:
			this.onTradeAborted(trade.getTradingContext(), false);
			return false;
		}

		// Call trade event, giving other plugins a chance to cancel the trade before it gets
		// applied:
		// Prepare the offered items for the event: Clone and ensure that the stack sizes match the
		// trading recipe.
		TradingRecipe tradingRecipe = trade.getTradingRecipe();
		ItemStack eventOfferedItem1 = ItemUtils.copyWithAmount(
				trade.getOfferedItem1(),
				tradingRecipe.getItem1().getAmount()
		);
		ItemStack eventOfferedItem2 = ItemUtils.cloneOrNullIfEmpty(trade.getOfferedItem2());
		if (eventOfferedItem2 != null) {
			// Not null: Minecraft disables the trade if there is second offered item but the trade
			// only expects a single item.
			UnmodifiableItemStack recipeItem2 = Unsafe.assertNonNull(tradingRecipe.getItem2());
			eventOfferedItem2.setAmount(recipeItem2.getAmount());
		}

		Shopkeeper shopkeeper = this.getShopkeeper();
		InventoryClickEvent clickEvent = trade.getInventoryClickEvent();
		Player tradingPlayer = trade.getTradingPlayer();
		ShopkeeperTradeEvent tradeEvent = new ShopkeeperTradeEvent(
				shopkeeper,
				tradingPlayer,
				clickEvent,
				tradingRecipe,
				UnmodifiableItemStack.ofNonNull(eventOfferedItem1),
				UnmodifiableItemStack.of(eventOfferedItem2),
				trade.isItemOrderSwapped()
		);
		Bukkit.getPluginManager().callEvent(tradeEvent);
		if (tradeEvent.isCancelled()) {
			Log.debug(() -> shopkeeper.getLogPrefix() + "Some plugin cancelled the trade event of "
					+ "player " + tradingPlayer.getName());
			this.onTradeAborted(trade.getTradingContext(), false);
			return false;
		}

		// Making sure that the click event is still cancelled:
		if (!clickEvent.isCancelled()) {
			Log.warning(shopkeeper.getLogPrefix()
					+ "Some plugin tried to uncancel the inventory click event of the trade event!");
			clickEvent.setCancelled(true);
		}

		// Assert: The click event and the affected inventories should not get modified during the
		// event!

		// We are going to apply the trade now:
		this.preApplyTrade(trade);
		return true;
	}

	private void commonApplyTrade(Trade trade) {
		// Update merchant inventory contents:
		MerchantInventory merchantInventory = trade.getMerchantInventory();
		merchantInventory.setItem(RESULT_ITEM_SLOT_ID, null); // Clear result slot, just in case

		TradingRecipe tradingRecipe = trade.getTradingRecipe();
		ItemStack newOfferedItem1 = ItemUtils.decreaseItemAmount(
				trade.getOfferedItem1(),
				ItemUtils.getItemStackAmount(tradingRecipe.getItem1())
		);
		ItemStack newOfferedItem2 = ItemUtils.decreaseItemAmount(
				trade.getOfferedItem2(),
				ItemUtils.getItemStackAmount(tradingRecipe.getItem2())
		);
		// Inform the merchant inventory about the change (updates the active trading recipe and
		// result item):
		boolean itemOrderSwapped = trade.isItemOrderSwapped();
		merchantInventory.setItem(
				itemOrderSwapped ? BUY_ITEM_2_SLOT_ID : BUY_ITEM_1_SLOT_ID, newOfferedItem1
		);
		merchantInventory.setItem(
				itemOrderSwapped ? BUY_ITEM_1_SLOT_ID : BUY_ITEM_2_SLOT_ID, newOfferedItem2
		);

		// TODO Increase uses of corresponding MerchantRecipe?
		// TODO Add support for exp-rewards?
		// TODO Support modifications to the MerchantRecipe's maxUses?

		Player player = trade.getTradingPlayer();

		// Increment 'traded-with-villager' statistic for every trade:
		if (Settings.incrementVillagerStatistics) {
			player.incrementStatistic(Statistic.TRADED_WITH_VILLAGER);
		}

		// Shopkeeper-specific application of the trade:
		this.onTradeApplied(trade);

		// Play a sound effect if this is the first trade triggered by the inventory click:
		boolean silent = (trade.getTradeNumber() > 1);
		if (!silent) {
			Settings.tradeSucceededSound.play(player);
		}

		// Inform listeners:
		tradingListeners.forEach(listener -> listener.onTradeCompleted(trade, silent));

		// Log trade:
		Log.debug(() -> this.getShopkeeper().getLogPrefix() + "Trade (#" + trade.getTradeNumber()
				+ ") by " + player.getName() + ": " + ItemUtils.getSimpleRecipeInfo(tradingRecipe));
	}

	/**
	 * Checks whether the given trade can take place and makes all the necessary preparations for
	 * the application of the trade, if it is actually carried out.
	 * <p>
	 * This is called for every trade that a player triggers through a merchant inventory action.
	 * Depending on the inventory action, multiple successive trades (even using different trading
	 * recipes) can be triggered by the same inventory action.
	 * <p>
	 * The corresponding {@link InventoryClickEvent} and the involved inventories (player,
	 * container, etc.) are expected to not be modified between this phase of the trade handling and
	 * the actual application of the trade.
	 * 
	 * @param trade
	 *            the trade, not <code>null</code>
	 * @return <code>true</code> to continue with the given trade, or <code>false</code> to cancel
	 *         the given trade and any successive trades that would be triggered by the same
	 *         inventory click
	 */
	protected boolean prepareTrade(Trade trade) {
		return true;
	}

	/**
	 * This is called whenever a trade attempt has been cancelled for some reason.
	 * <p>
	 * This is not called for cancelled {@link InventoryClickEvent}s, or inventory actions that are
	 * ignored because they would not result in a trade in vanilla Minecraft either.
	 * <p>
	 * If available, the corresponding {@link Trade} instance can be retrieved via
	 * {@link TradingContext#getCurrentTrade()}. However, trade attempts can also be aborted before
	 * a corresponding valid {@link Trade} instance could be created.
	 * {@link TradingContext#getCurrentTrade()} will then return <code>null</code>.
	 * {@link TradingContext#getTradeCount()} will always reflect the aborted trade attempt.
	 * <p>
	 * This is also called for trades that were aborted by {@link #prepareTrade(Trade)} and can be
	 * used to perform any necessary cleanup.
	 * <p>
	 * When a trade has been cancelled, no further trades will be processed for the same
	 * {@link TradingContext}.
	 * 
	 * @param tradingContext
	 *            the trading context, not <code>null</code>
	 * @param silent
	 *            <code>true</code> to skip any actions that might be noticeable by players on the
	 *            server
	 */
	protected void onTradeAborted(TradingContext tradingContext, boolean silent) {
		// Inform listeners:
		tradingListeners.forEach(listener -> listener.onTradeAborted(tradingContext, silent));

		// Play a sound effect, but only if this has been the first trade attempt triggered by the
		// inventory click:
		if (!silent && tradingContext.getTradeCount() == 1) {
			Settings.tradeFailedSound.play(tradingContext.getTradingPlayer());
		}
	}

	/**
	 * This is called right before a trade gets applied.
	 * <p>
	 * This can be used to perform any kind of pre-processing which needs to happen first.
	 * <p>
	 * At this phase of the trade handling, the trade should no longer get cancelled. Any conditions
	 * which could prevent a trade from getting successfully applied have to be checked inside
	 * {@link #prepareTrade(Trade)} instead.
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
	 * This can be used to perform any kind of post-processing which needs to happen last. For
	 * example any shopkeeper specific behavior required for applying the trade can happen here.
	 * <p>
	 * At this phase of the trade handling, the trade should no longer get cancelled. Any conditions
	 * which could prevent a trade from getting successfully applied have to be checked inside
	 * {@link #prepareTrade(Trade)} instead.
	 * 
	 * @param trade
	 *            the trade
	 */
	protected void onTradeApplied(Trade trade) {
		// Callback for subclasses.
	}

	// Returns a value >= 0 and <= amount.
	// Note: Depending on the configuration, the amount can end up 0.
	protected int getAmountAfterTaxes(int amount) {
		assert amount >= 0;
		if (Settings.taxRate == 0) return amount;

		int taxes;
		if (Settings.taxRoundUp) {
			taxes = (int) Math.ceil(amount * (Settings.taxRate / 100.0D));
		} else {
			taxes = (int) Math.floor(amount * (Settings.taxRate / 100.0D));
		}
		return Math.max(0, Math.min(amount - taxes, amount));
	}

	private static void debugLogItemStack(
			String itemStackName,
			@Nullable UnmodifiableItemStack itemStack
	) {
		debugLogItemStack(itemStackName, ItemUtils.asItemStackOrNull(itemStack));
	}

	private static void debugLogItemStack(String itemStackName, @Nullable ItemStack itemStack) {
		Object itemStackData = (itemStack != null) ? itemStack : "<empty>";
		Log.debug(ConfigUtils.toConfigYamlWithoutTrailingNewline(itemStackName, itemStackData));
	}
}
