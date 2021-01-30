package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Pig;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.BooleanProperty;
import com.nisovin.shopkeepers.property.Property;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class PigShop extends BabyableShop<Pig> {

	private final Property<Boolean> saddleProperty = new BooleanProperty(shopkeeper, "saddle", false);

	public PigShop(	LivingShops livingShops, SKLivingShopObjectType<PigShop> livingObjectType,
					AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		saddleProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		saddleProperty.save(configSection);
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

	public boolean hasSaddle() {
		return saddleProperty.getValue();
	}

	public void setSaddle(boolean saddle) {
		saddleProperty.setValue(saddle);
		shopkeeper.markDirty();
		this.applySaddle(this.getEntity()); // Null if not active
	}

	public void cycleSaddle() {
		this.setSaddle(!this.hasSaddle());
	}

	private void applySaddle(Pig entity) {
		if (entity == null) return;
		entity.setSaddle(this.hasSaddle());
	}

	private ItemStack getSaddleEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SADDLE);
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonPigSaddle, Messages.buttonPigSaddleLore);
		return iconItem;
	}

	private EditorHandler.Button getSaddleEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
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
