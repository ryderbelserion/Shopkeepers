package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
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

public class SheepShop extends BabyableShop<Sheep> {

	private final Property<DyeColor> colorProperty = new EnumProperty<>(DyeColor.class)
			.key("color")
			.defaultValue(DyeColor.WHITE)
			.onValueChanged(this::applyColor)
			.build(properties);
	private final Property<Boolean> shearedProperty = new BooleanProperty()
			.key("sheared")
			.defaultValue(false)
			.onValueChanged(this::applySheared)
			.build(properties);

	public SheepShop(	LivingShops livingShops, SKLivingShopObjectType<SheepShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		colorProperty.load(shopObjectData);
		shearedProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		colorProperty.save(shopObjectData);
		shearedProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyColor();
		this.applySheared();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getShearedEditorButton());
		return editorButtons;
	}

	// COLOR

	public DyeColor getColor() {
		return colorProperty.getValue();
	}

	public void setColor(DyeColor color) {
		colorProperty.setValue(color);
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(DyeColor.class, this.getColor(), backwards));
	}

	private void applyColor() {
		Sheep entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(this.getColor()));
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonSheepColor, Messages.buttonSheepColorLore);
		return iconItem;
	}

	private Button getColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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

	public boolean isSheared() {
		return shearedProperty.getValue();
	}

	public void setSheared(boolean sheared) {
		shearedProperty.setValue(sheared);
	}

	public void cycleSheared() {
		this.setSheared(!this.isSheared());
	}

	private void applySheared() {
		Sheep entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setSheared(this.isSheared());
	}

	private ItemStack getShearedEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SHEARS);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonSheepSheared, Messages.buttonSheepShearedLore);
		return iconItem;
	}

	private Button getShearedEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
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
