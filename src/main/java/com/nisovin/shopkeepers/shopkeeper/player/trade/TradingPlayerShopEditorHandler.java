package com.nisovin.shopkeepers.shopkeeper.player.trade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.shopkeeper.offers.SKTradeOffer;
import com.nisovin.shopkeepers.shopkeeper.player.PlayerShopEditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class TradingPlayerShopEditorHandler extends PlayerShopEditorHandler {

	protected TradingPlayerShopEditorHandler(SKTradingPlayerShopkeeper shopkeeper) {
		super(shopkeeper);
	}

	@Override
	public SKTradingPlayerShopkeeper getShopkeeper() {
		return (SKTradingPlayerShopkeeper) super.getShopkeeper();
	}

	@Override
	protected List<TradingRecipeDraft> getTradingRecipes() {
		SKTradingPlayerShopkeeper shopkeeper = this.getShopkeeper();

		// Add the shopkeeper's offers:
		List<SKTradeOffer> offers = shopkeeper.getOffers();
		List<TradingRecipeDraft> recipes = new ArrayList<>(offers.size() + 8); // Heuristic initial capacity
		offers.forEach(offer -> {
			// The offer returns copies of its items:
			TradingRecipeDraft recipe = new TradingRecipeDraft(offer.getResultItem(), offer.getItem1(), offer.getItem2());
			recipes.add(recipe);
		});

		// Add new empty recipe drafts for items from the container without existing offer:
		// We only add one recipe per similar item:
		List<ItemStack> newRecipes = new ArrayList<>();
		ItemStack[] containerContents = shopkeeper.getContainerContents(); // Empty if the container is not found
		for (ItemStack containerItem : containerContents) {
			if (ItemUtils.isEmpty(containerItem)) continue; // Ignore empty ItemStacks

			if (shopkeeper.hasOffer(containerItem)) {
				// There is already a recipe for this item:
				continue;
			}

			if (ItemUtils.contains(newRecipes, containerItem)) {
				// We already added a new recipe for this item:
				continue;
			}

			// Add new empty recipe:
			containerItem = ItemUtils.copySingleItem(containerItem); // Ensures a stack size of 1
			TradingRecipeDraft recipe = new TradingRecipeDraft(containerItem, null, null);
			recipes.add(recipe);
			newRecipes.add(containerItem);
		}

		return recipes;
	}

	@Override
	protected void clearRecipes() {
		SKTradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.clearOffers();
	}

	@Override
	protected void addRecipe(TradingRecipeDraft recipe) {
		assert recipe != null && recipe.isValid();
		SKTradingPlayerShopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.addOffer(ShopkeepersAPI.createTradeOffer(recipe.getResultItem(), recipe.getItem1(), recipe.getItem2()));
	}

	@Override
	protected void handlePlayerInventoryClick(Session session, InventoryClickEvent event) {
		// Assert: Event cancelled.
		// Clicking in player inventory:
		if (event.isShiftClick()) return; // Ignoring shift clicks

		ItemStack cursor = event.getCursor();
		ItemStack current = event.getCurrentItem();
		if (!ItemUtils.isEmpty(cursor)) {
			if (ItemUtils.isEmpty(current)) {
				// Place item from cursor:
				event.setCurrentItem(cursor);
				event.getView().setCursor(null); // Requires the event to be cancelled
			}
		} else if (!ItemUtils.isEmpty(current)) {
			// Pick up item to cursor:
			event.setCurrentItem(null);
			event.getView().setCursor(current); // Requires the event to be cancelled
		}
	}

	@Override
	protected void handleTradesClick(Session session, InventoryClickEvent event) {
		int rawSlot = event.getRawSlot();
		assert this.isTradesArea(rawSlot);
		assert this.isResultRow(rawSlot) || this.isItem1Row(rawSlot) || this.isItem2Row(rawSlot);

		ItemStack cursor = event.getCursor();
		if (!ItemUtils.isEmpty(cursor)) {
			// Place item from cursor:
			Inventory inventory = event.getInventory();
			ItemStack cursorClone = cursor.clone();
			cursorClone.setAmount(1);
			Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
				inventory.setItem(rawSlot, cursorClone);
			});
		} else {
			// Changing stack size of clicked item:
			boolean resultRow = this.isResultRow(rawSlot);
			this.handleUpdateItemAmountOnClick(event, resultRow ? 1 : 0);
		}
	}

	@Override
	protected void onInventoryDragEarly(InventoryDragEvent event, Player player) {
		event.setCancelled(true);
		ItemStack cursor = event.getOldCursor();
		// Assert: Cursor item is already a clone.
		if (ItemUtils.isEmpty(cursor)) return;

		Set<Integer> rawSlots = event.getRawSlots();
		if (rawSlots.size() != 1) return;

		int rawSlot = rawSlots.iterator().next();
		if (this.isResultRow(rawSlot) || this.isItem1Row(rawSlot) || this.isItem2Row(rawSlot)) {
			// Place item from cursor:
			Inventory inventory = event.getInventory();
			cursor.setAmount(1);
			Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
				inventory.setItem(rawSlot, cursor);
			});
		} else {
			InventoryView view = event.getView();
			if (this.isPlayerInventory(view, view.getSlotType(rawSlot), rawSlot)) {
				// Clicking in player inventory:
				// The cancelled drag event resets the cursor afterwards, so we need this delay:
				Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
					// Freshly get and check cursor to make sure that players don't abuse this delay:
					ItemStack cursorCurrent = view.getCursor();
					if (ItemUtils.isEmpty(cursorCurrent)) return;
					ItemStack current = view.getItem(rawSlot);
					if (ItemUtils.isEmpty(current)) {
						// Place item from cursor:
						view.setItem(rawSlot, cursorCurrent);
						view.setCursor(null);
					}
				});
			}
		}
	}
}
