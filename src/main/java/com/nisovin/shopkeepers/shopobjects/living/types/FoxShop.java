package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
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
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.EditorHandler;
import com.nisovin.shopkeepers.util.EnumUtils;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Validate;

public class FoxShop extends SittableShop<Fox> {

	private final Property<Fox.Type> foxTypeProperty = new EnumProperty<>(shopkeeper, Fox.Type.class, "foxType", Fox.Type.RED);
	private final Property<Boolean> sleepingProperty = new BooleanProperty(shopkeeper, "sleeping", false);
	private final Property<Boolean> crouchingProperty = new BooleanProperty(shopkeeper, "crouching", false);

	public FoxShop(	LivingShops livingShops, SKLivingShopObjectType<FoxShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		foxTypeProperty.load(configSection);
		sleepingProperty.load(configSection);
		crouchingProperty.load(configSection);
		// Incompatible with each other:
		if (this.isSleeping() && this.isCrouching()) {
			this.setCrouching(false);
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		foxTypeProperty.save(configSection);
		sleepingProperty.save(configSection);
		crouchingProperty.save(configSection);
	}

	@Override
	protected void onSpawn(Fox entity) {
		super.onSpawn(entity);
		this.applyFoxType(entity);
		this.applySleeping(entity);
		this.applyCrouching(entity);
	}

	@Override
	public List<EditorHandler.Button> createEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.createEditorButtons();
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
		Validate.notNull(foxType, "Fox type is null!");
		foxTypeProperty.setValue(foxType);
		shopkeeper.markDirty();
		this.applyFoxType(this.getEntity()); // Null if not spawned
	}

	public void cycleFoxType(boolean backwards) {
		this.setFoxType(EnumUtils.cycleEnumConstant(Fox.Type.class, this.getFoxType(), backwards));
	}

	private void applyFoxType(Fox entity) {
		if (entity == null) return;
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

	private EditorHandler.Button getFoxTypeEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
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
		Validate.notNull(sleeping, "Sleeping is null!");
		sleepingProperty.setValue(sleeping);
		// Incompatible with crouching:
		if (sleeping && this.isCrouching()) {
			this.setCrouching(false);
		}
		shopkeeper.markDirty();
		this.applySleeping(this.getEntity()); // Null if not spawned
	}

	public void cycleSleeping() {
		this.setSleeping(!this.isSleeping());
	}

	private void applySleeping(Fox entity) {
		if (entity == null) return;
		entity.setSleeping(this.isSleeping());
	}

	private ItemStack getSleepingEditorItem() {
		ItemStack iconItem = new ItemStack(this.isSleeping() ? Material.GREEN_BED : Material.RED_BED);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonFoxSleeping, Messages.buttonFoxSleepingLore);
		return iconItem;
	}

	private EditorHandler.Button getSleepingEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
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
		Validate.notNull(crouching, "Crouching is null!");
		crouchingProperty.setValue(crouching);
		// Incompatible with sleeping:
		if (crouching && this.isSleeping()) {
			this.setSleeping(false);
		}
		shopkeeper.markDirty();
		this.applyCrouching(this.getEntity()); // Null if not spawned
	}

	public void cycleCrouching() {
		this.setCrouching(!this.isCrouching());
	}

	private void applyCrouching(Fox entity) {
		if (entity == null) return;
		entity.setCrouching(this.isCrouching());
	}

	private ItemStack getCrouchingEditorItem() {
		ItemStack iconItem = new ItemStack(this.isCrouching() ? Material.GREEN_CARPET : Material.RED_CARPET);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonFoxCrouching, Messages.buttonFoxCrouchingLore);
		return iconItem;
	}

	private EditorHandler.Button getCrouchingEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
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
