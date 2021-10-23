package com.nisovin.shopkeepers.shopobjects.living;

import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectType;
import com.nisovin.shopkeepers.config.Settings.DerivedSettings;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.util.bukkit.PermissionUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class SKLivingShopObjectType<T extends SKLivingShopObject<?>> extends AbstractEntityShopObjectType<T> implements LivingShopObjectType<T> {

	@FunctionalInterface
	public static interface ShopObjectConstructor<T extends SKLivingShopObject<?>> {

		public T create(LivingShops livingShops, SKLivingShopObjectType<T> shopObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData);
	}

	private static final String PERMISSION_ALL_ENTITY_TYPES = "shopkeeper.entity.*";

	protected final LivingShops livingShops;
	protected final EntityType entityType;
	private final ShopObjectConstructor<T> shopObjectConstructor;

	protected SKLivingShopObjectType(	LivingShops livingShops, EntityType entityType, String identifier,
										List<String> aliases, String permission,
										ShopObjectConstructor<T> shopObjectConstructor) {
		super(identifier, aliases, permission);
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
		return PermissionUtils.hasPermission(player, PERMISSION_ALL_ENTITY_TYPES) || super.hasPermission(player);
	}

	@Override
	public String getDisplayName() {
		// TODO Translation support for the entity type name?
		return StringUtils.replaceArguments(Messages.shopObjectTypeLiving, "type", StringUtils.normalize(entityType.name()));
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

	@Override
	public final T createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		T shopObject = shopObjectConstructor.create(livingShops, this, shopkeeper, creationData);
		Validate.State.notNull(shopObject, () -> "SKLivingShopObjectType for entity type '" + entityType + "' created null shop object!");
		return shopObject;
	}
}
