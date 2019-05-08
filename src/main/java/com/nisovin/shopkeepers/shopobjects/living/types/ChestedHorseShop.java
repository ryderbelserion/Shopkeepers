package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Player;
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

public class ChestedHorseShop<E extends ChestedHorse> extends BabyableShop<E> {

	private static final Property<Boolean> PROPERTY_CARRYING_CHEST = new BooleanProperty("carryingChest", false);

	private boolean carryingChest = PROPERTY_CARRYING_CHEST.getDefaultValue();

	public ChestedHorseShop(LivingShops livingShops, SKLivingShopObjectType<?> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.carryingChest = PROPERTY_CARRYING_CHEST.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_CARRYING_CHEST.save(shopkeeper, configSection, carryingChest);
	}

	@Override
	protected void onSpawn(E entity) {
		super.onSpawn(entity);
		this.applyCarryingChest(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getCarryingChestEditorButton());
		return editorButtons;
	}

	// CARRYING CHEST

	public void setCarryingChest(boolean carryingChest) {
		this.carryingChest = carryingChest;
		shopkeeper.markDirty();
		this.applyCarryingChest(this.getEntity()); // null if not active
	}

	private void applyCarryingChest(E entity) {
		if (entity == null) return;
		entity.setCarryingChest(carryingChest);
	}

	public void cycleCarryingChest() {
		this.setCarryingChest(!carryingChest);
	}

	private ItemStack getCarryingChestEditorItem() {
		ItemStack iconItem = new ItemStack(Material.CHEST);
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getCarryingChestEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getCarryingChestEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleCarryingChest();
				return true;
			}
		};
	}
}
