package com.nisovin.shopkeepers.shopobjects.living.types;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Ocelot;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class CatShop extends SKLivingShopObject {

	private Ocelot.Type catType = Ocelot.Type.WILD_OCELOT;

	public CatShop(	LivingShops livingShops, SKLivingShopObjectType<CatShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		String catTypeName = configSection.getString("catType");
		try {
			catType = Ocelot.Type.valueOf(catTypeName);
		} catch (Exception e) {
			// fallback:
			Log.warning("Missing or invalid cat type '" + catTypeName + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + Ocelot.Type.WILD_OCELOT + "' now.");
			this.catType = Ocelot.Type.WILD_OCELOT;
			shopkeeper.markDirty();
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("catType", catType.name());
	}

	@Override
	public Ocelot getEntity() {
		assert super.getEntity().getType() == EntityType.OCELOT;
		return (Ocelot) super.getEntity();
	}

	// SUB TYPES

	@Override
	protected void applySubType() {
		super.applySubType();
		if (!this.isActive()) return;
		this.getEntity().setCatType(catType);
	}

	@Override
	public ItemStack getSubTypeItem() {
		switch (catType) {
		case BLACK_CAT:
			return new ItemStack(Material.BLACK_WOOL, 1);
		case RED_CAT:
			return new ItemStack(Material.RED_WOOL, 1);
		case SIAMESE_CAT:
			return new ItemStack(Material.LIGHT_GRAY_WOOL, 1);
		case WILD_OCELOT:
		default:
			return new ItemStack(Material.ORANGE_WOOL, 1);
		}
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		catType = Utils.getNextEnumConstant(Ocelot.Type.class, catType);
		assert catType != null;
		this.applySubType();
	}
}
