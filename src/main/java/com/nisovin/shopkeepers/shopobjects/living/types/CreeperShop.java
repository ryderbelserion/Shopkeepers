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
import com.nisovin.shopkeepers.util.Log;

public class CreeperShop extends SKLivingShopObject<Creeper> {

	private static final boolean DEFAULT_POWERED = false;

	private boolean powered = DEFAULT_POWERED;

	public CreeperShop(	LivingShops livingShops, SKLivingShopObjectType<CreeperShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.loadPowered(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		this.savePowered(configSection);
	}

	@Override
	protected void onSpawn(Creeper entity) {
		super.onSpawn(entity);
		this.applyPowered(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getPoweredEditorButton());
		return editorButtons;
	}

	// POWERED STATE

	private void loadPowered(ConfigurationSection configSection) {
		if (!configSection.isBoolean("powered")) {
			Log.warning("Missing or invalid 'powered' state for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + DEFAULT_POWERED + "' now.");
			powered = DEFAULT_POWERED;
			shopkeeper.markDirty();
		} else {
			powered = configSection.getBoolean("powered");
		}
	}

	private void savePowered(ConfigurationSection configSection) {
		configSection.set("powered", powered);
	}

	public void setPowered(boolean powered) {
		this.powered = powered;
		shopkeeper.markDirty();
		this.applyPowered(this.getEntity()); // null if not active
	}

	private void applyPowered(Creeper entity) {
		if (entity == null) return;
		entity.setPowered(powered);
	}

	public void cyclePowered() {
		this.setPowered(!powered);
	}

	private ItemStack getPoweredEditorItem() {
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

	private EditorHandler.Button getPoweredEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getPoweredEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cyclePowered();
				return true;
			}
		};
	}
}
