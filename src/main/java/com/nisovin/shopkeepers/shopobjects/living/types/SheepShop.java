package com.nisovin.shopkeepers.shopobjects.living.types;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityObjectType;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShop;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityShops;

public class SheepShop extends LivingEntityShop {

	private DyeColor color = DyeColor.WHITE; // default white

	public SheepShop(	LivingEntityShops livingEntityShops, LivingEntityObjectType<SheepShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingEntityShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.color = DyeColor.getByWoolData((byte) configSection.getInt("color"));
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("color", color.getWoolData());
	}

	@Override
	public Sheep getEntity() {
		assert super.getEntity().getType() == EntityType.SHEEP;
		return (Sheep) super.getEntity();
	}

	// SUB TYPES

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		this.getEntity().setColor(color);
	}

	@Override
	public ItemStack getSubTypeItem() {
		return new ItemStack(Material.WOOL, 1, color.getWoolData());
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		byte colorByte = color.getWoolData();
		colorByte += 1;
		color = DyeColor.getByWoolData(colorByte);
		if (color == null) {
			color = DyeColor.WHITE;
		}
		this.applySubType();
	}
}
