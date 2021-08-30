package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Wolf;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class WolfShop extends SittableShop<Wolf> {

	private final Property<Boolean> angryProperty = new BooleanProperty()
			.key("angry")
			.defaultValue(false)
			.build(properties);
	private final Property<DyeColor> collarColorProperty = new EnumProperty<DyeColor>(DyeColor.class)
			.key("collarColor")
			.nullable() // Null indicates 'no collar' / untamed
			.defaultValue(null)
			.build(properties);

	public WolfShop(LivingShops livingShops, SKLivingShopObjectType<WolfShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		angryProperty.load(shopObjectData);
		collarColorProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		angryProperty.save(shopObjectData);
		collarColorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn(Wolf entity) {
		super.onSpawn(entity);
		this.applyAngry(entity);
		this.applyCollarColor(entity);
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getAngryEditorButton());
		editorButtons.add(this.getCollarColorEditorButton());
		return editorButtons;
	}

	// ANGRY

	public boolean isAngry() {
		return angryProperty.getValue();
	}

	public void setAngry(boolean angry) {
		angryProperty.setValue(angry);
		this.applyAngry(this.getEntity()); // Null if not spawned
	}

	public void cycleAngry() {
		this.setAngry(!this.isAngry());
	}

	private void applyAngry(Wolf entity) {
		if (entity == null) return;
		entity.setAngry(this.isAngry());
	}

	private ItemStack getAngryEditorItem() {
		ItemStack iconItem = new ItemStack(this.isAngry() ? Material.RED_WOOL : Material.WHITE_WOOL);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonWolfAngry, Messages.buttonWolfAngryLore);
		return iconItem;
	}

	private Button getAngryEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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

	public DyeColor getCollarColor() {
		return collarColorProperty.getValue();
	}

	public void setCollarColor(DyeColor collarColor) {
		collarColorProperty.setValue(collarColor);
		this.applyCollarColor(this.getEntity()); // Null if not spawned
	}

	public void cycleCollarColor(boolean backwards) {
		this.setCollarColor(EnumUtils.cycleEnumConstantNullable(DyeColor.class, this.getCollarColor(), backwards));
	}

	private void applyCollarColor(Wolf entity) {
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

	private Button getCollarColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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
