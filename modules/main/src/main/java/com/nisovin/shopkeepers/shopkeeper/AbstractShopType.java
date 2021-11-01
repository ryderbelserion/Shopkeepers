package com.nisovin.shopkeepers.shopkeeper;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.events.PlayerCreateShopkeeperEvent;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperCreateException;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.types.AbstractSelectableType;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class AbstractShopType<T extends AbstractShopkeeper> extends AbstractSelectableType implements ShopType<T> {

	protected AbstractShopType(String identifier, List<String> aliases, String permission) {
		super(identifier, aliases, permission);
	}

	@Override
	protected void onSelect(Player player) {
		TextUtils.sendMessage(player, Messages.selectedShopType,
				"type", this.getDisplayName(),
				"description", this.getDescription()
		);
	}

	protected Text getCreatedMessage() {
		Text text = Messages.shopkeeperCreated;
		text.setPlaceholderArguments(
				"type", this.getDisplayName(),
				"description", this.getDescription(),
				"setupDesc", this.getSetupDescription()
		);
		return text;
	}

	/**
	 * Recreates a shopkeeper of this type by loading its previously saved data from the given {@link ShopkeeperData}.
	 * <p>
	 * In the course of data migrations, this operation may modify the given {@link ShopkeeperData} and its sub data
	 * containers. If this is the case, the loaded shopkeeper needs to be marked as
	 * {@link AbstractShopkeeper#markDirty() dirty} in order for the storage to be made aware of these changes.
	 * <p>
	 * Any stored data elements (such as for example item stacks, etc.) and collections of data elements are assumed to
	 * not be modified, neither by the loaded shopkeeper, nor in contexts outside of the loaded shopkeeper. If the
	 * loaded shopkeeper can guarantee not to modify these data elements, it is allowed to directly store them without
	 * copying them first.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param shopkeeperData
	 *            the shopkeeper data
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created (ex. due to invalid or missing data)
	 */
	public abstract T loadShopkeeper(int id, ShopkeeperData shopkeeperData) throws ShopkeeperCreateException;

	/**
	 * Creates a new shopkeeper of this type by using the data from the given {@link ShopCreationData}.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param shopCreationData
	 *            the shop creation data
	 * @return the created shopkeeper
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper could not be created (ex. due to invalid or missing data)
	 */
	public abstract T createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException;

	// Handles the shopkeeper creation by players.
	// Returns null in case of failure.
	public T handleShopkeeperCreation(ShopCreationData shopCreationData) {
		this.validateCreationData(shopCreationData);
		SKShopkeeperRegistry shopkeeperRegistry = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry();

		// The creator, can not be null when creating a shopkeeper via this method:
		Player creator = shopCreationData.getCreator();
		Validate.notNull(creator, "Creator of shopCreationData is null");

		ShopType<?> shopType = shopCreationData.getShopType();
		ShopObjectType<?> shopObjectType = shopCreationData.getShopObjectType();

		// Can the selected shop type be used?
		if (!shopType.hasPermission(creator)) {
			TextUtils.sendMessage(creator, Messages.noPermission);
			return null;
		}
		if (!shopType.isEnabled()) {
			TextUtils.sendMessage(creator, Messages.shopTypeDisabled, "type", shopType.getIdentifier());
			return null;
		}

		// Can the selected shop object type be used?
		if (!shopObjectType.hasPermission(creator)) {
			TextUtils.sendMessage(creator, Messages.noPermission);
			return null;
		}
		if (!shopObjectType.isEnabled()) {
			TextUtils.sendMessage(creator, Messages.shopObjectTypeDisabled, "type", shopObjectType.getIdentifier());
			return null;
		}

		Location spawnLocation = shopCreationData.getSpawnLocation(); // Can be null for virtual shops
		BlockFace targetedBlockFace = shopCreationData.getTargetedBlockFace(); // Can be null

		// Check if the shop can be placed there (enough space, etc.):
		if (!shopObjectType.isValidSpawnLocation(spawnLocation, targetedBlockFace)) {
			// Invalid spawn location or targeted block face:
			TextUtils.sendMessage(creator, Messages.shopCreateFail);
			return null;
		}

		if (spawnLocation != null && !shopkeeperRegistry.getShopkeepersAtLocation(spawnLocation).isEmpty()) {
			// There is already a shopkeeper at that location:
			TextUtils.sendMessage(creator, Messages.shopCreateFail);
			return null;
		}

		try {
			// Shop type specific handling:
			if (!this.handleSpecificShopkeeperCreation(shopCreationData)) {
				return null;
			}

			// Create and spawn the shopkeeper:
			@SuppressWarnings("unchecked")
			T shopkeeper = (T) shopkeeperRegistry.createShopkeeper(shopCreationData);
			assert shopkeeper != null;

			// Send creation message to creator:
			TextUtils.sendMessage(creator, this.getCreatedMessage());

			// Save:
			shopkeeper.save();

			return shopkeeper;
		} catch (ShopkeeperCreateException e) {
			// Some issue identified during shopkeeper creation (possibly hinting to a bug):
			// TODO Translation?
			TextUtils.sendMessage(creator, ChatColor.RED + "Shopkeeper creation failed: " + e.getMessage());
			return null;
		}
	}

	// Shop type specific handling of shopkeeper creation by players.
	// Returns null in case of failure.
	protected boolean handleSpecificShopkeeperCreation(ShopCreationData creationData) {
		// Call event:
		PlayerCreateShopkeeperEvent createEvent = new PlayerCreateShopkeeperEvent(creationData);
		Bukkit.getPluginManager().callEvent(createEvent);
		if (createEvent.isCancelled()) {
			Log.debug("ShopkeeperCreateEvent was cancelled!");
			return false;
		}
		return true;
	}

	// Common functions that might be useful for sub-classes:

	protected void validateCreationData(ShopCreationData shopCreationData) {
		Validate.notNull(shopCreationData, "shopCreationData is null");
		ShopType<?> shopType = shopCreationData.getShopType();
		Validate.isTrue(this == shopType, "ShopType of shopCreationData is not of type " + this.getClass().getName()
				+ ", but: " + shopType.getClass().getName());
	}

	protected void validateShopkeeperData(ShopkeeperData shopkeeperData) {
		Validate.notNull(shopkeeperData, "shopkeeperData is null");
	}
}
