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
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SheepShop extends BabyableShop<Sheep> {

	private final Property<DyeColor> colorProperty = new EnumProperty<>(shopkeeper, DyeColor.class, "color", DyeColor.WHITE);
	private final Property<Boolean> shearedProperty = new BooleanProperty(shopkeeper, "sheared", false);

	public SheepShop(	LivingShops livingShops, SKLivingShopObjectType<SheepShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		colorProperty.load(configSection);
		shearedProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		colorProperty.save(configSection);
		shearedProperty.save(configSection);
	}

	@Override
	protected void onSpawn(Sheep entity) {
		super.onSpawn(entity);
		this.applyColor(entity);
		this.applySheared(entity);
	}

	@Override
	public List<EditorHandler.Button> createEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getShearedEditorButton());
		return editorButtons;
	}

	// COLOR

	public DyeColor getColor() {
		return colorProperty.getValue();
	}

	public void setColor(DyeColor color) {
		Validate.notNull(color, "Color is null!");
		colorProperty.setValue(color);
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // Null if not spawned
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstant(DyeColor.class, this.getColor(), backwards));
	}

	private void applyColor(Sheep entity) {
		if (entity == null) return;
		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(this.getColor()));
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonSheepColor, Messages.buttonSheepColorLore);
		return iconItem;
	}

	private EditorHandler.Button getColorEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
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

	public boolean isSheared() {
		return shearedProperty.getValue();
	}

	public void setSheared(boolean sheared) {
		shearedProperty.setValue(sheared);
		shopkeeper.markDirty();
		this.applySheared(this.getEntity()); // Null if not spawned
	}

	public void cycleSheared() {
		this.setSheared(!this.isSheared());
	}

	private void applySheared(Sheep entity) {
		if (entity == null) return;
		entity.setSheared(this.isSheared());
	}

	private ItemStack getShearedEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SHEARS);
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonSheepSheared, Messages.buttonSheepShearedLore);
		return iconItem;
	}

	private EditorHandler.Button getShearedEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
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
