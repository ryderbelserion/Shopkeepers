package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
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

	private static final Cat.Type DEFAULT_CAT_TYPE = Cat.Type.TABBY;
	private static final DyeColor DEFAULT_COLLAR_COLOR = null; // null to indicate 'no collar / untamed'

	private Cat.Type catType = DEFAULT_CAT_TYPE;
	private DyeColor collarColor = DEFAULT_COLLAR_COLOR; // can be null

	public CatShop(	LivingShops livingShops, SKLivingShopObjectType<CatShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.loadCatType(configSection);
		this.loadCollarColor(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		this.saveCatType(configSection);
		this.saveCollarColor(configSection);
	}

	@Override
	protected void onSpawn(Cat entity) {
		super.onSpawn(entity);
		this.applyCatType(entity);
		this.applyCollarColor(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getCatTypeEditorButton());
		editorButtons.add(this.getCollarColorEditorButton());
		return editorButtons;
	}

	// CAT TYPE

	// MC 1.14: conversion from ocelot types to similar cat types:
	public static Cat.Type fromOcelotType(String ocelotType) {
		if (ocelotType == null) ocelotType = "WILD_OCELOT"; // default ocelot type
		switch (ocelotType) {
		case "BLACK_CAT":
			return Cat.Type.BLACK;
		case "RED_CAT":
		case "WILD_OCELOT": // there is no equivalent, RED seems to visually match the best
			return Cat.Type.RED;
		case "SIAMESE_CAT":
			return Cat.Type.SIAMESE;
		default:
			return DEFAULT_CAT_TYPE; // fallback to default
		}
	}

	private void loadCatType(ConfigurationSection configSection) {
		String catTypeName = configSection.getString("catType");
		Cat.Type catType = Utils.parseEnumValue(Cat.Type.class, catTypeName);
		if (catType == null) {
			// fallback:
			Log.warning("Missing or invalid cat type '" + catTypeName + "' for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + DEFAULT_CAT_TYPE + "' now.");
			catType = DEFAULT_CAT_TYPE;
			shopkeeper.markDirty();
		}
		this.catType = catType;
	}

	private void saveCatType(ConfigurationSection configSection) {
		configSection.set("catType", catType.name());
	}

	public void setCatType(Cat.Type catType) {
		Validate.notNull(catType, "Cat type is null!");
		this.catType = catType;
		shopkeeper.markDirty();
		this.applyCatType(this.getEntity()); // null if not active
	}

	private void applyCatType(Cat entity) {
		if (entity == null) return;
		entity.setCatType(catType);
	}

	public void cycleCatType() {
		this.setCatType(Utils.getNextEnumConstant(Cat.Type.class, catType));
	}

	private ItemStack getCatTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (catType) {
		case TABBY:
			ItemUtils.setLeatherColor(iconItem, Color.BLACK.mixColors(Color.ORANGE));
			break;
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

	private EditorHandler.Button getCatTypeEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getCatTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleCatType();
				return true;
			}
		};
	}

	// COLLAR COLOR

	private void loadCollarColor(ConfigurationSection configSection) {
		String colorName = configSection.getString("collarColor");
		DyeColor collarColor = Utils.parseEnumValue(DyeColor.class, colorName);
		if (collarColor == null && colorName != null) {
			// fallback to default:
			Log.warning("Invalid cat collar color '" + colorName + "' for shopkeeper " + shopkeeper.getId()
					+ ". Using '" + (DEFAULT_COLLAR_COLOR == null ? "none" : DEFAULT_COLLAR_COLOR) + "' now.");
			collarColor = DEFAULT_COLLAR_COLOR;
			shopkeeper.markDirty();
		}
		this.collarColor = collarColor;
	}

	private void saveCollarColor(ConfigurationSection configSection) {
		configSection.set("collarColor", collarColor == null ? null : collarColor.name());
	}

	public void setCollarColor(DyeColor collarColor) {
		this.collarColor = collarColor;
		shopkeeper.markDirty();
		this.applyCollarColor(this.getEntity()); // null if not active
	}

	private void applyCollarColor(Cat entity) {
		if (entity == null) return;
		if (collarColor == null) {
			// no collar / untamed:
			entity.setTamed(false);
		} else {
			entity.setTamed(true); // only tamed cats will show the collar
			entity.setCollarColor(collarColor);
		}
	}

	public void cycleCollarColor() {
		DyeColor nextCollarColor;
		if (collarColor == DyeColor.BLACK) {
			nextCollarColor = null;
		} else {
			nextCollarColor = Utils.getNextEnumConstant(DyeColor.class, collarColor);
		}
		this.setCollarColor(nextCollarColor);
	}

	private ItemStack getCollarColorEditorItem() {
		ItemStack iconItem;
		if (collarColor == null) {
			iconItem = new ItemStack(Material.BARRIER);
		} else {
			iconItem = new ItemStack(ItemUtils.getWoolType(collarColor));
		}
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getCollarColorEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getCollarColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleCollarColor();
				return true;
			}
		};
	}
}
