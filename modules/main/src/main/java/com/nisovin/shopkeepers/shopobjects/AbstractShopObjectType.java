package com.nisovin.shopkeepers.shopobjects;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopkeeper.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.types.AbstractSelectableType;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public abstract class AbstractShopObjectType<T extends AbstractShopObject> extends AbstractSelectableType implements ShopObjectType<T> {

	private final Class<T> shopObjectClass;

	protected AbstractShopObjectType(String identifier, List<String> aliases, String permission, Class<T> shopObjectClass) {
		super(identifier, aliases, permission);
		Validate.notNull(shopObjectClass, "shopObjectClass is null");
		this.shopObjectClass = shopObjectClass;
	}

	/**
	 * Gets the concrete and most specific class of the shop objects that are created by this {@link ShopObjectType}.
	 * 
	 * @return the concrete shop object class, not <code>null</code>
	 */
	public final Class<T> getShopObjectClass() {
		return shopObjectClass;
	}

	@Override
	protected void onSelect(Player player) {
		TextUtils.sendMessage(player, Messages.selectedShopObjectType, "type", this.getDisplayName());
	}

	/**
	 * Whether or not the spawning and despawning of shop objects of this type is managed by the Shopkeepers plugin.
	 * <p>
	 * Some types of shop objects may take care of the spawning and despawning of their shop objects themselves. If this
	 * returns <code>true</code>, the shop objects will be {@link AbstractShopObject#spawn() spawned} and
	 * {@link AbstractShopObject#despawn() despawned} with chunk loads and unloads (more accurately: with
	 * {@link ShopkeeperRegistry#getActiveChunks(String) chunk activations}).
	 * <p>
	 * The return value of this method is expected to be fixed.
	 * 
	 * @return <code>true</code> if the spawning and despawning of shop objects of this type is managed by the
	 *         Shopkeepers plugin
	 */
	public abstract boolean mustBeSpawned();

	/**
	 * Whether or not shop objects of this type shall be despawned right before world saves and respawned afterwards.
	 * <p>
	 * The return value of this method is only used if {@link #mustBeSpawned()} returns <code>true</code>.
	 * <p>
	 * The return value of this method is expected to be fixed.
	 * 
	 * @return <code>true</code> if shop objects of this type get despawned during world saves
	 */
	public boolean mustDespawnDuringWorldSave() {
		return this.mustBeSpawned();
	}

	@Override
	public boolean isValidSpawnLocation(Location spawnLocation, BlockFace targetedBlockFace) {
		// TODO Check actual object size?
		if (spawnLocation == null || spawnLocation.getWorld() == null) return false;
		Block spawnBlock = spawnLocation.getBlock();
		if (!spawnBlock.isPassable()) return false;
		if (targetedBlockFace != null) {
			// TODO DOWN might be a valid block face for some shop types (in the future). However, it certainly is not
			// for sign and entity shops (the current by default available types).
			if (targetedBlockFace == BlockFace.DOWN || !BlockFaceUtils.isBlockSide(targetedBlockFace)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Creates a shop object of this type.
	 * 
	 * @param shopkeeper
	 *            the shopkeeper, not <code>null</code>
	 * @param creationData
	 *            the used shop creation data, can be <code>null</code> (for ex. if the shopkeeper gets loaded)
	 * @return the shop object
	 */
	public abstract T createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData);

	/**
	 * Gets the active (i.e. ticking) {@link Shopkeeper} for the given object id that uses a {@link ShopObject} of this
	 * type.
	 * 
	 * @param objectId
	 *            the object id
	 * @return the shopkeeper, or <code>null</code> if no such shopkeeper is found, or if the shopkeeper is not using
	 *         this type of shop object
	 */
	protected AbstractShopkeeper getShopkeeperByObjectId(Object objectId) {
		SKShopkeeperRegistry shopkeeperRegistry = SKShopkeepersPlugin.getInstance().getShopkeeperRegistry();
		AbstractShopkeeper shopkeeper = shopkeeperRegistry.getActiveShopkeeper(objectId);
		if (shopkeeper != null && shopkeeper.getShopObject().getType() == this) {
			return shopkeeper;
		} else {
			return null;
		}
	}
}
