package com.nisovin.shopkeepers.api.shopobjects.living;

import java.util.Collection;
import java.util.List;

import org.bukkit.entity.EntityType;

public interface LivingShopObjectTypes {

	public List<String> getAliases(EntityType entityType);

	public Collection<? extends LivingShopObjectType<?>> getAll();

	public LivingShopObjectType<?> get(EntityType entityType);
}
