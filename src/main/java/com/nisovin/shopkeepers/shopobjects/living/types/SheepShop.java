package com.nisovin.shopkeepers.shopobjects.living.types;

import org.bukkit.DyeColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class SheepShop extends SKLivingShopObject {

	private DyeColor color = DyeColor.WHITE; // default white

	public SheepShop(	LivingShops livingShops, SKLivingShopObjectType<SheepShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.loadColor(configSection);
	}

	private void loadColor(ConfigurationSection configSection) {
		String colorName = configSection.getString("color");
		DyeColor color = parseColor(colorName);
		if (color == null) {
			// fallback to default white:
			Log.warning("Missing or invalid sheep color '" + colorName + "' for shopkeeper " + shopkeeper.getId()
					+ ". Using '" + DyeColor.WHITE + "' now.");
			color = DyeColor.WHITE;
			shopkeeper.markDirty();
		}
		this.color = color;
	}

	private static DyeColor parseColor(String colorName) {
		if (colorName != null) {
			try {
				return DyeColor.valueOf(colorName);
			} catch (IllegalArgumentException e) {
			}
		}
		return null;
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("color", color.name());
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
		return new ItemStack(ItemUtils.getWoolType(color), 1);
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		color = Utils.getNextEnumConstant(DyeColor.class, color);
		assert color != null;
		this.applySubType();
	}
}
