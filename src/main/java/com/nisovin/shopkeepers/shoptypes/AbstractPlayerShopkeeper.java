package com.nisovin.shopkeepers.shoptypes;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.ShopCreationData;
import com.nisovin.shopkeepers.api.ShopCreationData.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerShopkeeperHiredEvent;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.shoptypes.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.util.TradingRecipe;
import com.nisovin.shopkeepers.shopobjects.CitizensShop;
import com.nisovin.shopkeepers.shopobjects.SignShop;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.HiringHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.SKTradingRecipe;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractPlayerShopkeeper extends AbstractShopkeeper implements PlayerShopkeeper {

	protected static abstract class PlayerShopEditorHandler extends EditorHandler {

		// slot = column + offset:
		protected static final int HIGH_COST_OFFSET = 9;
		protected static final int LOW_COST_OFFSET = 18;

		protected PlayerShopEditorHandler(AbstractPlayerShopkeeper shopkeeper) {
			super(SKDefaultUITypes.EDITOR(), shopkeeper);
		}

		@Override
		public AbstractPlayerShopkeeper getShopkeeper() {
			return (AbstractPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			return super.canOpen(player) && (this.getShopkeeper().isOwner(player) || Utils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION));
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			// cancel all inventory clicks and handle everything on our own:
			// TODO maybe allow certain inventory actions which only affect the player's inventory?
			// (like moving items around)
			event.setCancelled(true);

			int rawSlot = event.getRawSlot();
			if (rawSlot >= LOW_COST_OFFSET && rawSlot < (LOW_COST_OFFSET + TRADE_COLUMNS)) {
				// change low cost:
				int column = rawSlot - LOW_COST_OFFSET;
				ItemStack soldItem = event.getInventory().getItem(column);
				if (ItemUtils.isEmpty(soldItem)) return;
				this.handleUpdateTradeCostItemOnClick(event, Settings.createCurrencyItem(1), Settings.createZeroCurrencyItem());
			} else if (rawSlot >= HIGH_COST_OFFSET && rawSlot < ((HIGH_COST_OFFSET + TRADE_COLUMNS))) {
				// change high cost:
				int column = rawSlot - HIGH_COST_OFFSET;
				ItemStack soldItem = event.getInventory().getItem(column);
				if (ItemUtils.isEmpty(soldItem)) return;
				this.handleUpdateTradeCostItemOnClick(event, Settings.createHighCurrencyItem(1), Settings.createHighZeroCurrencyItem());
			} else {
				// handle common editor buttons:
				super.onInventoryClick(event, player);
			}
		}

		protected void handleUpdateItemAmountOnClick(InventoryClickEvent event, int minAmount) {
			assert event.isCancelled();
			// ignore in certain situations:
			ItemStack clickedItem = event.getCurrentItem();
			if (ItemUtils.isEmpty(clickedItem)) return;

			// get new item amount:
			int currentItemAmount = clickedItem.getAmount();
			if (minAmount <= 0) minAmount = 0;
			int newItemAmount = this.getNewAmountAfterEditorClick(event, currentItemAmount, minAmount, clickedItem.getMaxStackSize());
			assert newItemAmount >= minAmount;
			assert newItemAmount <= clickedItem.getMaxStackSize();

			// update item in inventory:
			if (newItemAmount == 0) {
				// empty item slot:
				event.setCurrentItem(null);
			} else {
				clickedItem.setAmount(newItemAmount);
			}
		}

		protected void handleUpdateTradeCostItemOnClick(InventoryClickEvent event, ItemStack currencyItem, ItemStack zeroCurrencyItem) {
			assert event.isCancelled();
			// ignore in certain situations:
			if (ItemUtils.isEmpty(currencyItem)) return;

			// get new item amount:
			ItemStack clickedItem = event.getCurrentItem(); // can be null
			int currentItemAmount = 0;
			boolean isCurrencyItem = ItemUtils.isSimilar(clickedItem, currencyItem);
			if (isCurrencyItem) {
				assert clickedItem != null;
				currentItemAmount = clickedItem.getAmount();
			}
			int newItemAmount = this.getNewAmountAfterEditorClick(event, currentItemAmount, 0, currencyItem.getMaxStackSize());
			assert newItemAmount >= 0;
			assert newItemAmount <= currencyItem.getMaxStackSize();

			// update item in inventory:
			if (newItemAmount == 0) {
				// place zero-currency item:
				event.setCurrentItem(zeroCurrencyItem);
			} else {
				if (isCurrencyItem) {
					// only update item amount of already existing currency item:
					clickedItem.setAmount(newItemAmount);
				} else {
					// place currency item with new amount:
					currencyItem.setAmount(newItemAmount);
					event.setCurrentItem(currencyItem);
				}
			}
		}

		// note: in case the cost is too large to represent, it sets the cost to zero and prints a warning
		// (so opening and closing the editor window will remove the offer, instead of setting the costs to a lower
		// value than what was previously somehow specified)
		protected void setEditColumnCost(Inventory inventory, int column, int cost) {
			assert inventory != null && column >= 0 && column <= TRADE_COLUMNS;
			ItemStack highCostItem = null;
			ItemStack lowCostItem = null;

			int remainingCost = cost;
			if (Settings.isHighCurrencyEnabled()) {
				int highCost = 0;
				if (remainingCost > Settings.highCurrencyMinCost) {
					highCost = Math.min((remainingCost / Settings.highCurrencyValue), Settings.highCurrencyItem.getMaxStackSize());
				}
				if (highCost > 0) {
					remainingCost -= (highCost * Settings.highCurrencyValue);
					highCostItem = Settings.createHighCurrencyItem(highCost);
				} else {
					highCostItem = Settings.createHighZeroCurrencyItem();
				}
			}
			if (remainingCost > 0) {
				if (remainingCost <= Settings.currencyItem.getMaxStackSize()) {
					lowCostItem = Settings.createCurrencyItem(remainingCost);
				} else {
					// cost is to large to represent: reset cost to zero:
					lowCostItem = Settings.createZeroCurrencyItem();
					if (Settings.isHighCurrencyEnabled()) {
						highCostItem = Settings.createHighZeroCurrencyItem();
					}
				}
			} else {
				lowCostItem = Settings.createZeroCurrencyItem();
			}

			// apply to inventory:
			inventory.setItem(column + HIGH_COST_OFFSET, highCostItem);
			inventory.setItem(column + LOW_COST_OFFSET, lowCostItem);
		}

		protected int getPriceFromColumn(Inventory inventory, int column) {
			ItemStack lowCostItem = inventory.getItem(column + 18);
			ItemStack highCostItem = inventory.getItem(column + 9);
			int cost = 0;
			if (lowCostItem != null && lowCostItem.getType() == Settings.currencyItem && lowCostItem.getAmount() > 0) {
				cost += lowCostItem.getAmount();
			}
			if (Settings.isHighCurrencyEnabled() && highCostItem != null && highCostItem.getType() == Settings.highCurrencyItem && highCostItem.getAmount() > 0) {
				cost += (highCostItem.getAmount() * Settings.highCurrencyValue);
			}
			return cost;
		}
	}

	protected static abstract class PlayerShopTradingHandler extends TradingHandler {

		// state related to the currently handled trade:
		protected Inventory chestInventory = null;
		protected ItemStack[] newChestContents = null;

		protected PlayerShopTradingHandler(AbstractPlayerShopkeeper shopkeeper) {
			super(SKDefaultUITypes.TRADING(), shopkeeper);
		}

		@Override
		public AbstractPlayerShopkeeper getShopkeeper() {
			return (AbstractPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean canOpen(Player player) {
			if (!super.canOpen(player)) return false;
			PlayerShopkeeper shopkeeper = this.getShopkeeper();

			// stop opening if trading shall be prevented while the owner is offline:
			if (Settings.preventTradingWhileOwnerIsOnline && !Utils.hasPermission(player, ShopkeepersPlugin.BYPASS_PERMISSION)) {
				Player ownerPlayer = shopkeeper.getOwner();
				if (ownerPlayer != null) {
					Log.debug("Blocked trade window opening from " + player.getName() + " because the owner is online");
					Utils.sendMessage(player, Settings.msgCantTradeWhileOwnerOnline, "{owner}", ownerPlayer.getName());
					return false;
				}
			}
			return true;
		}

		@Override
		protected boolean prepareTrade(TradeData tradeData) {
			if (!super.prepareTrade(tradeData)) return false;
			PlayerShopkeeper shopkeeper = this.getShopkeeper();
			Player tradingPlayer = tradeData.tradingPlayer;

			// no trading with own shop:
			if (Settings.preventTradingWithOwnShop && shopkeeper.isOwner(tradingPlayer) && !tradingPlayer.isOp()) {
				this.debugPreventedTrade(tradingPlayer, "Trading with the own shop is not allowed.");
				return false;
			}

			// no trading while shop owner is online:
			if (Settings.preventTradingWhileOwnerIsOnline && !Utils.hasPermission(tradingPlayer, ShopkeepersPlugin.BYPASS_PERMISSION)) {
				Player ownerPlayer = shopkeeper.getOwner();
				if (ownerPlayer != null && !shopkeeper.isOwner(tradingPlayer)) {
					Utils.sendMessage(tradingPlayer, Settings.msgCantTradeWhileOwnerOnline, "{owner}", ownerPlayer.getName());
					this.debugPreventedTrade(tradingPlayer, "Trading is not allowed while the shop owner is online.");
					return false;
				}
			}

			// check for the shop's chest:
			Block chest = shopkeeper.getChest();
			if (!ItemUtils.isChest(chest.getType())) {
				this.debugPreventedTrade(tradingPlayer, "Couldn't find the shop's chest.");
				return false;
			}

			// setup common state information for handling this trade:
			this.chestInventory = ((Chest) chest.getState()).getInventory();
			this.newChestContents = chestInventory.getContents();

			return true;
		}

		@Override
		protected void onTradeApplied(TradeData tradeData) {
			super.onTradeApplied(tradeData);

			// apply chest content changes:
			if (chestInventory != null && newChestContents != null) {
				chestInventory.setContents(newChestContents);
			}

			// reset trade related state information:
			this.resetTradeState();
		}

		@Override
		protected void onTradeAborted(TradeData tradeData) {
			super.onTradeAborted(tradeData);
			this.resetTradeState();
		}

		protected void resetTradeState() {
			chestInventory = null;
			newChestContents = null;
		}
	}

	protected static class PlayerShopHiringHandler extends HiringHandler {

		protected PlayerShopHiringHandler(AbstractPlayerShopkeeper shopkeeper) {
			super(SKDefaultUITypes.HIRING(), shopkeeper);
		}

		@Override
		public AbstractPlayerShopkeeper getShopkeeper() {
			return (AbstractPlayerShopkeeper) super.getShopkeeper();
		}

		@Override
		protected boolean openWindow(Player player) {
			PlayerShopkeeper shopkeeper = this.getShopkeeper();
			Inventory inventory = Bukkit.createInventory(player, 9, Settings.forHireTitle);

			ItemStack hireItem = Settings.createHireButtonItem();
			inventory.setItem(2, hireItem);
			inventory.setItem(6, hireItem);

			ItemStack hireCost = shopkeeper.getHireCost();
			if (hireCost == null) return false;
			inventory.setItem(4, hireCost);

			player.openInventory(inventory);
			return true;
		}

		@Override
		protected void onInventoryClick(InventoryClickEvent event, Player player) {
			super.onInventoryClick(event, player);
			PlayerShopkeeper shopkeeper = this.getShopkeeper();
			int slot = event.getRawSlot();
			if (slot == 2 || slot == 6) {
				// handle hiring:
				// check if the player can hire (create) this type of shopkeeper:
				if (Settings.hireRequireCreationPermission && (!this.getShopkeeper().getType().hasPermission(player)
						|| !this.getShopkeeper().getShopObject().getObjectType().hasPermission(player))) {
					// missing permission to hire this type of shopkeeper:
					Utils.sendMessage(player, Settings.msgCantHireShopType);
					this.closeDelayed(player);
					return;
				}

				// check if the player can afford it and calculate the resulting player inventory:
				ItemStack[] newPlayerInventoryContents = player.getInventory().getContents();
				ItemStack hireCost = shopkeeper.getHireCost();
				for (int i = 0; i < newPlayerInventoryContents.length; i++) {
					ItemStack item = newPlayerInventoryContents[i];
					if (item != null && item.isSimilar(hireCost)) {
						if (item.getAmount() > hireCost.getAmount()) {
							ItemStack clonedItem = item.clone();
							newPlayerInventoryContents[i] = clonedItem;
							clonedItem.setAmount(item.getAmount() - hireCost.getAmount());
							hireCost.setAmount(0);
							break;
						} else if (item.getAmount() == hireCost.getAmount()) {
							newPlayerInventoryContents[i] = null;
							hireCost.setAmount(0);
							break;
						} else {
							hireCost.setAmount(hireCost.getAmount() - item.getAmount());
							newPlayerInventoryContents[i] = null;
						}
					}
				}

				if (hireCost.getAmount() != 0) {
					// not enough money:
					Utils.sendMessage(player, Settings.msgCantHire);
					// close window for this player:
					this.closeDelayed(player);
					return;
				}

				// call event:
				int maxShops = SKShopkeepersPlugin.getInstance().getMaxShops(player);
				PlayerShopkeeperHiredEvent hireEvent = new PlayerShopkeeperHiredEvent(player, shopkeeper, newPlayerInventoryContents, maxShops);
				Bukkit.getPluginManager().callEvent(hireEvent);
				if (hireEvent.isCancelled()) {
					// close window for this player:
					this.closeDelayed(player);
					return;
				}

				// check max shops limit:
				maxShops = hireEvent.getMaxShopsForPlayer();
				if (maxShops > 0) {
					int count = SKShopkeepersPlugin.getInstance().countShopsOfPlayer(player);
					if (count >= maxShops) {
						Utils.sendMessage(player, Settings.msgTooManyShops);
						this.closeDelayed(player);
						return;
					}
				}

				// hire the shopkeeper:
				player.getInventory().setContents(newPlayerInventoryContents); // apply inventory changes
				shopkeeper.setForHire(null);
				shopkeeper.setOwner(player);
				SKShopkeepersPlugin.getInstance().getShopkeeperStorage().save();
				Utils.sendMessage(player, Settings.msgHired);

				// close all open windows for this shopkeeper:
				shopkeeper.closeAllOpenWindows();
			}
		}
	}

	protected UUID ownerUUID; // not null after successful initialization
	protected String ownerName;
	protected int chestX;
	protected int chestY;
	protected int chestZ;
	protected ItemStack hireCost = null; // null if not for hire

	/**
	 * For use in sub-classes.
	 */
	protected AbstractPlayerShopkeeper() {
	}

	/**
	 * Expects a {@link PlayerShopCreationData}.
	 */
	@Override
	protected void initOnCreation(ShopCreationData creationData) throws ShopkeeperCreateException {
		super.initOnCreation(creationData);
		PlayerShopCreationData playerShopCreationData = (PlayerShopCreationData) creationData;
		Player owner = playerShopCreationData.getOwner();
		Block chest = playerShopCreationData.getShopChest();
		assert owner != null;
		assert chest != null;

		this.ownerUUID = owner.getUniqueId();
		this.ownerName = owner.getName();
		this.setChest(chest.getX(), chest.getY(), chest.getZ());
	}

	@Override
	protected void onInitDone() {
		super.onInitDone();
		this.registerUIHandler(new PlayerShopHiringHandler(this));
	}

	@Override
	protected void onRegistration(int sessionId) {
		super.onRegistration(sessionId);

		// register protected chest:
		SKShopkeepersPlugin.getInstance().getProtectedChests().addChest(this.getWorldName(), chestX, chestY, chestZ, this);
	}

	@Override
	protected void onDeletion() {
		super.onDeletion();

		// unregister previously protected chest:
		SKShopkeepersPlugin.getInstance().getProtectedChests().removeChest(this.getWorldName(), chestX, chestY, chestZ, this);
	}

	@Override
	public boolean openChestWindow(Player player) {
		Log.debug("checking open chest window ..");
		// make sure the chest still exists
		Block chest = this.getChest();
		if (ItemUtils.isChest(chest.getType())) {
			// open the chest directly as the player (no need for a custom UI)
			Log.debug("opening chest inventory window");
			Inventory inv = ((Chest) chest.getState()).getInventory();
			player.openInventory(inv);
			return true;
		}
		return false;
	}

	@Override
	protected void onPlayerInteraction(Player player) {
		// naming via item:
		ItemStack itemInHand = player.getItemInHand();
		if (Settings.namingOfPlayerShopsViaItem && Settings.isNamingItem(itemInHand)) {
			// check if player can edit this shopkeeper:
			PlayerShopEditorHandler editorHandler = (PlayerShopEditorHandler) this.getUIHandler(DefaultUITypes.EDITOR());
			if (editorHandler.canOpen(player)) {
				// rename with the player's item in hand:
				String newName;
				ItemMeta itemMeta;
				if (!itemInHand.hasItemMeta() || (itemMeta = itemInHand.getItemMeta()) == null || !itemMeta.hasDisplayName()) {
					newName = "";
				} else {
					newName = itemMeta.getDisplayName();
				}

				// handled name changing:
				if (this.requestNameChange(player, newName)) {
					// manually remove rename item from player's hand after this event is processed:
					Bukkit.getScheduler().runTask(SKShopkeepersPlugin.getInstance(), () -> {
						ItemStack newItemInHand = ItemUtils.descreaseItemAmount(itemInHand, 1);
						player.setItemInHand(newItemInHand);
					});
				}
				return;
			}
		}

		if (!player.isSneaking() && this.isForHire()) {
			// open hiring window:
			this.openHireWindow(player);
		} else {
			// open editor or trading window:
			super.onPlayerInteraction(player);
		}
	}

	@Override
	protected void load(ConfigurationSection config) throws ShopkeeperCreateException {
		super.load(config);
		try {
			ownerUUID = UUID.fromString(config.getString("owner uuid"));
		} catch (Exception e) {
			// uuid invalid or non-existent:
			throw new ShopkeeperCreateException("Missing owner uuid!");
		}
		ownerName = config.getString("owner", "unknown");

		if (!config.isInt("chestx") || !config.isInt("chesty") || !config.isInt("chestz")) {
			throw new ShopkeeperCreateException("Missing chest coordinate(s)");
		}

		// update chest:
		this.setChest(config.getInt("chestx"), config.getInt("chesty"), config.getInt("chestz"));

		hireCost = config.getItemStack("hirecost");
		// hire cost itemstack is not null, but empty -> normalize to null:
		if (hireCost != null && ItemUtils.isEmpty(hireCost)) {
			Log.warning("Invalid (empty) hire cost! Disabling 'for hire' for shopkeeper at " + this.getPositionString());
			hireCost = null;
		}
	}

	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("owner uuid", ownerUUID.toString());
		config.set("owner", ownerName);
		config.set("chestx", chestX);
		config.set("chesty", chestY);
		config.set("chestz", chestZ);
		if (hireCost != null) {
			config.set("hirecost", hireCost);
		}
	}

	@Override
	public void setOwner(Player player) {
		this.setOwner(player.getUniqueId(), player.getName());
	}

	@Override
	public void setOwner(UUID ownerUUID, String ownerName) {
		this.ownerUUID = ownerUUID;
		this.ownerName = ownerName;
		// TODO do this in a more abstract way
		if (!Settings.allowRenamingOfPlayerNpcShops && this.getShopObject().getObjectType() == DefaultShopObjectTypes.CITIZEN()) {
			// update the npc's name:
			((CitizensShop) this.getShopObject()).setName(ownerName);
		} else if (this.getShopObject().getObjectType() == DefaultShopObjectTypes.SIGN()) {
			// update sign:
			((SignShop) this.getShopObject()).updateSign();
		}
	}

	@Override
	public UUID getOwnerUUID() {
		return ownerUUID;
	}

	@Override
	public String getOwnerName() {
		return ownerName;
	}

	@Override
	public String getOwnerAsString() {
		return Utils.getPlayerAsString(ownerName, ownerUUID);
	}

	@Override
	public boolean isOwner(Player player) {
		return player.getUniqueId().equals(ownerUUID);
	}

	@Override
	public Player getOwner() {
		return Bukkit.getPlayer(ownerUUID);
	}

	@Override
	public boolean isForHire() {
		return (hireCost != null);
	}

	@Override
	public void setForHire(ItemStack hireCost) {
		if (ItemUtils.isEmpty(hireCost)) {
			// disable hiring:
			this.hireCost = null;
			this.setName("");
		} else {
			// set for hire:
			this.hireCost = hireCost.clone();
			this.setName(Settings.forHireTitle);
		}
	}

	@Override
	public ItemStack getHireCost() {
		return (this.isForHire() ? hireCost.clone() : null);
	}

	@Override
	public void setChest(int chestX, int chestY, int chestZ) {
		if (this.isValid()) {
			// unregister previously protected chest:
			SKShopkeepersPlugin.getInstance().getProtectedChests().removeChest(this.getWorldName(), chestX, chestY, chestZ, this);
		}

		// update chest:
		this.chestX = chestX;
		this.chestY = chestY;
		this.chestZ = chestZ;

		if (this.isValid()) {
			// register new protected chest:
			SKShopkeepersPlugin.getInstance().getProtectedChests().addChest(this.getWorldName(), chestX, chestY, chestZ, this);
		}
	}

	/**
	 * Checks whether this shop uses the indicated chest.
	 * 
	 * @param chest
	 *            the chest to check
	 * @return
	 * 		TODO unused?
	 */
	public boolean usesChest(Block chest) {
		if (!chest.getWorld().getName().equals(this.getWorldName())) return false;
		int x = chest.getX();
		int y = chest.getY();
		int z = chest.getZ();
		if (x == chestX && y == chestY && z == chestZ) return true;
		if (x == chestX + 1 && y == chestY && z == chestZ) return true;
		if (x == chestX - 1 && y == chestY && z == chestZ) return true;
		if (x == chestX && y == chestY && z == chestZ + 1) return true;
		if (x == chestX && y == chestY && z == chestZ - 1) return true;
		return false;
	}

	@Override
	public Block getChest() {
		return Bukkit.getWorld(this.getWorldName()).getBlockAt(chestX, chestY, chestZ);
	}

	// returns null (and logs a warning) if the price cannot be represented correctly by currency items
	protected TradingRecipe createSellingRecipe(ItemStack itemBeingSold, int price) {
		int remainingPrice = price;

		ItemStack item1 = null;
		ItemStack item2 = null;

		if (Settings.isHighCurrencyEnabled() && price > Settings.highCurrencyMinCost) {
			int highCurrencyAmount = Math.min(price / Settings.highCurrencyValue, Settings.highCurrencyItem.getMaxStackSize());
			if (highCurrencyAmount > 0) {
				remainingPrice -= (highCurrencyAmount * Settings.highCurrencyValue);
				ItemStack highCurrencyItem = Settings.createHighCurrencyItem(highCurrencyAmount);
				item1 = highCurrencyItem; // using the first slot
			}
		}

		if (remainingPrice > 0) {
			if (remainingPrice > Settings.currencyItem.getMaxStackSize()) {
				// cannot represent this price with the used currency items:
				Log.warning("Shopkeeper at " + this.getPositionString() + " owned by " + ownerName + " has an invalid cost!");
				return null;
			}

			ItemStack currencyItem = Settings.createCurrencyItem(remainingPrice);
			if (item1 == null) {
				item1 = currencyItem;
			} else {
				// the first item of the trading recipe is already used by the high currency item:
				item2 = currencyItem;
			}
		}
		return new SKTradingRecipe(itemBeingSold, item1, item2);
	}

	// returns null (and logs a warning) if the price cannot be represented correctly by currency items
	protected TradingRecipe createBuyingRecipe(ItemStack itemBeingBought, int price) {
		if (price > Settings.currencyItem.getMaxStackSize()) {
			// cannot represent this price with the used currency items:
			Log.warning("Shopkeeper at " + this.getPositionString() + " owned by " + ownerName + " has an invalid cost!");
			return null;
		}
		ItemStack currencyItem = Settings.createCurrencyItem(price);
		return new SKTradingRecipe(currencyItem, itemBeingBought, null);
	}

	@Override
	public int getCurrencyInChest() {
		Block chest = this.getChest();
		if (!ItemUtils.isChest(chest.getType())) return 0;

		int totalCurrency = 0;
		Inventory chestInventory = ((Chest) chest.getState()).getInventory();
		ItemStack[] chestContents = chestInventory.getContents();
		for (ItemStack itemStack : chestContents) {
			if (Settings.isCurrencyItem(itemStack)) {
				totalCurrency += itemStack.getAmount();
			} else if (Settings.isHighCurrencyItem(itemStack)) {
				totalCurrency += (itemStack.getAmount() * Settings.highCurrencyValue);
			}
		}
		return totalCurrency;
	}

	protected List<ItemCount> getItemsFromChest(Filter<ItemStack> filter) {
		ItemStack[] chestContents = null;
		Block chest = this.getChest();
		if (ItemUtils.isChest(chest.getType())) {
			Inventory chestInventory = ((Chest) chest.getState()).getInventory();
			chestContents = chestInventory.getContents();
		}
		// returns an empty list if the chest couldn't be found:
		return ItemUtils.countItems(chestContents, filter);
	}
}
