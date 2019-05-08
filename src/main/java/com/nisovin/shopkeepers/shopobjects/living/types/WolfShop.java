package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Utils;

public class WolfShop extends SittableShop<Wolf> {

	private static final Property<Boolean> PROPERTY_ANGRY = new BooleanProperty("angry", false);
	private static final Property<DyeColor> PROPERTY_COLLAR_COLOR = new EnumProperty<DyeColor>(DyeColor.class, "collarColor", null) {
		@Override
		public boolean isNullable() {
			// null to indicate 'no collar / untamed'
			return true;
		}
	};

	private boolean angry = PROPERTY_ANGRY.getDefaultValue();
	private DyeColor collarColor = PROPERTY_COLLAR_COLOR.getDefaultValue(); // can be null

	public WolfShop(LivingShops livingShops, SKLivingShopObjectType<WolfShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.angry = PROPERTY_ANGRY.load(shopkeeper, configSection);
		this.collarColor = PROPERTY_COLLAR_COLOR.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_ANGRY.save(shopkeeper, configSection, angry);
		PROPERTY_COLLAR_COLOR.save(shopkeeper, configSection, collarColor);
	}

	@Override
	protected void onSpawn(Wolf entity) {
		super.onSpawn(entity);
		this.applyAngry(entity);
		this.applyCollarColor(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		// TODO this doesn't currently work since minecraft will reset the angry state every tick if the wolf has no
		// target
		// editorButtons.add(this.getAngryEditorButton());
		editorButtons.add(this.getCollarColorEditorButton());
		return editorButtons;
	}

	// ANGRY

	public void setAngry(boolean angry) {
		this.angry = angry;
		shopkeeper.markDirty();
		this.applyAngry(this.getEntity()); // null if not active
	}

	private void applyAngry(Wolf entity) {
		if (entity == null) return;
		entity.setAngry(angry);
	}

	public void cycleAngry() {
		this.setAngry(!angry);
	}

	private ItemStack getAngryEditorItem() {
		ItemStack iconItem = new ItemStack(angry ? Material.RED_WOOL : Material.WHITE_WOOL);
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getAngryEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getAngryEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleAngry();
				return true;
			}
		};
	}

	// COLLAR COLOR

	public void setCollarColor(DyeColor collarColor) {
		this.collarColor = collarColor;
		shopkeeper.markDirty();
		this.applyCollarColor(this.getEntity()); // null if not active
	}

	private void applyCollarColor(Wolf entity) {
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
		this.setCollarColor(Utils.cycleEnumConstantNullable(DyeColor.class, collarColor));
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
