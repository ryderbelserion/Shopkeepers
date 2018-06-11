package com.nisovin.shopkeepers.shopobjects.living;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.AbstractShopkeeper;
import com.nisovin.shopkeepers.api.ShopCreationData;

public class SheepShop extends LivingEntityShop {

	private DyeColor color = DyeColor.WHITE; // default white

	protected SheepShop(LivingEntityObjectType<SheepShop> livingObjectType, AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingObjectType, shopkeeper, creationData);
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

	// SUB TYPES

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		assert entity.getType() == EntityType.SHEEP;
		((Sheep) entity).setColor(color);
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
