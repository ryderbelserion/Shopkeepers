package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.PigZombie;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.api.ShopCreationData;

public class PigZombieShop extends LivingEntityShop {

	private boolean baby = false;

	protected PigZombieShop(LivingEntityObjectType<PigZombieShop> livingObjectType, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection config) {
		super.load(config);
		baby = config.getBoolean("baby");
	}

	@Override
	public void save(ConfigurationSection config) {
		super.save(config);
		config.set("baby", baby);
	}

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		assert entity.getType() == EntityType.PIG_ZOMBIE;
		((PigZombie) entity).setBaby(baby);
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.MONSTER_EGG, 1, (short) 57);
	}

	@Override
	public void cycleSubType() {
		baby = !baby;
		this.applySubType();
	}
}
