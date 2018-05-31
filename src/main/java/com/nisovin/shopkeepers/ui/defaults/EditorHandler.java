package com.nisovin.shopkeepers.ui.defaults;

import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.Shopkeeper;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.events.ShopkeeperDeletedEvent;
import com.nisovin.shopkeepers.events.ShopkeeperEditedEvent;
import com.nisovin.shopkeepers.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.shoptypes.PlayerShopType;
import com.nisovin.shopkeepers.ui.UIHandler;
import com.nisovin.shopkeepers.ui.UIType;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

public abstract class EditorHandler extends UIHandler {

	// column = [0,7] (last column of the inventory is used for editor buttons)
	protected static final int TRADE_COLUMNS = 8;

	protected EditorHandler(UIType uiType, AbstractShopkeeper shopkeeper) {
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
		this.getShopkeeper().closeAllOpenWindows();
		SKShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
	}

	@Override
	protected void onInventoryClick(InventoryClickEvent event, Player player) {
		assert event != null && player != null;
		AbstractShopkeeper shopkeeper = this.getShopkeeper();

		// check for special action buttons:
		int rawSlot = event.getRawSlot();
		if (rawSlot == 26) {
			// delete button - delete shopkeeper:
			event.setCancelled(true);

			// return shop creation item for player shopkeepers:
			if (Settings.deletingPlayerShopReturnsCreationItem && shopkeeper.getType() instanceof PlayerShopType) {
				ItemStack shopCreationItem = Settings.createShopCreationItem();
				Map<Integer, ItemStack> remaining = player.getInventory().addItem(shopCreationItem);
				if (!remaining.isEmpty()) {
					player.getWorld().dropItem(shopkeeper.getActualLocation(), shopCreationItem);
				}
			}

			// delete shopkeeper:
			// this also deactivates the ui and closes all open windows for this shopkeeper after a delay
			shopkeeper.delete();

			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperDeletedEvent(player, shopkeeper));

			// save:
			SKShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
		} else if (rawSlot == 17) {
			// cycle button - cycle to next object type variation:
			event.setCancelled(true);

			ItemStack cursor = event.getCursor();
			if (!ItemUtils.isEmpty(cursor)) {
				// equip item:
				shopkeeper.getShopObject().setItem(cursor.clone());
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
						event.getInventory().setItem(17, ItemUtils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
					}
				}
			}

			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, shopkeeper));

			// save:
			SKShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
		} else if (rawSlot == 8) {
			// naming or chest inventory button:
			event.setCancelled(true);

			// renaming is disabled for citizens player shops:
			if (!Settings.enableChestOptionOnPlayerShop
					&& !Settings.allowRenamingOfPlayerNpcShops
					&& shopkeeper.getType() instanceof PlayerShopType
					&& shopkeeper.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN()) {
				return;
				// TODO restructure this all, to allow for dynamic editor buttons depending on shop (object) types and
				// settings
			}

			// prepare closing the editor window:
			this.saveEditor(event.getInventory(), player);

			// run event:
			Bukkit.getPluginManager().callEvent(new ShopkeeperEditedEvent(player, shopkeeper));

			// save:
			SKShopkeepersPlugin.getInstance().getShopkeeperStorage().save();

			// ignore other click events for this shopkeeper in the same tick:
			shopkeeper.deactivateUI();

			// determine resulting action depending on the clicked item type:
			ItemStack clickedItem = event.getCurrentItem();
			boolean renaming = (!ItemUtils.isEmpty(clickedItem) && clickedItem.getType() == Settings.nameItem);
			boolean openChest = (!renaming && !ItemUtils.isEmpty(clickedItem) && clickedItem.getType() == Settings.chestItem);

			// close editor window delayed, and optionally open chest inventory afterwards:
			Bukkit.getScheduler().runTask(SKShopkeepersPlugin.getInstance(), () -> {
				informOnClose(player);
				player.closeInventory();

				// reactivate ui for this shopkeeper:
				shopkeeper.activateUI();

				// open chest inventory:
				if (openChest) {
					shopkeeper.openChestWindow(player);
				}
			});

			if (renaming) {
				// start naming:
				shopkeeper.startNaming(player);
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
			inventory.setItem(8, Settings.createChestButtonItem());
		} else {
			// naming button:
			boolean useNamingButton = false;
			if (!(shopkeeper.getType() instanceof PlayerShopType)) {
				useNamingButton = true;
			} else {
				// naming via button enabled?
				if (!Settings.namingOfPlayerShopsViaItem) {
					// no naming button for citizens player shops if renaming is disabled for those
					if (Settings.allowRenamingOfPlayerNpcShops || shopkeeper.getShopObject().getObjectType() != DefaultShopObjectTypes.CITIZEN()) {
						useNamingButton = true;
					}
				}
			}

			if (useNamingButton) {
				inventory.setItem(8, Settings.createNameButtonItem());
			}
		}

		// sub-type cycle button:
		ItemStack typeItem = shopkeeper.getShopObject().getSubTypeItem();
		if (typeItem != null) {
			inventory.setItem(17, ItemUtils.setItemStackNameAndLore(typeItem, Settings.msgButtonType, Settings.msgButtonTypeLore));
		}

		// delete button:
		inventory.setItem(26, Settings.createDeleteButtonItem());
	}
}
