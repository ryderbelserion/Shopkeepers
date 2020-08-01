package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sittable;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

// Using Babyable as common super type of all sittable mobs for now.
public class SittableShop<E extends Ageable & Sittable> extends BabyableShop<E> {

	private static final Property<Boolean> PROPERTY_SITTING = new BooleanProperty("sitting", false);

	private boolean sitting = PROPERTY_SITTING.getDefaultValue();

	public SittableShop(LivingShops livingShops, SKLivingShopObjectType<? extends SittableShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.sitting = PROPERTY_SITTING.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_SITTING.save(shopkeeper, configSection, sitting);
	}

	@Override
	protected void onSpawn(E entity) {
		super.onSpawn(entity);
		this.applySitting(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getSittingEditorButton());
		return editorButtons;
	}

	// SITTING STATE

	public void setSitting(boolean sitting) {
		this.sitting = sitting;
		shopkeeper.markDirty();
		this.applySitting(this.getEntity()); // Null if not active
	}

	private void applySitting(Sittable entity) {
		if (entity == null) return;
		entity.setSitting(sitting);
	}

	public void cycleSitting() {
		this.setSitting(!sitting);
	}

	private ItemStack getSittingEditorItem() {
		ItemStack iconItem = new ItemStack(Material.IRON_HORSE_ARMOR);
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonSitting, Settings.msgButtonSittingLore);
		return iconItem;
	}

	private EditorHandler.Button getSittingEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getSittingEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleSitting();
				return true;
			}
		};
	}
}
