package com.nisovin.shopkeepers.ui.defaults;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerDeleteShopkeeperEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopType;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.TradingRecipeDraft;
import com.nisovin.shopkeepers.ui.AbstractUIType;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public abstract class EditorHandler extends UIHandler {

	protected static final int INVENTORY_COLUMNS = 9;
	// 8 columns, column = [0,7] (last column of the inventory is used for editor buttons)
	protected static final int TRADE_COLUMNS = 8;

	protected static final int TRADE_ROW_1_START = 0;
	protected static final int TRADE_ROW_1_END = TRADE_ROW_1_START + TRADE_COLUMNS - 1;
	protected static final int TRADE_ROW_2_START = 9;
	protected static final int TRADE_ROW_2_END = TRADE_ROW_2_START + TRADE_COLUMNS - 1;
	protected static final int TRADE_ROW_3_START = 18;
	protected static final int TRADE_ROW_3_END = TRADE_ROW_3_START + TRADE_COLUMNS - 1;

	// slot = column + offset:
	protected static final int RESULT_ITEM_OFFSET = TRADE_ROW_1_START;
	protected static final int ITEM_1_OFFSET = TRADE_ROW_3_START;
	protected static final int ITEM_2_OFFSET = TRADE_ROW_2_START;

	protected static final int BUTTON_NAMING = 8;
	protected static final int BUTTON_CHEST = 8;
	protected static final int BUTTON_OBJECT_VARIANT = 17;
	protected static final int BUTTON_DELETE = 26;

	protected abstract class CommonEditorSetup<S extends AbstractShopkeeper, O> {

		protected final S shopkeeper;

		public CommonEditorSetup(S shopkeeper) {
			this.shopkeeper = shopkeeper;
		}

		public boolean openWindow(Player player) {
			// create inventory:
			Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

			// setup trade columns:
			this.setupTradeColumns(inventory, player);

			// add the special buttons:
			setActionButtons(inventory);

			// show editing inventory:
			player.openInventory(inventory);

			return true;
		}

		protected void setupTradeColumns(Inventory inventory, Player player) {
			// TODO allow setup similar to trading shopkeeper?
			// add the shopkeeper's offers:
			int column = 0;
			List<O> offers = this.getOffers();
			if (offers == null) offers = Collections.emptyList();
			for (; column < offers.size() && column < TRADE_COLUMNS; column++) {
				O offer = offers.get(column);

				// create recipe:
				TradingRecipeDraft recipe;
				if (offer == null) {
					recipe = null;
				} else {
					recipe = this.toTradingRecipe(offer);
				}
				if (recipe == null) {
					column--;
					continue;
				}

				// add recipe to inventory:
				this.setTradeColumn(inventory, column, recipe);
			}

			if (column < TRADE_COLUMNS) {
				// add empty offers for items from the chest:
				List<ItemCount> chestItems = this.getItemsFromChest();
				int chestItemIndex = 0;
				for (; chestItemIndex < chestItems.size() && column < TRADE_COLUMNS; column++, chestItemIndex++) {
					ItemCount itemCount = chestItems.get(chestItemIndex);
					ItemStack itemFromChest = itemCount.getItem(); // this item is already a copy with amount 1

					if (this.hasOffer(itemFromChest)) {
						column--;
						continue; // already added
					}

					// create recipe:
					TradingRecipeDraft recipe = this.toTradingRecipe(itemFromChest);
					if (recipe == null) {
						column--;
						continue;
					}

					// add recipe to inventory:
					this.setTradeColumn(inventory, column, recipe);
				}
			}
		}

		protected abstract List<O> getOffers();

		protected abstract List<ItemCount> getItemsFromChest();

		protected abstract boolean hasOffer(ItemStack itemFromChest);

		protected abstract TradingRecipeDraft toTradingRecipe(O offer);

		protected abstract TradingRecipeDraft toTradingRecipe(ItemStack itemFromChest);

		protected void setTradeColumn(Inventory inventory, int column, TradingRecipeDraft recipe) {
			EditorHandler.this.setTradeColumn(inventory, column, recipe.getResultItem(), recipe.getItem1(), recipe.getItem2());
		}

		public void saveEditor(Inventory inventory, Player player) {
			this.clearOffers();
			for (int column = 0; column < TRADE_COLUMNS; column++) {
				TradingRecipeDraft recipeDraft = EditorHandler.this.getTradingRecipe(inventory, column);
				if (!recipeDraft.isValid()) {
					this.handleInvalidRecipeDraft(player, recipeDraft);
					continue;
				}
				this.addOffer(player, recipeDraft);
			}
		}

		// called for every recipe draft that is not valid:
		protected void handleInvalidRecipeDraft(Player player, TradingRecipeDraft recipe) {
		}

		protected abstract void clearOffers();

		protected abstract void addOffer(Player player, TradingRecipeDraft recipe);
	}

	protected EditorHandler(AbstractUIType uiType, AbstractShopkeeper shopkeeper) {
		super(uiType, shopkeeper);
	}

	@Override
	protected boolean canOpen(Player player) {
		assert player != null;
		// permission for the type of shopkeeper is checked in the AdminShopkeeper specific EditorHandler
		// owner is checked in the PlayerShopkeeper specific EditorHandler
		return true;
	}

	@Override
	public boolean isWindow(Inventory inventory) {
		return inventory != null && inventory.getTitle().equals(Settings.editorTitle);
	}

	@Override
	protected void onInventoryClose(InventoryCloseEvent event, Player player) {
		this.saveEditor(event.getInventory(), player);
		Shopkeeper shopkeeper = this.getShopkeeper();
		shopkeeper.closeAllOpenWindows();
		shopkeeper.save();
	}

	@Override
	protected void onInventoryDrag(InventoryDragEvent event, Player player) {
		// allowed by default
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		assert event != null && player != null;
		AbstractShopkeeper shopkeeper = this.getShopkeeper();

		// check for special action buttons:
		int rawSlot = event.getRawSlot();
		if (rawSlot == BUTTON_DELETE) {
			// delete button - delete shopkeeper:
			event.setCancelled(true);

			// call event:
			PlayerDeleteShopkeeperEvent deleteEvent = new PlayerDeleteShopkeeperEvent(shopkeeper, player);
			Bukkit.getPluginManager().callEvent(deleteEvent);
			if (deleteEvent.isCancelled()) {
				Log.debug("ShopkeeperDeleteEvent was cancelled!");
			} else {
				// return shop creation item for player shopkeepers:
				if (Settings.deletingPlayerShopReturnsCreationItem && shopkeeper.getType() instanceof PlayerShopType) {
					ItemStack shopCreationItem = Settings.createShopCreationItem();
					Map<Integer, ItemStack> remaining = player.getInventory().addItem(shopCreationItem);
					if (!remaining.isEmpty()) {
						player.getWorld().dropItem(shopkeeper.getObjectLocation(), shopCreationItem);
					}
				}

				// delete shopkeeper:
				// this also deactivates the ui and closes all open windows for this shopkeeper after a delay
				shopkeeper.delete();

				// save:
				shopkeeper.save();
			}
		} else if (rawSlot == BUTTON_OBJECT_VARIANT) {
			// cycle button - cycle to next object type variation:
			event.setCancelled(true);

			ItemStack cursor = event.getCursor();
			if (!ItemUtils.isEmpty(cursor)) {
				// equip item:
				shopkeeper.getShopObject().equipItem(cursor.clone());
				// TODO how to remove equipped item again?
				// TODO equipped items don't get saved current -> they get lost when the entity is respawned
				// TODO not possible for player shops currently, because clicking/picking up items in player inventory
				// is blocked
			} else {
				// cycle object type variant:
				if (event.getClick() != ClickType.DOUBLE_CLICK) { // ignore double clicks
					shopkeeper.getShopObject().cycleSubType();
					ItemStack typeItem = shopkeeper.getShopObject().getSubTypeItem();
					if (!ItemUtils.isEmpty(typeItem)) {
						event.getInventory().setItem(BUTTON_OBJECT_VARIANT, ItemUtils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
					}
				}
			}

			// call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// save:
			shopkeeper.save();
		} else if (rawSlot == BUTTON_NAMING) {
			// naming or chest inventory button:
			event.setCancelled(true);

			// renaming is disabled for citizens player shops:
			if (!Settings.enableChestOptionOnPlayerShop
					&& !Settings.allowRenamingOfPlayerNpcShops
					&& shopkeeper.getType() instanceof PlayerShopType
					&& shopkeeper.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
				return;
				// TODO restructure this all, to allow for dynamic editor buttons depending on shop (object) types and
				// settings
			}

			// prepare closing the editor window:
			this.saveEditor(event.getInventory(), player);

			// call event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(shopkeeper, player));

			// save:
			shopkeeper.save();

			// ignore other click events for this shopkeeper in the same tick:
			shopkeeper.deactivateUI();

			// determine resulting action depending on the clicked item type:
			ItemStack clickedItem = event.getCurrentItem();
			boolean renaming = (!ItemUtils.isEmpty(clickedItem) && clickedItem.getType() == Settings.nameItem);
			boolean openChest = (!renaming && !ItemUtils.isEmpty(clickedItem) && clickedItem.getType() == Settings.chestItem);

			// close editor window delayed, and optionally open chest inventory afterwards:
			Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
				player.closeInventory();

				// reactivate ui for this shopkeeper:
				shopkeeper.activateUI();

				// open chest inventory:
				if (openChest && shopkeeper instanceof PlayerShopkeeper) {
					((PlayerShopkeeper) shopkeeper).openChestWindow(player);
				}
			});

			if (renaming) {
				// start naming:
				SKShopkeepersPlugin.getInstance().getShopkeeperNaming().startNaming(player, shopkeeper);
				Utils.sendMessage(player, Settings.msgTypeNewName);
			}
		}
	}

	/**
	 * Saves the current state of the editor interface.
	 * 
	 * @param inventory
	 *            the inventory of the editor window
	 * @param player
	 *            the editing player
	 */
	protected abstract void saveEditor(Inventory inventory, Player player);

	protected boolean isResultRow(int rawSlot) {
		return rawSlot >= TRADE_ROW_1_START && rawSlot <= TRADE_ROW_1_END;
	}

	protected boolean isItem1Row(int rawSlot) {
		return rawSlot >= TRADE_ROW_3_START && rawSlot <= TRADE_ROW_3_END;
	}

	protected boolean isItem2Row(int rawSlot) {
		return rawSlot >= TRADE_ROW_2_START && rawSlot <= TRADE_ROW_2_END;
	}

	protected boolean isTradesArea(int rawSlot) {
		return this.isResultRow(rawSlot) || this.isItem1Row(rawSlot) || this.isItem2Row(rawSlot);
	}

	protected boolean isPlayerInventory(InventoryView view, SlotType slotType, int rawSlot) {
		return rawSlot >= view.getTopInventory().getSize() && (slotType == SlotType.CONTAINER || slotType == SlotType.QUICKBAR);
	}

	protected TradingRecipeDraft getTradingRecipe(Inventory inventory, int column) {
		ItemStack resultItem = inventory.getItem(column + RESULT_ITEM_OFFSET);
		ItemStack item1 = inventory.getItem(column + ITEM_1_OFFSET);
		ItemStack item2 = inventory.getItem(column + ITEM_2_OFFSET);
		return new TradingRecipeDraft(resultItem, item1, item2);
	}

	protected void setTradeColumns(Inventory inventory, List<TradingRecipeDraft> recipes) {
		if (inventory == null || recipes == null) return;
		for (int column = 0; column < recipes.size() && column < TRADE_COLUMNS; column++) {
			TradingRecipeDraft recipe = recipes.get(column);
			if (recipe == null) {
				column--;
				continue;
			}
			this.setTradeColumn(inventory, column, recipe.getResultItem(), recipe.getItem1(), recipe.getItem2());
		}
	}

	protected void setTradeColumn(Inventory inventory, int column, ItemStack resultItem, ItemStack item1, ItemStack item2) {
		if (inventory == null) return;
		inventory.setItem(column + RESULT_ITEM_OFFSET, resultItem);
		inventory.setItem(column + ITEM_1_OFFSET, item1);
		inventory.setItem(column + ITEM_2_OFFSET, item2);
	}

	protected int getNewAmountAfterEditorClick(InventoryClickEvent event, int currentAmount, int minAmount, int maxAmount) {
		// validate bounds:
		if (minAmount > maxAmount) return currentAmount; // no valid value possible
		if (minAmount == maxAmount) return minAmount; // only one valid value possible

		int newAmount = currentAmount;
		ClickType clickType = event.getClick();
		switch (clickType) {
		case LEFT:
			newAmount += 1;
			break;
		case SHIFT_LEFT:
			newAmount += 10;
			break;
		case RIGHT:
			newAmount -= 1;
			break;
		case SHIFT_RIGHT:
			newAmount -= 10;
			break;
		case MIDDLE:
			newAmount = minAmount;
			break;
		case NUMBER_KEY:
			assert event.getHotbarButton() >= 0;
			newAmount = event.getHotbarButton() + 1;
			break;
		default:
			break;
		}
		// bounds:
		if (newAmount < minAmount) newAmount = minAmount;
		if (newAmount > maxAmount) newAmount = maxAmount;
		return newAmount;
	}

	protected void setActionButtons(Inventory inventory) {
		final Shopkeeper shopkeeper = this.getShopkeeper();
		// TODO restructure this to allow button types to be registered and unregistered (instead of this condition
		// check here)

		if (Settings.enableChestOptionOnPlayerShop && shopkeeper.getType() instanceof PlayerShopType) {
			// chest button:
			inventory.setItem(BUTTON_CHEST, Settings.createChestButtonItem());
		} else {
			// naming button:
			boolean useNamingButton = false;
			if (!(shopkeeper.getType() instanceof PlayerShopType)) {
				useNamingButton = true;
			} else {
				// naming via button enabled?
				if (!Settings.namingOfPlayerShopsViaItem) {
					// no naming button for citizens player shops if renaming is disabled for those
					if (Settings.allowRenamingOfPlayerNpcShops || shopkeeper.getShopObject().getType() != DefaultShopObjectTypes.CITIZEN()) {
						useNamingButton = true;
					}
				}
			}

			if (useNamingButton) {
				inventory.setItem(BUTTON_NAMING, Settings.createNameButtonItem());
			}
		}

		// sub-type cycle button:
		ItemStack typeItem = shopkeeper.getShopObject().getSubTypeItem();
		if (typeItem != null) {
			inventory.setItem(BUTTON_OBJECT_VARIANT, ItemUtils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
		}

		// delete button:
		inventory.setItem(BUTTON_DELETE, Settings.createDeleteButtonItem());
	}
}
