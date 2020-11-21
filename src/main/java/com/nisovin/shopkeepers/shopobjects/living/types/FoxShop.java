package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Fox;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Messages;
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

public class FoxShop extends SittableShop<Fox> {

	private static final Property<Fox.Type> PROPERTY_FOX_TYPE = new EnumProperty<>(Fox.Type.class, "foxType", Fox.Type.RED);
	private static final Property<Boolean> PROPERTY_SLEEPING = new BooleanProperty("sleeping", false);
	private static final Property<Boolean> PROPERTY_CROUCHING = new BooleanProperty("crouching", false);

	private Fox.Type foxType = PROPERTY_FOX_TYPE.getDefaultValue();
	private boolean sleeping = PROPERTY_SLEEPING.getDefaultValue();
	private boolean crouching = PROPERTY_CROUCHING.getDefaultValue();

	public FoxShop(	LivingShops livingShops, SKLivingShopObjectType<FoxShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.foxType = PROPERTY_FOX_TYPE.load(shopkeeper, configSection);
		this.sleeping = PROPERTY_SLEEPING.load(shopkeeper, configSection);
		this.crouching = PROPERTY_CROUCHING.load(shopkeeper, configSection);
		// Incompatible with each other:
		if (sleeping && crouching) {
			this.setCrouching(false);
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_FOX_TYPE.save(shopkeeper, configSection, foxType);
		PROPERTY_SLEEPING.save(shopkeeper, configSection, sleeping);
		PROPERTY_CROUCHING.save(shopkeeper, configSection, crouching);
	}

	@Override
	protected void onSpawn(Fox entity) {
		super.onSpawn(entity);
		this.applyFoxType(entity);
		this.applySleeping(entity);
		this.applyCrouching(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getFoxTypeEditorButton());
		editorButtons.add(this.getSleepingEditorButton());
		editorButtons.add(this.getCrouchingEditorButton());
		return editorButtons;
	}

	// FOX TYPE

	public void setFoxType(Fox.Type foxType) {
		Validate.notNull(foxType, "Fox type is null!");
		this.foxType = foxType;
		shopkeeper.markDirty();
		this.applyFoxType(this.getEntity()); // Null if not active
	}

	private void applyFoxType(Fox entity) {
		if (entity == null) return;
		entity.setFoxType(foxType);
	}

	public void cycleFoxType(boolean backwards) {
		this.setFoxType(EnumUtils.cycleEnumConstant(Fox.Type.class, foxType, backwards));
	}

	private ItemStack getFoxTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (foxType) {
		case SNOW:
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		case RED:
		default:
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
			break;
		}
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonFoxVariant, Messages.buttonFoxVariantLore);
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

	public void setSleeping(boolean sleeping) {
		Validate.notNull(sleeping, "Sleeping is null!");
		this.sleeping = sleeping;
		// Incompatible with crouching:
		if (sleeping && crouching) {
			this.setCrouching(false);
		}
		shopkeeper.markDirty();
		this.applySleeping(this.getEntity()); // Null if not active
	}

	private void applySleeping(Fox entity) {
		if (entity == null) return;
		entity.setSleeping(sleeping);
	}

	public void cycleSleeping() {
		this.setSleeping(!sleeping);
	}

	private ItemStack getSleepingEditorItem() {
		ItemStack iconItem = new ItemStack(sleeping ? Material.GREEN_BED : Material.RED_BED);
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonFoxSleeping, Messages.buttonFoxSleepingLore);
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

	public void setCrouching(boolean crouching) {
		Validate.notNull(crouching, "Crouching is null!");
		this.crouching = crouching;
		// Incompatible with sleeping:
		if (crouching && sleeping) {
			this.setSleeping(false);
		}
		shopkeeper.markDirty();
		this.applyCrouching(this.getEntity()); // Null if not active
	}

	private void applyCrouching(Fox entity) {
		if (entity == null) return;
		entity.setCrouching(crouching);
	}

	public void cycleCrouching() {
		this.setCrouching(!crouching);
	}

	private ItemStack getCrouchingEditorItem() {
		ItemStack iconItem = new ItemStack(crouching ? Material.GREEN_CARPET : Material.RED_CARPET);
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonFoxCrouching, Messages.buttonFoxCrouchingLore);
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
