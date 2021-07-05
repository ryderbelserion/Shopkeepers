package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Cat;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.EditorHandler;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

public class CatShop extends SittableShop<Cat> {

	private final Property<Cat.Type> catTypeProperty = new EnumProperty<>(shopkeeper, Cat.Type.class, "catType", Cat.Type.TABBY);
	private final Property<DyeColor> collarColorProperty = new EnumProperty<DyeColor>(shopkeeper, DyeColor.class, "collarColor", null) {
		@Override
		public boolean isNullable() {
			return true; // Null indicates 'no collar' / untamed
		}
	};

	public CatShop(	LivingShops livingShops, SKLivingShopObjectType<CatShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		catTypeProperty.load(configSection);
		collarColorProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		catTypeProperty.save(configSection);
		collarColorProperty.save(configSection);
	}

	@Override
	protected void onSpawn(Cat entity) {
		super.onSpawn(entity);
		this.applyCatType(entity);
		this.applyCollarColor(entity);
	}

	@Override
	public List<EditorHandler.Button> createEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.createEditorButtons();
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
			return Cat.Type.TABBY; // Fallback to default cat type
		}
	}

	public Cat.Type getCatType() {
		return catTypeProperty.getValue();
	}

	public void setCatType(Cat.Type catType) {
		Validate.notNull(catType, "Cat type is null!");
		catTypeProperty.setValue(catType);
		shopkeeper.markDirty();
		this.applyCatType(this.getEntity()); // Null if not spawned
	}

	public void cycleCatType(boolean backwards) {
		this.setCatType(EnumUtils.cycleEnumConstant(Cat.Type.class, this.getCatType(), backwards));
	}

	private void applyCatType(Cat entity) {
		if (entity == null) return;
		entity.setCatType(this.getCatType());
	}

	private ItemStack getCatTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getCatType()) {
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
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonCatVariant, Messages.buttonCatVariantLore);
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

	public DyeColor getCollarColor() {
		return collarColorProperty.getValue();
	}

	public void setCollarColor(DyeColor collarColor) {
		collarColorProperty.setValue(collarColor);
		shopkeeper.markDirty();
		this.applyCollarColor(this.getEntity()); // Null if not spawned
	}

	public void cycleCollarColor(boolean backwards) {
		this.setCollarColor(EnumUtils.cycleEnumConstantNullable(DyeColor.class, this.getCollarColor(), backwards));
	}

	private void applyCollarColor(Cat entity) {
		if (entity == null) return;
		DyeColor collarColor = this.getCollarColor();
		if (collarColor == null) {
			// No collar / untamed:
			entity.setTamed(false);
		} else {
			entity.setTamed(true); // Only tamed cats will show the collar
			entity.setCollarColor(collarColor);
		}
	}

	private ItemStack getCollarColorEditorItem() {
		DyeColor collarColor = this.getCollarColor();
		ItemStack iconItem;
		if (collarColor == null) {
			iconItem = new ItemStack(Material.BARRIER);
		} else {
			iconItem = new ItemStack(ItemUtils.getWoolType(collarColor));
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonCollarColor, Messages.buttonCollarColorLore);
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
