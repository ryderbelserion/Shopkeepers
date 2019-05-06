package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;

public class CreeperShop extends SKLivingShopObject<Creeper> {

	private boolean powered = false;

	public CreeperShop(	LivingShops livingShops, SKLivingShopObjectType<CreeperShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		powered = configSection.getBoolean("powered");
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("powered", powered);
	}

	@Override
	protected void onSpawn(Creeper entity) {
		super.onSpawn(entity);
		this.applyPowered(entity);
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getPoweredEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cyclePowered();
				return true;
			}
		});
		return editorButtons;
	}

	// POWERED STATE

	public void setPowered(boolean powered) {
		this.powered = powered;
		shopkeeper.markDirty();
		this.applyPowered(this.getEntity()); // null if not active
	}

	protected void applyPowered(Creeper entity) {
		if (entity == null) return;
		entity.setPowered(powered);
	}

	public void cyclePowered() {
		this.setPowered(!powered);
	}

	protected ItemStack getPoweredEditorItem() {
		ItemStack iconItem;
		if (powered) {
			iconItem = new ItemStack(Material.LIGHT_BLUE_WOOL);
		} else {
			iconItem = new ItemStack(Material.LIME_WOOL);
		}
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}
}
