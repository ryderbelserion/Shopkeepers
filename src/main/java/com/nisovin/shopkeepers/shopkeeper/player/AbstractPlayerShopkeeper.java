package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerShopkeeperHireEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.citizens.SKCitizensShopObject;
import com.nisovin.shopkeepers.shopobjects.sign.SKSignShopObject;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.ui.defaults.HiringHandler;
import com.nisovin.shopkeepers.ui.defaults.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.defaults.TradingHandler;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public abstract class AbstractPlayerShopkeeper extends AbstractShopkeeper implements PlayerShopkeeper {

	public static abstract class PlayerShopEditorHandler extends EditorHandler {

		protected static abstract class CommonEditorSetup<S extends AbstractPlayerShopkeeper, O> {

			protected final S shopkeeper;
			protected final PlayerShopEditorHandler editorHandler;

			public CommonEditorSetup(S shopkeeper, PlayerShopEditorHandler editorHandler) {
				this.shopkeeper = shopkeeper;
				this.editorHandler = editorHandler;
			}

			public boolean openWindow(Player player) {
				// create inventory:
				Inventory inventory = Bukkit.createInventory(player, 27, Settings.editorTitle);

				// setup trade columns:
				this.setupOfferColumns(inventory, player);

				// add the special buttons:
				editorHandler.setActionButtons(inventory);

				// show editing inventory:
				player.openInventory(inventory);

				return true;
			}

			protected void setupOfferColumns(Inventory inventory, Player player) {
				// TODO allow setup similar to trading shopkeeper?
				// add the shopkeeper's offers:
				int column = 0;
				List<O> offers = this.getOffers();
				for (; column < offers.size() && column < TRADE_COLUMNS; column++) {
					O offer = offers.get(column);
					// add offer to inventory:
					this.setupColumnForOffer(inventory, column, offer);
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

						// add new offer to inventory:
						this.setupColumnForItem(inventory, column, itemFromChest);
					}
				}
			}

			protected abstract List<O> getOffers();

			protected abstract List<ItemCount> getItemsFromChest();

			protected abstract boolean hasOffer(ItemStack itemFromChest);

			protected abstract void setupColumnForOffer(Inventory inventory, int column, O offer);

			protected abstract void setupColumnForItem(Inventory inventory, int column, ItemStack itemFromChest);
		}

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
		protected void onInventoryDrag(InventoryDragEvent event, Player player) {
			// cancel all inventory clicks and handle everything on our own:
			// TODO maybe allow certain inventory actions which only affect the player's inventory?
			event.setCancelled(true);
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

	public static abstract class PlayerShopTradingHandler extends TradingHandler {

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
				// TODO prevent hiring own shops?
				// actually: this feature was originally meant for admins to set up pre-existing shops
				// handle hiring:
				// check if the player can hire (create) this type of shopkeeper:
				if (Settings.hireRequireCreationPermission && (!this.getShopkeeper().getType().hasPermission(player)
						|| !this.getShopkeeper().getShopObject().getType().hasPermission(player))) {
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
				int maxShops = Settings.getMaxShops(player);
				PlayerShopkeeperHireEvent hireEvent = new PlayerShopkeeperHireEvent(shopkeeper, player, newPlayerInventoryContents, maxShops);
				Bukkit.getPluginManager().callEvent(hireEvent);
				if (hireEvent.isCancelled()) {
					Log.debug("PlayerShopkeeperHireEvent was cancelled!");
					// close window for this player:
					this.closeDelayed(player);
					return;
				}

				// check max shops limit:
				maxShops = hireEvent.getMaxShopsLimit();
				if (maxShops > 0) {
					int count = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry().countShopsOfPlayer(player);
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
				shopkeeper.save();
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
	 * Creates a not yet initialized {@link AbstractPlayerShopkeeper} (for use in sub-classes).
	 * <p>
	 * See {@link AbstractShopkeeper} for details on initialization.
	 * 
	 * @param id
	 *            the shopkeeper id
	 */
	protected AbstractPlayerShopkeeper(int id) {
		super(id);
	}

	/**
	 * Expects a {@link PlayerShopCreationData}.
	 */
	@Override
	protected void loadFromCreationData(ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		super.loadFromCreationData(shopCreationData);
		PlayerShopCreationData playerShopCreationData = (PlayerShopCreationData) shopCreationData;
		Player owner = playerShopCreationData.getCreator();
		Block chest = playerShopCreationData.getShopChest();
		assert owner != null;
		assert chest != null;

		this.ownerUUID = owner.getUniqueId();
		this.ownerName = owner.getName();
		this._setChest(chest.getX(), chest.getY(), chest.getZ());
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.HIRING()) == null) {
			this.registerUIHandler(new PlayerShopHiringHandler(this));
		}
		super.setup();
	}

	@Override
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		try {
			ownerUUID = UUID.fromString(configSection.getString("owner uuid"));
		} catch (Exception e) {
			// uuid invalid or non-existent:
			throw new ShopkeeperCreateException("Missing owner uuid!");
		}
		ownerName = configSection.getString("owner", "unknown");

		if (!configSection.isInt("chestx") || !configSection.isInt("chesty") || !configSection.isInt("chestz")) {
			throw new ShopkeeperCreateException("Missing chest coordinate(s)");
		}

		// update chest:
		this._setChest(configSection.getInt("chestx"), configSection.getInt("chesty"), configSection.getInt("chestz"));

		hireCost = configSection.getItemStack("hirecost");
		// hire cost itemstack is not null, but empty -> normalize to null:
		if (hireCost != null && ItemUtils.isEmpty(hireCost)) {
			Log.warning("Invalid (empty) hire cost! Disabling 'for hire' for shopkeeper at " + this.getPositionString());
			hireCost = null;
			this.markDirty();
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("owner uuid", ownerUUID.toString());
		configSection.set("owner", ownerName);
		configSection.set("chestx", chestX);
		configSection.set("chesty", chestY);
		configSection.set("chestz", chestZ);
		if (hireCost != null) {
			configSection.set("hirecost", hireCost);
		}
	}

	@Override
	protected void onAdded(ShopkeeperAddedEvent.Cause cause) {
		super.onAdded(cause);

		// register protected chest:
		SKShopkeepersPlugin.getInstance().getProtectedChests().addChest(this.getWorldName(), chestX, chestY, chestZ, this);
	}

	@Override
	protected void onRemoval(ShopkeeperRemoveEvent.Cause cause) {
		super.onRemoval(cause);

		// unregister previously protected chest:
		SKShopkeepersPlugin.getInstance().getProtectedChests().removeChest(this.getWorldName(), chestX, chestY, chestZ, this);
	}

	@Override
	public void onPlayerInteraction(Player player) {
		// naming via item:
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		if (Settings.namingOfPlayerShopsViaItem && Settings.isNamingItem(itemInMainHand)) {
			// check if player can edit this shopkeeper:
			PlayerShopEditorHandler editorHandler = (PlayerShopEditorHandler) this.getUIHandler(DefaultUITypes.EDITOR());
			if (editorHandler.canOpen(player)) {
				// rename with the player's item in hand:
				String newName;
				ItemMeta itemMeta;
				if (!itemInMainHand.hasItemMeta() || (itemMeta = itemInMainHand.getItemMeta()) == null || !itemMeta.hasDisplayName()) {
					newName = "";
				} else {
					newName = itemMeta.getDisplayName();
				}

				// handled name changing:
				if (SKShopkeepersPlugin.getInstance().getShopkeeperNaming().requestNameChange(player, this, newName)) {
					// manually remove rename item from player's hand after this event is processed:
					Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
						ItemStack newItemInMainHand = ItemUtils.descreaseItemAmount(itemInMainHand, 1);
						playerInventory.setItemInMainHand(newItemInMainHand);
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
	public void setOwner(Player player) {
		this.setOwner(player.getUniqueId(), player.getName());
	}

	@Override
	public void setOwner(UUID ownerUUID, String ownerName) {
		this.markDirty();
		this.ownerUUID = ownerUUID;
		this.ownerName = ownerName;
		// TODO do this in a more abstract way
		if (!Settings.allowRenamingOfPlayerNpcShops && this.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
			// update the npc's name:
			((SKCitizensShopObject) this.getShopObject()).setName(ownerName);
		} else if (this.getShopObject().getType() == DefaultShopObjectTypes.SIGN()) {
			// update sign:
			((SKSignShopObject) this.getShopObject()).updateSign();
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
		this.markDirty();
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

	protected void _setChest(int chestX, int chestY, int chestZ) {
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

	@Override
	public void setChest(int chestX, int chestY, int chestZ) {
		this._setChest(chestX, chestY, chestZ);
		this.markDirty();
	}

	@Override
	public Block getChest() {
		return Bukkit.getWorld(this.getWorldName()).getBlockAt(chestX, chestY, chestZ);
	}

	// returns null (and logs a warning) if the price cannot be represented correctly by currency items
	protected TradingRecipe createSellingRecipe(ItemStack itemBeingSold, int price, boolean outOfStock) {
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
		return ShopkeepersAPI.createTradingRecipe(itemBeingSold, item1, item2, outOfStock);
	}

	// returns null (and logs a warning) if the price cannot be represented correctly by currency items
	protected TradingRecipe createBuyingRecipe(ItemStack itemBeingBought, int price, boolean outOfStock) {
		if (price > Settings.currencyItem.getMaxStackSize()) {
			// cannot represent this price with the used currency items:
			Log.warning("Shopkeeper at " + this.getPositionString() + " owned by " + ownerName + " has an invalid cost!");
			return null;
		}
		ItemStack currencyItem = Settings.createCurrencyItem(price);
		return ShopkeepersAPI.createTradingRecipe(currencyItem, itemBeingBought, null, outOfStock);
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

	// SHOPKEEPER UIs - shortcuts for common UI types:

	@Override
	public boolean openHireWindow(Player player) {
		return this.openWindow(DefaultUITypes.HIRING(), player);
	}

	@Override
	public boolean openChestWindow(Player player) {
		// make sure the chest still exists
		Block chest = this.getChest();
		if (!ItemUtils.isChest(chest.getType())) {
			Log.debug("Cannot open chest inventory for player '" + player.getName() + "': The block is no longer a chest!");
			return false;
		}

		Log.debug("Opening chest inventory for player '" + player.getName() + "'.");
		// open the chest directly as the player (no need for a custom UI)
		Inventory inv = ((Chest) chest.getState()).getInventory();
		player.openInventory(inv);
		return true;
	}
}
