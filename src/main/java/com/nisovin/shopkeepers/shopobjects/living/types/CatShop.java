package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

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
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

public class CatShop extends SittableShop<Cat> {

	private static final Property<Cat.Type> PROPERTY_CAT_TYPE = new EnumProperty<>(Cat.Type.class, "catType", Cat.Type.TABBY);
	private static final Property<DyeColor> PROPERTY_COLLAR_COLOR = new EnumProperty<DyeColor>(DyeColor.class, "collarColor", null) {
		@Override
		public boolean isNullable() {
			// Null to indicate 'no collar / untamed':
			return true;
		}
	};

	private Cat.Type catType = PROPERTY_CAT_TYPE.getDefaultValue();
	private DyeColor collarColor = PROPERTY_COLLAR_COLOR.getDefaultValue(); // Can be null

	public CatShop(	LivingShops livingShops, SKLivingShopObjectType<CatShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.catType = PROPERTY_CAT_TYPE.load(shopkeeper, configSection);
		this.collarColor = PROPERTY_COLLAR_COLOR.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_CAT_TYPE.save(shopkeeper, configSection, catType);
		PROPERTY_COLLAR_COLOR.save(shopkeeper, configSection, collarColor);
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

	// MC 1.14: Conversion from ocelot types to similar cat types:
	public static Cat.Type fromOcelotType(String ocelotType) {
		if (ocelotType == null) ocelotType = "WILD_OCELOT"; // Default ocelot type
		switch (ocelotType) {
		case "BLACK_CAT":
			return Cat.Type.BLACK;
		case "RED_CAT":
		case "WILD_OCELOT": // There is no equivalent, RED seems to visually match the best
			return Cat.Type.RED;
		case "SIAMESE_CAT":
			return Cat.Type.SIAMESE;
		default:
			return PROPERTY_CAT_TYPE.getDefaultValue(); // Fallback to default
		}
	}

	public void setCatType(Cat.Type catType) {
		Validate.notNull(catType, "Cat type is null!");
		this.catType = catType;
		shopkeeper.markDirty();
		this.applyCatType(this.getEntity()); // Null if not active
	}

	private void applyCatType(Cat entity) {
		if (entity == null) return;
		entity.setCatType(catType);
	}

	public void cycleCatType(boolean backwards) {
		this.setCatType(EnumUtils.cycleEnumConstant(Cat.Type.class, catType, backwards));
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
			// Unknown type:
			ItemUtils.setLeatherColor(iconItem, Color.PURPLE);
			break;
		}
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonCatVariant, Settings.msgButtonCatVariantLore);
		return iconItem;
	}

	private EditorHandler.Button getCatTypeEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getCatTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleCatType(backwards);
				return true;
			}
		};
	}

	// COLLAR COLOR

	public void setCollarColor(DyeColor collarColor) {
		this.collarColor = collarColor;
		shopkeeper.markDirty();
		this.applyCollarColor(this.getEntity()); // Null if not active
	}

	private void applyCollarColor(Cat entity) {
		if (entity == null) return;
		if (collarColor == null) {
			// No collar / untamed:
			entity.setTamed(false);
		} else {
			entity.setTamed(true); // Only tamed cats will show the collar
			entity.setCollarColor(collarColor);
		}
	}

	public void cycleCollarColor(boolean backwards) {
		this.setCollarColor(EnumUtils.cycleEnumConstantNullable(DyeColor.class, collarColor, backwards));
	}

	private ItemStack getCollarColorEditorItem() {
		ItemStack iconItem;
		if (collarColor == null) {
			iconItem = new ItemStack(Material.BARRIER);
		} else {
			iconItem = new ItemStack(ItemUtils.getWoolType(collarColor));
		}
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonCollarColor, Settings.msgButtonCollarColorLore);
		return iconItem;
	}

	private EditorHandler.Button getCollarColorEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getCollarColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleCollarColor(backwards);
				return true;
			}
		};
	}
}
