package com.nisovin.shopkeepers.shopobjects.living.types;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Sheep;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
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
		DyeColor color = null;
		String colorInput;
		if (configSection.isInt("color")) {
			// import from pre 1.13 wool data values:
			// TODO remove this again at some point
			Log.info("Importing old sheep color for shopkeeper '" + this.getId() + "'.");
			int woolData = configSection.getInt("color");
			colorInput = String.valueOf(woolData);
			color = DyeColor.getByWoolData((byte) woolData);
			shopkeeper.markDirty();
		} else {
			String colorName = configSection.getString("color");
			colorInput = colorName;
			color = parseColor(colorName);
		}
		if (color == null) {
			// fallback to default white:
			color = DyeColor.WHITE;
			Log.warning("Missing or invalid sheep color '" + colorInput + "' for shopkeeper " + shopkeeper.getId()
					+ ". Using '" + DyeColor.WHITE + "' now.");
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
		return new ItemStack(getWoolType(color), 1);
	}

	// TODO this can be removed again once bukkit provides a non-deprecated mapping itself
	private static Material getWoolType(DyeColor dyeColor) {
		switch (dyeColor) {
		case ORANGE:
			return Material.ORANGE_WOOL;
		case MAGENTA:
			return Material.MAGENTA_WOOL;
		case LIGHT_BLUE:
			return Material.LIGHT_BLUE_WOOL;
		case YELLOW:
			return Material.YELLOW_WOOL;
		case LIME:
			return Material.LIME_WOOL;
		case PINK:
			return Material.PINK_WOOL;
		case GRAY:
			return Material.GRAY_WOOL;
		case SILVER:
			return Material.LIGHT_GRAY_WOOL;
		case CYAN:
			return Material.CYAN_WOOL;
		case PURPLE:
			return Material.PURPLE_WOOL;
		case BLUE:
			return Material.BLUE_WOOL;
		case BROWN:
			return Material.BROWN_WOOL;
		case GREEN:
			return Material.GREEN_WOOL;
		case RED:
			return Material.RED_WOOL;
		case BLACK:
			return Material.BLACK_WOOL;
		case WHITE:
		default:
			return Material.WHITE_WOOL;
		}
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		color = Utils.getNextEnumConstant(DyeColor.class, color);
		assert color != null;
		this.applySubType();
	}
}
