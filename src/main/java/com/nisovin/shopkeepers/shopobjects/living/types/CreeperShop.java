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

	private final Property<Boolean> poweredProperty = new BooleanProperty(shopkeeper, "powered", false);

	public CreeperShop(	LivingShops livingShops, SKLivingShopObjectType<CreeperShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		poweredProperty.load(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		poweredProperty.save(configSection);
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

	public boolean isPowered() {
		return poweredProperty.getValue();
	}

	public void setPowered(boolean powered) {
		poweredProperty.setValue(powered);
		shopkeeper.markDirty();
		this.applyPowered(this.getEntity()); // Null if not active
	}

	public void cyclePowered() {
		this.setPowered(!this.isPowered());
	}

	private void applyPowered(Creeper entity) {
		if (entity == null) return;
		entity.setPowered(this.isPowered());
	}

	private ItemStack getPoweredEditorItem() {
		ItemStack iconItem;
		if (this.isPowered()) {
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
