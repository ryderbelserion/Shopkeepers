package com.nisovin.shopkeepers.api.shopobjects.living;

import java.util.Collection;
import java.util.List;

import org.bukkit.entity.EntityType;

import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;

public interface LivingEntityObjectTypes {

	public List<String> getAliases(EntityType entityType);

	public Collection<? extends ShopObjectType<?>> getAllObjectTypes();

	public ShopObjectType<?> getObjectType(EntityType entityType);
}
