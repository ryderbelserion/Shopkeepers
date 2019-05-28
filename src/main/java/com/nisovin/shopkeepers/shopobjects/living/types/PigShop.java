package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Pig;
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

public class PigShop extends BabyableShop<Pig> {

	private static final Property<Boolean> PROPERTY_SADDLE = new BooleanProperty("saddle", false);

	private boolean saddle = PROPERTY_SADDLE.getDefaultValue();

	public PigShop(	LivingShops livingShops, SKLivingShopObjectType<PigShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.saddle = PROPERTY_SADDLE.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_SADDLE.save(shopkeeper, configSection, saddle);
	}

	@Override
	protected void onSpawn(Pig entity) {
		super.onSpawn(entity);
		this.applySaddle(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getSaddleEditorButton());
		return editorButtons;
	}

	// SADDLE

	public void setSaddle(boolean saddle) {
		this.saddle = saddle;
		shopkeeper.markDirty();
		this.applySaddle(this.getEntity()); // null if not active
	}

	private void applySaddle(Pig entity) {
		if (entity == null) return;
		entity.setSaddle(saddle);
	}

	public void cycleSaddle() {
		this.setSaddle(!saddle);
	}

	private ItemStack getSaddleEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SADDLE);
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonPigSaddle, Settings.msgButtonPigSaddleLore);
		return iconItem;
	}

	private EditorHandler.Button getSaddleEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getSaddleEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleSaddle();
				return true;
			}
		};
	}
}
