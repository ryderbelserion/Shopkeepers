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
import com.nisovin.shopkeepers.shopobjects.AbstractShopObjectType;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.types.AbstractSelectableType;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.java.Validate;
import com.nisovin.shopkeepers.util.logging.Log;

public abstract class AbstractShopType<T extends AbstractShopkeeper> extends AbstractSelectableType implements ShopType<T> {

	private final Class<T> shopkeeperClass;

	protected AbstractShopType(String identifier, List<String> aliases, String permission, Class<T> shopkeeperClass) {
		super(identifier, aliases, permission);
		Validate.notNull(shopkeeperClass, "shopkeeperClass is null");
		this.shopkeeperClass = shopkeeperClass;
	}

	/**
	 * Gets the concrete and most specific class of the shopkeepers that are created by this {@link ShopType}.
	 * 
	 * @return the concrete shopkeeper class, not <code>null</code>
	 */
	public final Class<T> getShopkeeperClass() {
		return shopkeeperClass;
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
	 * Creates a new and not yet {@link AbstractShopkeeper#isInitialized() initialized} shopkeeper of this type.
	 * 
	 * @return the new shopkeeper, not <code>null</code>
	 */
	private final T createShopkeeper() {
		T shopkeeper = this.createNewShopkeeper();

		// Validate the created shopkeeper:
		if (shopkeeper == null) {
			throw new RuntimeException("ShopType '" + this.getClass().getName() + "' created null shopkeeper!");
		}
		if (shopkeeper.getType() != this) {
			throw new RuntimeException("ShopType '" + this.getClass().getName()
					+ "' created a shopkeeper of a different type (expected: " + this.getIdentifier()
					+ ", got: " + shopkeeper.getType().getIdentifier() + ")!");
		}
		if (shopkeeper.getClass() != this.getShopkeeperClass()) {
			throw new RuntimeException("ShopType '" + this.getClass().getName()
					+ "' created a shopkeeper of unexpected class (expected: "
					+ this.getShopkeeperClass().getName() + ", got: "
					+ shopkeeper.getClass().getName() + ")!");
		}
		if (shopkeeper.isInitialized()) {
			throw new RuntimeException("ShopType '" + this.getClass().getName()
					+ "' created an already initialized shopkeeper!");
		}

		return shopkeeper;
	}

	/**
	 * Creates a new and not yet {@link AbstractShopkeeper#isInitialized() initialized} shopkeeper of this type.
	 * 
	 * @return the new shopkeeper, not <code>null</code>
	 * @see #createShopkeeper()
	 */
	protected abstract T createNewShopkeeper();

	/**
	 * Creates a new shopkeeper of this type based on the given {@link ShopCreationData}.
	 * 
	 * @param id
	 *            the shopkeeper id
	 * @param shopCreationData
	 *            the shop creation data, not <code>null</code>
	 * @return the created shopkeeper, not <code>null</code>
	 * @throws ShopkeeperCreateException
	 *             if the shopkeeper cannot be created (e.g. due to invalid or missing data)
	 */
	public final T createShopkeeper(int id, ShopCreationData shopCreationData) throws ShopkeeperCreateException {
		T shopkeeper = this.createShopkeeper();
		assert shopkeeper != null;
		shopkeeper.initOnCreation(id, shopCreationData);
		return shopkeeper;
	}

	/**
	 * Recreates a shopkeeper of this type by loading its previously saved state from the given {@link ShopkeeperData}.
	 * <p>
	 * The data is expected to already have been {@link ShopkeeperData#migrate(String) migrated}.
	 * <p>
	 * This operation does not modify the given {@link ShopkeeperData}. Any stored data elements (such as for example
	 * item stacks, etc.) and collections of data elements are assumed to not be modified, neither by the loaded
	 * shopkeeper, nor in contexts outside the loaded shopkeeper. If the loaded shopkeeper can guarantee not to modify
	 * these data elements, it is allowed to directly store them without copying them first.
	 * 
	 * @param shopkeeperData
	 *            the shopkeeper data, not <code>null</code>
	 * @return the loaded shopkeeper, not <code>null</code>
	 * @throws InvalidDataException
	 *             if the shopkeeper cannot be loaded (e.g. due to invalid or missing data)
	 */
	public final T loadShopkeeper(ShopkeeperData shopkeeperData) throws InvalidDataException {
		T shopkeeper = this.createShopkeeper();
		assert shopkeeper != null;
		shopkeeper.initOnLoad(shopkeeperData);
		return shopkeeper;
	}

	/**
	 * Checks if the given {@link ShopCreationData} is compatible with this shop type.
	 * <p>
	 * This checks the common basic pre-requirements, such as whether the given shop creation data is of the expected
	 * type. This may not necessarily check if the given shop creation data contains all the information required to
	 * create a valid shopkeeper.
	 * 
	 * @param shopCreationData
	 *            the shop creation data
	 */
	protected void validateCreationData(ShopCreationData shopCreationData) {
		Validate.notNull(shopCreationData, "shopCreationData is null");
		ShopType<?> shopType = shopCreationData.getShopType();
		Validate.isTrue(this == shopType, () -> "ShopType of shopCreationData is not of type "
				+ this.getClass().getName() + ", but: " + shopType.getClass().getName());

		ShopObjectType<?> shopObjectType = shopCreationData.getShopObjectType();
		Validate.isTrue(shopObjectType instanceof AbstractShopObjectType,
				() -> "ShopObjectType of shopCreationData is not of type "
						+ AbstractShopObjectType.class.getSimpleName()
						+ ", but: " + shopType.getClass().getName());
	}

	// Handles the shopkeeper creation by players.
	// Returns null in case of failure.
	public T handleShopkeeperCreation(ShopCreationData shopCreationData) {
		this.validateCreationData(shopCreationData);
		SKShopkeeperRegistry shopkeeperRegistry = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry();

		// The creator, can not be null when creating a shopkeeper via this method:
		Player creator = shopCreationData.getCreator();
		Validate.notNull(creator, "Creator of shopCreationData is null");

		AbstractShopType<?> shopType = this;
		AbstractShopObjectType<?> shopObjectType = (AbstractShopObjectType<?>) shopCreationData.getShopObjectType();

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

		// Validate the spawn location:
		// This is also expected to send feedback to the shop creator if necessary.
		if (!shopObjectType.validateSpawnLocation(creator, spawnLocation, targetedBlockFace)) {
			return null;
		}

		// Check if the location is already used by another shopkeeper:
		if (spawnLocation != null && !shopkeeperRegistry.getShopkeepersAtLocation(spawnLocation).isEmpty()) {
			TextUtils.sendMessage(creator, Messages.locationAlreadyInUse);
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
}
