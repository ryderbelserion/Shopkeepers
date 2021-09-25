package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.EnumProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class FoxShop extends SittableShop<Fox> {

	private final Property<Fox.Type> foxTypeProperty = new EnumProperty<>(Fox.Type.class)
			.key("foxType")
			.defaultValue(Fox.Type.RED)
			.onValueChanged(this::applyFoxType)
			.build(properties);
	private final Property<Boolean> sleepingProperty = new BooleanProperty()
			.key("sleeping")
			.defaultValue(false)
			.onValueChanged(this::applySleeping)
			.build(properties);
	private final Property<Boolean> crouchingProperty = new BooleanProperty()
			.key("crouching")
			.defaultValue(false)
			.onValueChanged(this::applyCrouching)
			.build(properties);

	public FoxShop(	LivingShops livingShops, SKLivingShopObjectType<FoxShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) {
		super.load(shopObjectData);
		foxTypeProperty.load(shopObjectData);
		sleepingProperty.load(shopObjectData);
		crouchingProperty.load(shopObjectData);
		// Incompatible with each other:
		if (this.isSleeping() && this.isCrouching()) {
			this.setCrouching(false);
		}
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		foxTypeProperty.save(shopObjectData);
		sleepingProperty.save(shopObjectData);
		crouchingProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyFoxType();
		this.applySleeping();
		this.applyCrouching();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getFoxTypeEditorButton());
		editorButtons.add(this.getSleepingEditorButton());
		editorButtons.add(this.getCrouchingEditorButton());
		return editorButtons;
	}

	// FOX TYPE

	public Fox.Type getFoxType() {
		return foxTypeProperty.getValue();
	}

	public void setFoxType(Fox.Type foxType) {
		foxTypeProperty.setValue(foxType);
	}

	public void cycleFoxType(boolean backwards) {
		this.setFoxType(EnumUtils.cycleEnumConstant(Fox.Type.class, this.getFoxType(), backwards));
	}

	private void applyFoxType() {
		Fox entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setFoxType(this.getFoxType());
	}

	private ItemStack getFoxTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getFoxType()) {
		case SNOW:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		case RED:
		default:
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonFoxVariant, Messages.buttonFoxVariantLore);
		return iconItem;
	}

	private Button getFoxTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getFoxTypeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleFoxType(backwards);
				return true;
			}
		};
	}

	// SLEEPING

	public boolean isSleeping() {
		return sleepingProperty.getValue();
	}

	public void setSleeping(boolean sleeping) {
		sleepingProperty.setValue(sleeping);
		// Incompatible with crouching:
		if (sleeping && this.isCrouching()) {
			this.setCrouching(false);
		}
	}

	public void cycleSleeping() {
		this.setSleeping(!this.isSleeping());
	}

	private void applySleeping() {
		Fox entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setSleeping(this.isSleeping());
	}

	private ItemStack getSleepingEditorItem() {
		ItemStack iconItem = new ItemStack(this.isSleeping() ? Material.GREEN_BED : Material.RED_BED);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonFoxSleeping, Messages.buttonFoxSleepingLore);
		return iconItem;
	}

	private Button getSleepingEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getSleepingEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleSleeping();
				this.updateAllIcons(); // Required if crouching got disabled
				return true;
			}
		};
	}

	// CROUCHING

	public boolean isCrouching() {
		return crouchingProperty.getValue();
	}

	public void setCrouching(boolean crouching) {
		crouchingProperty.setValue(crouching);
		// Incompatible with sleeping:
		if (crouching && this.isSleeping()) {
			this.setSleeping(false);
		}
	}

	public void cycleCrouching() {
		this.setCrouching(!this.isCrouching());
	}

	private void applyCrouching() {
		Fox entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setCrouching(this.isCrouching());
	}

	private ItemStack getCrouchingEditorItem() {
		ItemStack iconItem = new ItemStack(this.isCrouching() ? Material.GREEN_CARPET : Material.RED_CARPET);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonFoxCrouching, Messages.buttonFoxCrouchingLore);
		return iconItem;
	}

	private Button getCrouchingEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getCrouchingEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleCrouching();
				this.updateAllIcons(); // Required if sleeping got disabled
				return true;
			}
		};
	}
}
