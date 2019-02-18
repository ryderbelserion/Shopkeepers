package com.nisovin.shopkeepers.shopobjects.living;

import java.util.List;

import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.api.shopobjects.living.LivingShopObjectType;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.entity.AbstractEntityShopObjectType;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public abstract class SKLivingShopObjectType<T extends SKLivingShopObject> extends AbstractEntityShopObjectType<T> implements LivingShopObjectType<T> {

	protected final LivingShops livingShops;
	protected final EntityType entityType;
	protected final List<String> aliases; // unmodifiable, not null, might be empty

	protected SKLivingShopObjectType(LivingShops livingShops, EntityType entityType, List<String> aliases, String identifier, String permission) {
		super(identifier, permission);
		this.livingShops = livingShops;
		this.entityType = entityType;
		assert entityType.isAlive();
		assert aliases != null;
		// assert: aliases are normalized
		this.aliases = aliases;
	}

	@Override
	public EntityType getEntityType() {
		return entityType;
	}

	@Override
	public boolean hasPermission(Player player) {
		return super.hasPermission(player) || Utils.hasPermission(player, "shopkeeper.entity.*");
	}

	@Override
	public String getDisplayName() {
		// TODO translation support for the entity type name?
		return Utils.replaceArgs(Settings.msgShopObjectTypeLiving,
				"{type}", StringUtils.normalize(entityType.name()));
	}

	@Override
	public abstract T createObject(AbstractShopkeeper shopkeeper, ShopCreationData creationData);

	@Override
	public boolean isEnabled() {
		return Settings.enabledLivingShops.contains(entityType.name());
	}

	@Override
	public boolean matches(String identifier) {
		identifier = StringUtils.normalize(identifier);
		if (super.matches(identifier)) return true;
		for (String alias : aliases) {
			if (identifier.startsWith(alias)) return true;
		}
		return false;
	}

	@Override
	public boolean needsSpawning() {
		return true; // despawn shop entities on chunk unload, and spawn them again on chunk load
	}

	@Override
	public boolean despawnDuringWorldSaves() {
		// spawned entities are non-persistent and therefore already skipped during world saves:
		return false;
	}
}
