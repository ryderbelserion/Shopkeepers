package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class BabyableShop<E extends Ageable> extends SKLivingShopObject<E> {

	private final Property<Boolean> babyProperty = new BooleanProperty(shopkeeper, "baby", false);

	public BabyableShop(LivingShops livingShops, SKLivingShopObjectType<? extends BabyableShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	// TODO Remove special case once this is resolved differently.
	protected boolean isBabyable() {
		// Some mobs don't support the baby variant even though they are Ageable.
		switch (this.getEntityType()) {
		case PARROT:
		case WANDERING_TRADER:
			return false;
		default:
			return true;
		}
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		if (this.isBabyable()) {
			babyProperty.load(configSection);
		}
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		if (this.isBabyable()) {
			babyProperty.save(configSection);
		}
	}

	@Override
	protected void onSpawn(E entity) {
		super.onSpawn(entity);
		if (this.isBabyable()) {
			this.applyBaby(entity);
		}
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		if (!this.isBabyable()) {
			return super.getEditorButtons();
		}
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getBabyEditorButton());
		return editorButtons;
	}

	// BABY

	public boolean isBaby() {
		return babyProperty.getValue();
	}

	public void setBaby(boolean baby) {
		if (!this.isBabyable()) return;
		babyProperty.setValue(baby);
		shopkeeper.markDirty();
		this.applyBaby(this.getEntity()); // Null if not active
	}

	public void cycleBaby() {
		this.setBaby(!this.isBaby());
	}

	private void applyBaby(E entity) {
		if (entity == null) return;
		if (!this.isBabyable()) return;
		if (this.isBaby()) {
			entity.setBaby();
		} else {
			entity.setAdult();
			// TODO: MC-9568: Growing up mobs get moved.
			this.teleportBack();
		}
	}

	private ItemStack getBabyEditorItem() {
		ItemStack iconItem = new ItemStack(Material.EGG);
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonBaby, Messages.buttonBabyLore);
		return iconItem;
	}

	private EditorHandler.Button getBabyEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getBabyEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleBaby();
				return true;
			}
		};
	}
}
