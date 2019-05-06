package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class CatShop extends BabyableShop<Cat> {

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
	protected void onSpawn(Cat entity) {
		super.onSpawn(entity);
		this.applyCatType(entity);
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.getEditorButtons(); // assumes modifiable
		editorButtons.add(new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getCatTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleCatType();
				return true;
			}
		});
		return editorButtons;
	}

	// CAT TYPE

	public void setCatType(Cat.Type catType) {
		Validate.notNull(catType, "Cat type is null!");
		this.catType = catType;
		shopkeeper.markDirty();
		this.applyCatType(this.getEntity()); // null if not active
	}

	protected void applyCatType(Cat entity) {
		if (entity == null) return;
		entity.setCatType(catType);
	}

	public void cycleCatType() {
		this.setCatType(Utils.getNextEnumConstant(Cat.Type.class, catType));
	}

	protected ItemStack getCatTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (catType) {
		case ALL_BLACK:
			ItemUtils.setLeatherColor(iconItem, Color.BLACK);
			break;
		case BLACK:
			ItemUtils.setLeatherColor(iconItem, Color.BLACK.mixDyes(DyeColor.GRAY));
			break;
		case BRITISH_SHORTHAIR:
			ItemUtils.setLeatherColor(iconItem, Color.SILVER);
			break;
		case CALICO:
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE.mixDyes(DyeColor.BROWN));
			break;
		case JELLIE:
			ItemUtils.setLeatherColor(iconItem, Color.GRAY);
			break;
		case PERSIAN:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.ORANGE));
			break;
		case RAGDOLL:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.BROWN));
			break;
		case RED:
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
			break;
		case SIAMESE:
			ItemUtils.setLeatherColor(iconItem, Color.GRAY.mixDyes(DyeColor.BROWN));
			break;
		case WHITE:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		case TABBY:
			ItemUtils.setLeatherColor(iconItem, Color.BLACK.mixColors(Color.ORANGE));
			break;
		default:
			// unknown type:
			ItemUtils.setLeatherColor(iconItem, Color.PURPLE);
			break;
		}
		// TODO use more specific text
		// String catTypeName = StringUtils.capitalizeAll(catType.name().toLowerCase(Locale.ROOT).replace('_', ' '));
		// ItemUtils.setItemStackNameAndLore(item, ChatColor.GOLD + catTypeName, null);
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
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
