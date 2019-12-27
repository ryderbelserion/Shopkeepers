package com.nisovin.shopkeepers.shopobjects.living;

import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.util.PermissionUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public abstract class SKLivingShopObjectType<T extends SKLivingShopObject<?>> extends AbstractEntityShopObjectType<T> implements LivingShopObjectType<T> {

	protected final LivingShops livingShops;
	protected final EntityType entityType;

	protected SKLivingShopObjectType(LivingShops livingShops, EntityType entityType, List<String> aliases, String identifier, String permission) {
		super(identifier, aliases, permission);
		this.livingShops = livingShops;
		this.entityType = entityType;
		assert entityType.isAlive();
	}

	@Override
	public EntityType getEntityType() {
		return entityType;
	}

	@Override
	public boolean hasPermission(Player player) {
		return super.hasPermission(player) || PermissionUtils.hasPermission(player, "shopkeeper.entity.*");
	}

	@Override
	public String getDisplayName() {
		// TODO translation support for the entity type name?
		return TextUtils.replaceArguments(Settings.msgShopObjectTypeLiving, "type", StringUtils.normalize(entityType.name()));
	}

	@Override
	public abstract T createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData);

	@Override
	public boolean isEnabled() {
		return Settings.enabledLivingShops.contains(entityType.name());
	}
}
