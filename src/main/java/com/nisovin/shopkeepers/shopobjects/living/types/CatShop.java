package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.Locale;

import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Cat;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class CatShop extends SKLivingShopObject {

	private Cat.Type catType = Cat.Type.TABBY;

	public CatShop(	LivingShops livingShops, SKLivingShopObjectType<CatShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		String catTypeName = configSection.getString("catType");
		try {
			catType = Cat.Type.valueOf(catTypeName);
		} catch (Exception e) {
			// fallback:
			Log.warning("Missing or invalid cat type '" + catTypeName + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + Cat.Type.TABBY + "' now.");
			this.catType = Cat.Type.TABBY;
			shopkeeper.markDirty();
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("catType", catType.name());
	}

	@Override
	public Cat getEntity() {
		assert super.getEntity().getType() == EntityType.CAT;
		return (Cat) super.getEntity();
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
		ItemStack item = new ItemStack(Material.LEATHER_CHESTPLATE);
		// TODO translations?
		String catTypeName = StringUtils.capitalizeAll(catType.name().toLowerCase(Locale.ROOT).replace('_', ' '));
		ItemUtils.setItemStackNameAndLore(item, ChatColor.GOLD + catTypeName, null);
		switch (catType) {
		case ALL_BLACK:
			ItemUtils.setLeatherColor(item, Color.BLACK);
			break;
		case BLACK:
			ItemUtils.setLeatherColor(item, Color.BLACK.mixColors(Color.GRAY));
			break;
		case BRITISH_SHORTHAIR:
			ItemUtils.setLeatherColor(item, Color.SILVER);
			break;
		case CALICO:
			ItemUtils.setLeatherColor(item, Color.ORANGE.mixColors(Color.MAROON));
			break;
		case JELLIE:
			ItemUtils.setLeatherColor(item, Color.GRAY);
			break;
		case PERSIAN:
			ItemUtils.setLeatherColor(item, Color.WHITE.mixColors(Color.ORANGE));
			break;
		case RAGDOLL:
			ItemUtils.setLeatherColor(item, Color.WHITE.mixColors(Color.MAROON));
			break;
		case RED:
			ItemUtils.setLeatherColor(item, Color.ORANGE);
			break;
		case SIAMESE:
			ItemUtils.setLeatherColor(item, Color.MAROON.mixColors(Color.GRAY));
			break;
		case WHITE:
			ItemUtils.setLeatherColor(item, Color.WHITE);
			break;
		case TABBY:
		default:
			ItemUtils.setLeatherColor(item, Color.BLACK.mixColors(Color.ORANGE));
			break;
		}
		return item;
	}

	@Override
	public void cycleSubType() {
		shopkeeper.markDirty();
		catType = Utils.getNextEnumConstant(Cat.Type.class, catType);
		assert catType != null;
		this.applySubType();
	}

	// MC 1.14: conversion from ocelot types to similar cat types:
	public static Cat.Type fromOcelotType(String ocelotType) {
		if (ocelotType == null) ocelotType = "WILD_OCELOT"; // default ocelot type
		switch (ocelotType) {
		case "BLACK_CAT":
			return Cat.Type.BLACK;
		case "RED_CAT":
			return Cat.Type.RED;
		case "SIAMESE_CAT":
			return Cat.Type.SIAMESE;
		case "WILD_OCELOT":
		default:
			return Cat.Type.TABBY; // fallback to default
		}
	}
}
