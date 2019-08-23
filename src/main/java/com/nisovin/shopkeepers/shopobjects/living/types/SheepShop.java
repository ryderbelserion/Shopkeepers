package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
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
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SheepShop extends BabyableShop<Sheep> {

	private static final Property<DyeColor> PROPERTY_COLOR = new EnumProperty<>(DyeColor.class, "color", DyeColor.WHITE);
	private static final Property<Boolean> PROPERTY_SHEARED = new BooleanProperty("sheared", false);

	private DyeColor color = PROPERTY_COLOR.getDefaultValue();
	private boolean sheared = PROPERTY_SHEARED.getDefaultValue();

	public SheepShop(	LivingShops livingShops, SKLivingShopObjectType<SheepShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.color = PROPERTY_COLOR.load(shopkeeper, configSection);
		this.sheared = PROPERTY_SHEARED.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_COLOR.save(shopkeeper, configSection, color);
		PROPERTY_SHEARED.save(shopkeeper, configSection, sheared);
	}

	@Override
	protected void onSpawn(Sheep entity) {
		super.onSpawn(entity);
		this.applyColor(entity);
		this.applySheared(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getShearedEditorButton());
		return editorButtons;
	}

	// COLOR

	public void setColor(DyeColor color) {
		Validate.notNull(color, "Color is null!");
		this.color = color;
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // null if not active
	}

	private void applyColor(Sheep entity) {
		if (entity == null) return;
		entity.setColor(color);
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(DyeColor.class, color, backwards));
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(color));
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonSheepColor, Settings.msgButtonSheepColorLore);
		return iconItem;
	}

	private EditorHandler.Button getColorEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleColor(backwards);
				return true;
			}
		};
	}

	// SHEARED

	public void setSheared(boolean sheared) {
		this.sheared = sheared;
		shopkeeper.markDirty();
		this.applySheared(this.getEntity()); // null if not active
	}

	private void applySheared(Sheep entity) {
		if (entity == null) return;
		entity.setSheared(sheared);
	}

	public void cycleSheared() {
		this.setSheared(!sheared);
	}

	private ItemStack getShearedEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SHEARS);
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonSheepSheared, Settings.msgButtonSheepShearedLore);
		return iconItem;
	}

	private EditorHandler.Button getShearedEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getShearedEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleSheared();
				return true;
			}
		};
	}
}
