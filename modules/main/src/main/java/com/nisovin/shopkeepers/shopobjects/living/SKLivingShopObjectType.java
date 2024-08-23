package com.nisovin.shopkeepers.shopobjects.living;

import java.util.List;

import org.bukkit.Difficulty;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectType;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class SKLivingShopObjectType<T extends SKLivingShopObject<?>>
		extends AbstractEntityShopObjectType<T> implements LivingShopObjectType<T> {

	@FunctionalInterface
	public static interface ShopObjectConstructor<T extends SKLivingShopObject<?>> {

		public @NonNull T create(
				LivingShops livingShops,
				SKLivingShopObjectType<T> shopObjectType,
				AbstractShopkeeper shopkeeper,
				@Nullable ShopCreationData creationData
		);
	}

	private static final String PERMISSION_ALL_ENTITY_TYPES = "shopkeeper.entity.*";

	protected final LivingShops livingShops;
	protected final EntityType entityType;
	private final ShopObjectConstructor<T> shopObjectConstructor;

	protected SKLivingShopObjectType(
			LivingShops livingShops,
			EntityType entityType,
			String identifier,
			List<? extends String> aliases,
			String permission,
			Class<@NonNull T> shopObjectType,
			ShopObjectConstructor<T> shopObjectConstructor
	) {
		super(identifier, aliases, permission, shopObjectType);
		Validate.isTrue(entityType.isAlive(), "entityType is not alive");
		Validate.isTrue(entityType.isSpawnable(), "entityType is not spawnable");
		Validate.notNull(shopObjectConstructor, "shopObjectConstructor is null");
		this.livingShops = livingShops;
		this.entityType = entityType;
		this.shopObjectConstructor = shopObjectConstructor;
	}

	@Override
	public EntityType getEntityType() {
		return entityType;
	}

	@Override
	public boolean isEnabled() {
		return DerivedSettings.enabledLivingShops.contains(entityType);
	}

	@Override
	public boolean hasPermission(Player player) {
		return PermissionUtils.hasPermission(player, PERMISSION_ALL_ENTITY_TYPES)
				|| super.hasPermission(player);
	}

	@Override
	public String getDisplayName() {
		// TODO Translation support for the entity type name?
		return StringUtils.replaceArguments(Messages.shopObjectTypeLiving,
				"type", StringUtils.normalize(entityType.name())
		);
	}

	@Override
	public boolean mustBeSpawned() {
		return true; // Despawn entities on chunk unload, and spawn them again on chunk load.
	}

	@Override
	public boolean mustDespawnDuringWorldSave() {
		// Spawned entities are non-persistent and therefore already skipped during world saves:
		return false;
	}

	private boolean isDownValidAttachedBlockFace() {
		switch (entityType) {
		case SHULKER:
			return true;
		default:
			return false;
		}
	}

	@Override
	public boolean validateSpawnLocation(
			@Nullable Player creator,
			@Nullable Location spawnLocation,
			@Nullable BlockFace attachedBlockFace
	) {
		if (!super.validateSpawnLocation(creator, spawnLocation, attachedBlockFace)) {
			return false;
		}
		assert spawnLocation != null;

		Block spawnBlock = spawnLocation.getBlock();
		// TODO Require an empty block for shulkers? However, placing a shulker on a non-empty block
		// actually works fine and preserves the block.
		if (!spawnBlock.isPassable()) {
			if (creator != null) {
				TextUtils.sendMessage(creator, Messages.spawnBlockNotEmpty);
			}
			return false;
		}

		if ((attachedBlockFace == BlockFace.DOWN && !this.isDownValidAttachedBlockFace())
				|| (attachedBlockFace != null && !BlockFaceUtils.isBlockSide(attachedBlockFace))) {
			if (creator != null) {
				TextUtils.sendMessage(creator, Messages.invalidSpawnBlockFace);
			}
			return false;
		}

		// Check if the world's difficulty would prevent the mob from spawning:
		if (EntityUtils.isRemovedOnPeacefulDifficulty(entityType)) {
			World world = Unsafe.assertNonNull(spawnLocation.getWorld());
			if (world.getDifficulty() == Difficulty.PEACEFUL) {
				if (creator != null) {
					TextUtils.sendMessage(creator, Messages.mobCannotSpawnOnPeacefulDifficulty);
				}
				return false;
			}
		}
		return true;
	}

	@Override
	public final T createObject(
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		T shopObject = shopObjectConstructor.create(livingShops, this, shopkeeper, creationData);
		Validate.State.notNull(shopObject, () -> "SKLivingShopObjectType for entity type '"
				+ entityType + "' created null shop object!");
		assert shopObject != null;
		return shopObject;
	}
}
