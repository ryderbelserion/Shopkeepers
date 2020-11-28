package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Creeper;
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

public class CreeperShop extends SKLivingShopObject<Creeper> {

	private static final Property<Boolean> PROPERTY_POWERED = new BooleanProperty("powered", false);

	private boolean powered = PROPERTY_POWERED.getDefaultValue();

	public CreeperShop(	LivingShops livingShops, SKLivingShopObjectType<CreeperShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.powered = PROPERTY_POWERED.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_POWERED.save(shopkeeper, configSection, powered);
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

	public void setPowered(boolean powered) {
		this.powered = powered;
		shopkeeper.markDirty();
		this.applyPowered(this.getEntity()); // Null if not active
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
		ItemUtils.setItemStackNameAndLore(iconItem, Messages.buttonCreeperCharged, Messages.buttonCreeperChargedLore);
		return iconItem;
	}

	private EditorHandler.Button getPoweredEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
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
