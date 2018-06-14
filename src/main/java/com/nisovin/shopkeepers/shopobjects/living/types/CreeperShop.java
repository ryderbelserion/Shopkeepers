package com.nisovin.shopkeepers.shopobjects.living.types;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityObjectType;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShop;

public class CreeperShop extends LivingEntityShop {

	private boolean powered = false;

	public CreeperShop(LivingEntityObjectType<CreeperShop> livingObjectType, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		powered = configSection.getBoolean("powered");
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("powered", powered);
	}

	// SUB TYPES

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		assert entity.getType() == EntityType.CREEPER;
		((Creeper) entity).setPowered(powered);
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.WOOL, 1, powered ? (short) 3 : (short) 5);
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		powered = !powered;
		this.applySubType();
	}
}
