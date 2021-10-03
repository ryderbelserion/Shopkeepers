package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.api.user.User;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.debug.DebugOptions;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKTradingRecipe;
import com.nisovin.shopkeepers.shopkeeper.ShopkeeperData;
import com.nisovin.shopkeepers.user.SKUser;
import com.nisovin.shopkeepers.util.bukkit.DataUtils;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.InventoryUtils;
import com.nisovin.shopkeepers.util.inventory.ItemMigration;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CyclicCounter;
import com.nisovin.shopkeepers.util.java.RateLimiter;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class AbstractPlayerShopkeeper extends AbstractShopkeeper implements PlayerShopkeeper {

	private static final boolean DEFAULT_NOTIFY_ON_TRADES = true;

	private static final int CHECK_CONTAINER_PERIOD_SECONDS = 5;
	private static final CyclicCounter nextCheckingOffset = new CyclicCounter(1, CHECK_CONTAINER_PERIOD_SECONDS + 1);

	protected User owner; // Not null after successful initialization
	// TODO Store container world separately? Currently it uses the shopkeeper world.
	// This would allow the container and shopkeeper to be located in different worlds, and virtual player shops.
	protected int containerX;
	protected int containerY;
	protected int containerZ;
	private boolean notifyOnTrades = DEFAULT_NOTIFY_ON_TRADES;
	protected UnmodifiableItemStack hireCost = null; // Null if not for hire

	// Initial threshold between [1, CHECK_CONTAINER_PERIOD_SECONDS] for load balancing:
	private final RateLimiter checkContainerLimiter = new RateLimiter(CHECK_CONTAINER_PERIOD_SECONDS, nextCheckingOffset.getAndIncrement());

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
		Block container = playerShopCreationData.getShopContainer();
		assert owner != null;
		assert container != null;

		this._setOwner(owner.getUniqueId(), owner.getName());
		this._setContainer(container.getX(), container.getY(), container.getZ());
	}

	@Override
	protected void setup() {
		if (this.getUIHandler(DefaultUITypes.HIRING()) == null) {
			this.registerUIHandler(new PlayerShopHiringHandler(this));
		}
		super.setup();
	}

	@Override
	public void loadDynamicState(ShopkeeperData shopkeeperData) throws InvalidDataException {
		super.loadDynamicState(shopkeeperData);
		this.loadOwner(shopkeeperData);
		this.loadContainer(shopkeeperData);
		this.loadNotifyOnTrades(shopkeeperData);
		this.loadForHire(shopkeeperData);
	}

	@Override
	public void saveDynamicState(ShopkeeperData shopkeeperData) {
		super.saveDynamicState(shopkeeperData);
		this.saveOwner(shopkeeperData);
		this.saveContainer(shopkeeperData);
		this.saveNotifyOnTrades(shopkeeperData);
		this.saveForHire(shopkeeperData);
	}

	@Override
	protected void onAdded(ShopkeeperAddedEvent.Cause cause) {
		super.onAdded(cause);

		// Register protected container:
		SKShopkeepersPlugin.getInstance().getProtectedContainers().addContainer(this.getWorldName(), containerX, containerY, containerZ, this);
	}

	@Override
	protected void onRemoval(ShopkeeperRemoveEvent.Cause cause) {
		super.onRemoval(cause);

		// Unregister previously protected container:
		SKShopkeepersPlugin.getInstance().getProtectedContainers().removeContainer(this.getWorldName(), containerX, containerY, containerZ, this);
	}

	@Override
	public void delete(Player player) {
		// Return the shop creation item:
		if (Settings.deletingPlayerShopReturnsCreationItem && player != null && this.isOwner(player)) {
			ItemStack shopCreationItem = Settings.createShopCreationItem();
			Map<Integer, ItemStack> remaining = player.getInventory().addItem(shopCreationItem);
			if (!remaining.isEmpty()) {
				// Inventory is full, drop the item instead:
				Location playerLocation = player.getEyeLocation();
				Location shopLocation = this.getShopObject().getLocation(); // Null if not spawned
				// If within a certain range, drop the item at the shop's location, else drop at player's location:
				Location dropLocation;
				if (shopLocation != null && LocationUtils.getDistanceSquared(shopLocation, playerLocation) <= 100) {
					dropLocation = shopLocation;
				} else {
					dropLocation = playerLocation;
				}
				dropLocation.getWorld().dropItem(dropLocation, shopCreationItem);
			}
		}
		super.delete(player);
	}

	@Override
	protected void populateMessageArguments(Map<String, Supplier<Object>> messageArguments) {
		super.populateMessageArguments(messageArguments);
		messageArguments.put("owner_name", () -> this.getOwnerName());
		messageArguments.put("owner_uuid", () -> this.getOwnerUUID().toString());
	}

	@Override
	public void onPlayerInteraction(Player player) {
		Validate.notNull(player, "player is null");

		// Naming via item:
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		if (Settings.namingOfPlayerShopsViaItem && Settings.isNamingItem(itemInMainHand)) {
			// Check if player can edit this shopkeeper:
			PlayerShopEditorHandler editorHandler = (PlayerShopEditorHandler) this.getUIHandler(DefaultUITypes.EDITOR());
			if (editorHandler.canOpen(player, false)) {
				// Rename with the player's item in hand:
				ItemMeta itemMeta = itemInMainHand.getItemMeta(); // Can be null
				String newName = (itemMeta != null && itemMeta.hasDisplayName()) ? itemMeta.getDisplayName() : "";
				assert newName != null; // ItemMeta#getDisplayName returns non-null in all cases

				// Handled name changing:
				if (SKShopkeepersPlugin.getInstance().getShopkeeperNaming().requestNameChange(player, this, newName)) {
					// Manually remove rename item from player's hand after this event is processed:
					Bukkit.getScheduler().runTask(ShopkeepersPlugin.getInstance(), () -> {
						ItemStack newItemInMainHand = ItemUtils.descreaseItemAmount(itemInMainHand, 1);
						playerInventory.setItemInMainHand(newItemInMainHand);
					});
				}
				return;
			}
		}

		if (!player.isSneaking() && this.isForHire()) {
			// Open hiring window:
			this.openHireWindow(player);
		} else {
			// Open editor or trading window:
			super.onPlayerInteraction(player);
		}
	}

	// OWNER

	private void loadOwner(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		UUID ownerUUID;
		try {
			ownerUUID = UUID.fromString(shopkeeperData.getString("owner uuid"));
		} catch (Exception e) {
			// UUID is invalid or non-existent:
			throw new InvalidDataException("Missing or invalid owner uuid!");
		}
		String ownerName = shopkeeperData.getString("owner");
		// TODO We no longer use the fallback name (since late 1.14.4). Remove the "unknown"-check again in the future
		// (as soon as possible, because it conflicts with any player actually named 'unknown').
		if (ownerName == null || ownerName.isEmpty() || ownerName.equals("unknown")) {
			throw new InvalidDataException("Missing owner name!");
		}
		this._setOwner(ownerUUID, ownerName);
	}

	private void saveOwner(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set("owner uuid", this.getOwnerUUID());
		shopkeeperData.set("owner", this.getOwnerName());
	}

	@Override
	public void setOwner(Player player) {
		this.setOwner(player.getUniqueId(), player.getName());
	}

	// TODO Add to API
	public void setOwner(User owner) {
		this._setOwner(owner);
		this.markDirty();
	}

	@Override
	public void setOwner(UUID ownerUUID, String ownerName) {
		this._setOwner(ownerUUID, ownerName);
		this.markDirty();
	}

	private void _setOwner(UUID ownerUUID, String ownerName) {
		this._setOwner(SKUser.of(ownerUUID, ownerName));
	}

	private void _setOwner(User owner) {
		Validate.notNull(owner, "owner is null");
		this.owner = owner;

		// Inform the shop object:
		this.getShopObject().onShopOwnerChanged();
	}

	public User getOwnerUser() {
		return owner;
	}

	@Override
	public UUID getOwnerUUID() {
		return owner.getUniqueId();
	}

	@Override
	public String getOwnerName() {
		return owner.getLastKnownName();
	}

	@Override
	public String getOwnerString() {
		return TextUtils.getPlayerString(owner);
	}

	@Override
	public boolean isOwner(Player player) {
		return player.getUniqueId().equals(this.getOwnerUUID());
	}

	@Override
	public Player getOwner() {
		return Bukkit.getPlayer(this.getOwnerUUID());
	}

	// TRADE NOTIFICATIONS

	private void loadNotifyOnTrades(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		boolean notifyOnTrades = shopkeeperData.getBooleanOrDefault("notifyOnTrades", DEFAULT_NOTIFY_ON_TRADES);
		this._setNotifyOnTrades(notifyOnTrades);
	}

	private void saveNotifyOnTrades(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		// We only store this property if its value does not match the default value:
		if (notifyOnTrades != DEFAULT_NOTIFY_ON_TRADES) {
			shopkeeperData.set("notifyOnTrades", notifyOnTrades);
		}
	}

	@Override
	public boolean isNotifyOnTrades() {
		return notifyOnTrades;
	}

	@Override
	public void setNotifyOnTrades(boolean notifyOnTrades) {
		if (this.notifyOnTrades == notifyOnTrades) return;
		this._setNotifyOnTrades(notifyOnTrades);
		this.markDirty();
	}

	private void _setNotifyOnTrades(boolean notifyOnTrades) {
		this.notifyOnTrades = notifyOnTrades;
	}

	// HIRING

	private void loadForHire(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		// The item is assumed to be immutable and therefore does not need to be copied.
		UnmodifiableItemStack hireCost = DataUtils.loadUnmodifiableItemStack(shopkeeperData, "hirecost");
		// Hire cost ItemStack is not null, but empty. -> Normalize to null:
		if (hireCost != null && ItemUtils.isEmpty(hireCost)) {
			Log.warning(this.getLogPrefix() + "Hire cost item is empty! Disabling 'for hire'.");
			hireCost = null;
			this.markDirty();
		}
		UnmodifiableItemStack migratedHireCost = ItemMigration.migrateItemStack(hireCost);
		if (!ItemUtils.isSimilar(hireCost, migratedHireCost)) {
			if (ItemUtils.isEmpty(migratedHireCost) && !ItemUtils.isEmpty(hireCost)) {
				// Migration failed:
				Log.warning(this.getLogPrefix() + "Hire cost item migration failed: " + hireCost);
				hireCost = null;
			} else {
				hireCost = migratedHireCost;
				Log.debug(DebugOptions.itemMigrations, () -> this.getLogPrefix() + "Migrated hire cost item.");
			}
			this.markDirty();
		}
		this._setForHire(ItemUtils.asItemStackOrNull(hireCost));
	}

	private void saveForHire(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		DataUtils.saveItemStack(shopkeeperData, "hirecost", hireCost);
	}

	@Override
	public boolean isForHire() {
		return (hireCost != null);
	}

	@Override
	public void setForHire(ItemStack hireCost) {
		this._setForHire(hireCost);
		this.markDirty();
	}

	private void _setForHire(ItemStack hireCost) {
		boolean isForHire = this.isForHire();
		if (ItemUtils.isEmpty(hireCost)) {
			// Disable hiring:
			this.hireCost = null;

			// If the shopkeeper was previously for hire, reset its name:
			if (isForHire) {
				this.setName("");
			}
		} else {
			// Set for hire:
			this.hireCost = ItemUtils.unmodifiableCloneIfModifiable(hireCost);
			this.setName(Messages.forHireTitle);
		}
		// TODO Close any currently open hiring UIs for players.
	}

	@Override
	public UnmodifiableItemStack getHireCost() {
		return hireCost;
	}

	// CONTAINER

	private void loadContainer(ShopkeeperData shopkeeperData) throws InvalidDataException {
		assert shopkeeperData != null;
		// TODO Rename to storage keys to containerx/y/z?
		if (!shopkeeperData.isNumber("chestx") || !shopkeeperData.isNumber("chesty") || !shopkeeperData.isNumber("chestz")) {
			throw new InvalidDataException("Missing or invalid container coordinates!");
		}
		this._setContainer(shopkeeperData.getInt("chestx"), shopkeeperData.getInt("chesty"), shopkeeperData.getInt("chestz"));
	}

	private void saveContainer(ShopkeeperData shopkeeperData) {
		assert shopkeeperData != null;
		shopkeeperData.set("chestx", containerX);
		shopkeeperData.set("chesty", containerY);
		shopkeeperData.set("chestz", containerZ);
	}

	protected void _setContainer(int containerX, int containerY, int containerZ) {
		if (this.isValid()) {
			// Unregister previously protected container:
			SKShopkeepersPlugin.getInstance().getProtectedContainers().removeContainer(this.getWorldName(), containerX, containerY, containerZ, this);
		}

		// Update container:
		this.containerX = containerX;
		this.containerY = containerY;
		this.containerZ = containerZ;

		if (this.isValid()) {
			// Register new protected container:
			SKShopkeepersPlugin.getInstance().getProtectedContainers().addContainer(this.getWorldName(), containerX, containerY, containerZ, this);
		}
	}

	@Override
	public int getContainerX() {
		return containerX;
	}

	@Override
	public int getContainerY() {
		return containerY;
	}

	@Override
	public int getContainerZ() {
		return containerZ;
	}

	@Override
	public void setContainer(int containerX, int containerY, int containerZ) {
		this._setContainer(containerX, containerY, containerZ);
		this.markDirty();
	}

	@Override
	public Block getContainer() {
		return Bukkit.getWorld(this.getWorldName()).getBlockAt(containerX, containerY, containerZ);
	}

	// Returns null if the container could not be found.
	public Inventory getContainerInventory() {
		Block container = this.getContainer();
		if (ShopContainers.isSupportedContainer(container.getType())) {
			return ShopContainers.getInventory(container); // Not null
		}
		return null;
	}

	// Returns an empty array if the container could not be found.
	public ItemStack[] getContainerContents() {
		Inventory containerInventory = this.getContainerInventory();
		if (containerInventory == null) {
			// Container not found:
			return InventoryUtils.emptyItemStackArray();
		} else {
			return containerInventory.getContents(); // Not null
		}
	}

	@Override
	public int getCurrencyInContainer() {
		int totalCurrency = 0;
		ItemStack[] contents = this.getContainerContents(); // Empty if the container is not found
		for (ItemStack itemStack : contents) {
			if (Settings.isCurrencyItem(itemStack)) {
				totalCurrency += itemStack.getAmount();
			} else if (Settings.isHighCurrencyItem(itemStack)) {
				totalCurrency += (itemStack.getAmount() * Settings.highCurrencyValue);
			}
		}
		return totalCurrency;
	}

	// Returns null (and logs a warning) if the price cannot be represented correctly by currency items.
	protected final TradingRecipe createSellingRecipe(UnmodifiableItemStack itemBeingSold, int price, boolean outOfStock) {
		int remainingPrice = price;

		UnmodifiableItemStack item1 = null;
		UnmodifiableItemStack item2 = null;

		if (Settings.isHighCurrencyEnabled() && price > Settings.highCurrencyMinCost) {
			int highCurrencyAmount = Math.min(price / Settings.highCurrencyValue, Settings.highCurrencyItem.getType().getMaxStackSize());
			if (highCurrencyAmount > 0) {
				remainingPrice -= (highCurrencyAmount * Settings.highCurrencyValue);
				UnmodifiableItemStack highCurrencyItem = UnmodifiableItemStack.of(Settings.createHighCurrencyItem(highCurrencyAmount));
				item1 = highCurrencyItem; // Using the first slot
			}
		}

		if (remainingPrice > 0) {
			if (remainingPrice > Settings.currencyItem.getType().getMaxStackSize()) {
				// Cannot represent this price with the used currency items:
				// TODO Move this warning into the loading phase.
				int maxPrice = getMaximumSellingPrice();
				Log.warning(this.getLogPrefix() + "Skipping offer with invalid price (" + price + "). Maximum price is "
						+ maxPrice + ".");
				return null;
			}

			UnmodifiableItemStack currencyItem = UnmodifiableItemStack.of(Settings.createCurrencyItem(remainingPrice));
			if (item1 == null) {
				item1 = currencyItem;
			} else {
				// The first item of the trading recipe is already used by the high currency item:
				item2 = currencyItem;
			}
		}
		return new SKTradingRecipe(itemBeingSold, item1, item2, outOfStock);
	}

	// Returns null (and logs a warning) if the price cannot be represented correctly by currency items.
	protected final TradingRecipe createBuyingRecipe(UnmodifiableItemStack itemBeingBought, int price, boolean outOfStock) {
		int maxPrice = Settings.currencyItem.getType().getMaxStackSize();
		if (price > maxPrice) {
			// Cannot represent this price with the used currency items:
			// TODO Move this warning into the loading phase.
			Log.warning(this.getLogPrefix() + "Skipping offer with invalid price (" + price + "). Maximum price is "
					+ maxPrice + ".");
			return null;
		}
		UnmodifiableItemStack currencyItem = UnmodifiableItemStack.of(Settings.createCurrencyItem(price));
		return new SKTradingRecipe(currencyItem, itemBeingBought, null, outOfStock);
	}

	private static int getMaximumSellingPrice() {
		int maxPrice = Settings.currencyItem.getType().getMaxStackSize();
		if (Settings.isHighCurrencyEnabled()) {
			maxPrice += Settings.highCurrencyItem.getType().getMaxStackSize() * Settings.highCurrencyValue;
		}
		return maxPrice;
	}

	// SHOPKEEPER UIs - Shortcuts for common UI types:

	@Override
	public boolean openHireWindow(Player player) {
		return this.openWindow(DefaultUITypes.HIRING(), player);
	}

	@Override
	public boolean openContainerWindow(Player player) {
		// Check if the container still exists:
		Inventory containerInventory = this.getContainerInventory();
		if (containerInventory == null) {
			Log.debug(() -> "Cannot open container inventory for player '" + player.getName()
					+ "': The block is no longer a valid container!");
			return false;
		}

		Log.debug(() -> "Opening container inventory for player '" + player.getName() + "'.");
		// Open the container directly for the player (no need for a custom UI):
		player.openInventory(containerInventory);
		return true;
	}

	// TICKING

	@Override
	public void tick() {
		super.tick();
		// Delete the shopkeeper if the container is no longer present (eg. if it got removed externally by another
		// plugin, such as WorldEdit, etc.):
		if (Settings.deleteShopkeeperOnBreakContainer) {
			if (!checkContainerLimiter.request()) {
				return;
			}

			// This checks if the block is still a valid container:
			Block containerBlock = this.getContainer();
			if (!ShopContainers.isSupportedContainer(containerBlock.getType())) {
				// Note: If this shopkeeper got deleted due to the chest being broken, we will trigger a delayed save
				// after the ticking of the shopkeepers.
				SKShopkeepersPlugin.getInstance().getRemoveShopOnContainerBreak().handleBlockBreakage(containerBlock);
			}
		}
	}
}
