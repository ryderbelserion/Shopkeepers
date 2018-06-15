package com.nisovin.shopkeepers.shopobjects.living.types;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Zombie;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityObjectType;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShop;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShops;

public class ZombieShop extends LivingEntityShop {

	private boolean baby = false;

	public ZombieShop(	LivingEntityShops livingEntityShops, LivingEntityObjectType<ZombieShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingEntityShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		baby = configSection.getBoolean("baby");
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("baby", baby);
	}

	@Override
	public Zombie getEntity() {
		assert super.getEntity().getType() == EntityType.ZOMBIE;
		return (Zombie) super.getEntity();
	}

	// SUB TYPES

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		this.getEntity().setBaby(baby);
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.MONSTER_EGG, 1, (short) 54);
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		baby = !baby;
		this.applySubType();
	}
}
