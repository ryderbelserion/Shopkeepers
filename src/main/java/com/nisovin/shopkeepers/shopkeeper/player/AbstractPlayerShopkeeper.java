package com.nisovin.shopkeepers.shopkeeper.player;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.ShopkeeperAddedEvent;
import com.nisovin.shopkeepers.api.events.ShopkeeperRemoveEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopkeeper.TradingRecipe;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.DefaultShopObjectTypes;
import com.nisovin.shopkeepers.api.ui.DefaultUITypes;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.citizens.SKCitizensShopObject;
import com.nisovin.shopkeepers.shopobjects.sign.SKSignShopObject;
import com.nisovin.shopkeepers.util.Filter;
import com.nisovin.shopkeepers.util.ItemCount;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Utils;
import com.nisovin.shopkeepers.util.Validate;

public abstract class AbstractPlayerShopkeeper extends AbstractShopkeeper implements PlayerShopkeeper {

	private static final int CHECK_CONTAINER_PERIOD_SECONDS = 5;

	protected UUID ownerUUID; // Not null after successful initialization
	protected String ownerName; // Not null after successful initialization
	// TODO Store container world separately? Currently it uses the shopkeeper world.
	// This would allow the container and shopkeeper to be located in different worlds, and virtual player shops.
	protected int containerX;
	protected int containerY;
	protected int containerZ;
	protected ItemStack hireCost = null; // Null if not for hire

	// Random shopkeeper-specific starting offset between [1, CHECK_CONTAINER_PERIOD_SECONDS]
	private int remainingCheckContainerSeconds = (int) (Math.random() * CHECK_CONTAINER_PERIOD_SECONDS) + 1;

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

		this.ownerUUID = owner.getUniqueId();
		this.ownerName = owner.getName();
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
	protected void loadFromSaveData(ConfigurationSection configSection) throws ShopkeeperCreateException {
		super.loadFromSaveData(configSection);
		try {
			ownerUUID = UUID.fromString(configSection.getString("owner uuid"));
		} catch (Exception e) {
			// UUID is invalid or non-existent:
			throw new ShopkeeperCreateException("Missing owner uuid!");
		}
		ownerName = configSection.getString("owner");
		// TODO We no longer use the fallback name (since late 1.14.4). Remove the "unknown"-check again in the future
		// (as soon as possible, because it conflicts with any player actually named 'unknown').
		if (ownerName == null || ownerName.isEmpty() || ownerName.equals("unknown")) {
			throw new ShopkeeperCreateException("Missing owner name!");
		}

		if (!configSection.isInt("chestx") || !configSection.isInt("chesty") || !configSection.isInt("chestz")) {
			throw new ShopkeeperCreateException("Missing container coordinate(s)");
		}

		// Update container:
		// TODO Rename to storage keys to containerx/y/z?
		this._setContainer(configSection.getInt("chestx"), configSection.getInt("chesty"), configSection.getInt("chestz"));

		hireCost = configSection.getItemStack("hirecost");
		// Hire cost ItemStack is not null, but empty. -> Normalize to null:
		if (hireCost != null && ItemUtils.isEmpty(hireCost)) {
			Log.warning("Invalid (empty) hire cost! Disabling 'for hire' for shopkeeper at " + this.getPositionString());
			hireCost = null;
			this.markDirty();
		}
		ItemStack migratedHireCost = ItemUtils.migrateItemStack(hireCost);
		if (!ItemUtils.isSimilar(hireCost, migratedHireCost)) {
			if (ItemUtils.isEmpty(migratedHireCost) && !ItemUtils.isEmpty(hireCost)) {
				// Migration failed:
				Log.warning("Shopkeeper " + this.getId() + ": Hire cost item migration failed: " + hireCost.toString());
				hireCost = null;
			} else {
				hireCost = migratedHireCost;
				Log.debug(Settings.DebugOptions.itemMigrations,
						() -> "Shopkeeper " + this.getId() + ": Migrated hire cost item."
				);
			}
			this.markDirty();
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("owner uuid", ownerUUID.toString());
		configSection.set("owner", ownerName);
		configSection.set("chestx", containerX);
		configSection.set("chesty", containerY);
		configSection.set("chestz", containerZ);
		if (hireCost != null) {
			configSection.set("hirecost", hireCost);
		}
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
				Location shopLocation = this.getShopObject().getLocation();
				// If within a certain range, drop the item at the shop's location, else drop at player's location:
				Location dropLocation;
				if (shopLocation != null && Utils.getDistanceSquared(shopLocation, playerLocation) <= 100) {
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
	public void onPlayerInteraction(Player player) {
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

	@Override
	public void setOwner(Player player) {
		this.setOwner(player.getUniqueId(), player.getName());
	}

	@Override
	public void setOwner(UUID ownerUUID, String ownerName) {
		Validate.notNull(ownerUUID, "Owner uuid is null!");
		Validate.notEmpty(ownerName, "Owner name is empty!");
		this.markDirty();
		this.ownerUUID = ownerUUID;
		this.ownerName = ownerName;
		// TODO Do this in a more abstract way.
		if (!Settings.allowRenamingOfPlayerNpcShops && this.getShopObject().getType() == DefaultShopObjectTypes.CITIZEN()) {
			// Update the NPC's name:
			((SKCitizensShopObject) this.getShopObject()).setName(ownerName);
		} else if (this.getShopObject().getType() == DefaultShopObjectTypes.SIGN()) {
			// Update sign:
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
	public String getOwnerString() {
		return TextUtils.getPlayerString(ownerName, ownerUUID);
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
			// Disable hiring:
			this.hireCost = null;
			this.setName("");
		} else {
			// Set for hire:
			this.hireCost = hireCost.clone();
			this.setName(Messages.forHireTitle);
		}
	}

	@Override
	public ItemStack getHireCost() {
		return (this.isForHire() ? hireCost.clone() : null);
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

	@Deprecated
	@Override
	public int getChestX() {
		return this.getContainerX();
	}

	@Deprecated
	@Override
	public int getChestY() {
		return this.getContainerY();
	}

	@Deprecated
	@Override
	public int getChestZ() {
		return this.getContainerZ();
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

	@Deprecated
	@Override
	public void setChest(int containerX, int containerY, int containerZ) {
		this.setContainer(containerX, containerY, containerZ);
	}

	@Override
	public void setContainer(int containerX, int containerY, int containerZ) {
		this._setContainer(containerX, containerY, containerZ);
		this.markDirty();
	}

	@Deprecated
	@Override
	public Block getChest() {
		return this.getContainer();
	}

	@Override
	public Block getContainer() {
		return Bukkit.getWorld(this.getWorldName()).getBlockAt(containerX, containerY, containerZ);
	}

	// Returns null (and logs a warning) if the price cannot be represented correctly by currency items.
	protected TradingRecipe createSellingRecipe(ItemStack itemBeingSold, int price, boolean outOfStock) {
		int remainingPrice = price;

		ItemStack item1 = null;
		ItemStack item2 = null;

		if (Settings.isHighCurrencyEnabled() && price > Settings.highCurrencyMinCost) {
			int highCurrencyAmount = Math.min(price / Settings.highCurrencyValue, Settings.highCurrencyItem.getType().getMaxStackSize());
			if (highCurrencyAmount > 0) {
				remainingPrice -= (highCurrencyAmount * Settings.highCurrencyValue);
				ItemStack highCurrencyItem = Settings.createHighCurrencyItem(highCurrencyAmount);
				item1 = highCurrencyItem; // Using the first slot
			}
		}

		if (remainingPrice > 0) {
			if (remainingPrice > Settings.currencyItem.getType().getMaxStackSize()) {
				// Cannot represent this price with the used currency items:
				Log.warning("Shopkeeper " + this.getIdString() + " at " + this.getPositionString()
						+ " owned by " + this.getOwnerString() + " has an invalid cost!");
				return null;
			}

			ItemStack currencyItem = Settings.createCurrencyItem(remainingPrice);
			if (item1 == null) {
				item1 = currencyItem;
			} else {
				// The first item of the trading recipe is already used by the high currency item:
				item2 = currencyItem;
			}
		}
		return ShopkeepersAPI.createTradingRecipe(itemBeingSold, item1, item2, outOfStock);
	}

	// Returns null (and logs a warning) if the price cannot be represented correctly by currency items.
	protected TradingRecipe createBuyingRecipe(ItemStack itemBeingBought, int price, boolean outOfStock) {
		if (price > Settings.currencyItem.getType().getMaxStackSize()) {
			// Cannot represent this price with the used currency items:
			Log.warning("Shopkeeper " + this.getIdString() + " at " + this.getPositionString()
					+ " owned by " + this.getOwnerString() + " has an invalid cost!");
			return null;
		}
		ItemStack currencyItem = Settings.createCurrencyItem(price);
		return ShopkeepersAPI.createTradingRecipe(currencyItem, itemBeingBought, null, outOfStock);
	}

	@Deprecated
	@Override
	public int getCurrencyInChest() {
		return this.getCurrencyInContainer();
	}

	@Override
	public int getCurrencyInContainer() {
		Block container = this.getContainer();
		if (!ShopContainers.isSupportedContainer(container.getType())) {
			return 0;
		}

		int totalCurrency = 0;
		Inventory inventory = ShopContainers.getInventory(container);
		ItemStack[] contents = inventory.getContents();
		for (ItemStack itemStack : contents) {
			if (Settings.isCurrencyItem(itemStack)) {
				totalCurrency += itemStack.getAmount();
			} else if (Settings.isHighCurrencyItem(itemStack)) {
				totalCurrency += (itemStack.getAmount() * Settings.highCurrencyValue);
			}
		}
		return totalCurrency;
	}

	protected List<ItemCount> getItemsFromContainer(Filter<ItemStack> filter) {
		ItemStack[] contents = null;
		Block container = this.getContainer();
		if (ShopContainers.isSupportedContainer(container.getType())) {
			Inventory inventory = ShopContainers.getInventory(container);
			contents = inventory.getContents();
		}
		// Returns an empty list if the container could not be found:
		return ItemUtils.countItems(contents, filter);
	}

	// SHOPKEEPER UIs - Shortcuts for common UI types:

	@Override
	public boolean openHireWindow(Player player) {
		return this.openWindow(DefaultUITypes.HIRING(), player);
	}

	@Deprecated
	@Override
	public boolean openChestWindow(Player player) {
		return this.openContainerWindow(player);
	}

	@Override
	public boolean openContainerWindow(Player player) {
		// Check if the container still exists:
		Block container = this.getContainer();
		if (!ShopContainers.isSupportedContainer(container.getType())) {
			Log.debug(() -> "Cannot open container inventory for player '" + player.getName()
					+ "': The block is no longer a valid container!");
			return false;
		}

		Log.debug(() -> "Opening container inventory for player '" + player.getName() + "'.");
		// Open the container directly for the player (no need for a custom UI):
		Inventory inventory = ShopContainers.getInventory(container);
		player.openInventory(inventory);
		return true;
	}

	// TICKING

	@Override
	public void tick() {
		// Delete the shopkeeper if the container is no longer present (eg. if it got removed externally by another
		// plugin, such as WorldEdit, etc.):
		if (Settings.deleteShopkeeperOnBreakContainer) {
			remainingCheckContainerSeconds--;
			if (remainingCheckContainerSeconds <= 0) {
				remainingCheckContainerSeconds = CHECK_CONTAINER_PERIOD_SECONDS;
				// This checks if the block is still a valid container:
				Block containerBlock = this.getContainer();
				if (!ShopContainers.isSupportedContainer(containerBlock.getType())) {
					SKShopkeepersPlugin.getInstance().getRemoveShopOnContainerBreak().handleBlockBreakage(containerBlock);
				}
			}
		}
	}
}
