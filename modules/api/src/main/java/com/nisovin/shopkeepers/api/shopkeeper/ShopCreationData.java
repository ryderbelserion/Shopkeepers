package com.nisovin.shopkeepers.api.shopkeeper;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.google.common.base.Preconditions;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.api.shopobjects.virtual.VirtualShopObjectType;

/**
 * Holds the different possible arguments needed for the creation of a shopkeeper of a certain type.
 * <p>
 * Additional data might be available through sub-classes, or when dynamically added via
 * {@link #setValue(String, Object)}.
 */
public abstract class ShopCreationData {

	private final @Nullable Player creator;
	private final ShopType<?> shopType;
	private final ShopObjectType<?> shopObjectType;
	private @Nullable Location spawnLocation; // Modifiable, can be null for virtual shops
	private @Nullable BlockFace targetedBlockFace; // Modifiable

	private @Nullable Map<String, Object> additionalData;

	/**
	 * Creates a {@link ShopCreationData}.
	 * 
	 * @param creator
	 *            the creator, can be <code>null</code>
	 * @param shopType
	 *            the shop type, not <code>null</code>
	 * @param shopObjectType
	 *            the shop object type, not <code>null</code>
	 * @param spawnLocation
	 *            the spawn location, can be <code>null</code> for virtual shops
	 * @param targetedBlockFace
	 *            the targeted block face, can be <code>null</code>
	 */
	protected ShopCreationData(
			@Nullable Player creator,
			ShopType<?> shopType,
			ShopObjectType<?> shopObjectType,
			@Nullable Location spawnLocation,
			@Nullable BlockFace targetedBlockFace
	) {
		Preconditions.checkNotNull(shopType, "shopType is null");
		Preconditions.checkNotNull(shopObjectType, "shopObjectType is null");
		this.creator = creator;
		this.shopType = shopType;
		this.shopObjectType = shopObjectType;
		if (spawnLocation != null) {
			Preconditions.checkNotNull(spawnLocation.getWorld(), "spawnLocation has no world");
			spawnLocation.checkFinite();
			this.spawnLocation = spawnLocation.clone();
		} else {
			Preconditions.checkArgument(shopObjectType instanceof VirtualShopObjectType,
					"spawnLocation is null, but the shop object type is not virtual");
			this.spawnLocation = null;
		}
		this.targetedBlockFace = targetedBlockFace;
	}

	/**
	 * The creator of the shop.
	 * 
	 * @return the creating player, can be <code>null</code> depending on the shop type and the
	 *         context in which it is created
	 */
	public @Nullable Player getCreator() {
		return creator;
	}

	/**
	 * The type of shop to create.
	 * 
	 * @return the shop type, not <code>null</code>
	 */
	public ShopType<?> getShopType() {
		return shopType;
	}

	/**
	 * The object type for the shop.
	 * 
	 * @return the shop object type, not <code>null</code>
	 */
	public ShopObjectType<?> getShopObjectType() {
		return shopObjectType;
	}

	/**
	 * The location the shopkeeper gets created at.
	 * 
	 * @return the spawn location, can be <code>null</code> for virtual shops
	 */
	public @Nullable Location getSpawnLocation() {
		return (spawnLocation != null) ? spawnLocation.clone() : null;
	}

	/**
	 * Sets the spawn location.
	 * <p>
	 * Has to be located in the same world as the previous spawn location.
	 * 
	 * @param newSpawnLocation
	 *            the new spawn location, can be <code>null</code> for virtual shops
	 */
	public void setSpawnLocation(@Nullable Location newSpawnLocation) {
		if (!(shopObjectType instanceof VirtualShopObjectType)) {
			Preconditions.checkNotNull(newSpawnLocation,
					"newSpawnLocation is null, but the shop object type is not virtual");
		}
		if (newSpawnLocation == null) {
			this.spawnLocation = null;
		} else {
			Preconditions.checkNotNull(
					newSpawnLocation.getWorld(),
					"newSpawnLocation has no world"
			);
			newSpawnLocation.checkFinite();
			if (this.spawnLocation != null) {
				Preconditions.checkArgument(
						this.spawnLocation.getWorld() == newSpawnLocation.getWorld(),
						"Cannot set the spawn location to a different world!"
				);
			}
			this.spawnLocation = newSpawnLocation.clone();
		}
	}

	/**
	 * The block face clicked or targeted during shop creation.
	 * 
	 * @return the targeted block face, can be <code>null</code>
	 */
	public @Nullable BlockFace getTargetedBlockFace() {
		return targetedBlockFace;
	}

	/**
	 * Sets the targeted block face.
	 * 
	 * @param blockFace
	 *            the new block face
	 */
	public void setTargetedBlockFace(@Nullable BlockFace blockFace) {
		this.targetedBlockFace = blockFace;
	}

	/**
	 * Gets a previously set value for the specific key.
	 * 
	 * @param <T>
	 *            the type of the value
	 * @param key
	 *            the key
	 * @return the value, or <code>null</code> if there is no value for the specified key
	 */
	@SuppressWarnings("unchecked")
	public <T> @Nullable T getValue(String key) {
		return (additionalData != null) ? (T) additionalData.get(key) : null;
	}

	/**
	 * Sets a value for the specified key.
	 * 
	 * @param <T>
	 *            the type of the value
	 * @param key
	 *            the key
	 * @param value
	 *            the value, or <code>null</code> to remove the value for the specified key
	 */
	public <T> void setValue(String key, @Nullable T value) {
		if (value == null) {
			if (additionalData != null) {
				additionalData.remove(key);
			}
		} else {
			if (additionalData == null) {
				additionalData = new HashMap<>();
			}
			assert additionalData != null;
			additionalData.put(key, value);
		}
	}
}
