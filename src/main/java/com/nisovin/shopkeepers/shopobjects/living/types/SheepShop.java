package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Sheep;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.Log;
import com.nisovin.shopkeepers.util.Utils;

public class SheepShop extends BabyableShop<Sheep> {

	private static final DyeColor DEFAULT_COLOR = DyeColor.WHITE;
	private static final boolean DEFAULT_SHEARED = false;

	private DyeColor color = DEFAULT_COLOR;
	private boolean sheared = DEFAULT_SHEARED;

	public SheepShop(	LivingShops livingShops, SKLivingShopObjectType<SheepShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.loadColor(configSection);
		this.loadSheared(configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		this.saveColor(configSection);
		this.saveSheared(configSection);
	}

	@Override
	protected void onSpawn(Sheep entity) {
		super.onSpawn(entity);
		this.applyColor(entity);
		this.applySheared(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getColorEditorButton());
		editorButtons.add(this.getShearedEditorButton());
		return editorButtons;
	}

	// COLOR

	private void loadColor(ConfigurationSection configSection) {
		String colorName = configSection.getString("color");
		DyeColor color = Utils.parseEnumValue(DyeColor.class, colorName);
		if (color == null) {
			// fallback to default:
			Log.warning("Missing or invalid sheep color '" + colorName + "' for shopkeeper " + shopkeeper.getId()
					+ ". Using '" + DEFAULT_COLOR + "' now.");
			color = DEFAULT_COLOR;
			shopkeeper.markDirty();
		}
		this.color = color;
	}

	private void saveColor(ConfigurationSection configSection) {
		configSection.set("color", color.name());
	}

	public void setColor(DyeColor color) {
		Validate.notNull(color, "Color is null!");
		this.color = color;
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // null if not active
	}

	private void applyColor(Sheep entity) {
		if (entity == null) return;
		entity.setColor(color);
	}

	public void cycleColor() {
		this.setColor(Utils.getNextEnumConstant(DyeColor.class, color));
	}

	private ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(color));
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getColorEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleColor();
				return true;
			}
		};
	}

	// SHEARED

	private void loadSheared(ConfigurationSection configSection) {
		if (!configSection.isBoolean("sheared")) {
			Log.warning("Missing or invalid 'sheared' state for shopkeeper " + shopkeeper.getId()
					+ "'. Using '" + DEFAULT_SHEARED + "' now.");
			sheared = DEFAULT_SHEARED;
			shopkeeper.markDirty();
		} else {
			sheared = configSection.getBoolean("sheared");
		}
	}

	private void saveSheared(ConfigurationSection configSection) {
		configSection.set("sheared", sheared);
	}

	public void setSheared(boolean sheared) {
		this.sheared = sheared;
		shopkeeper.markDirty();
		this.applySheared(this.getEntity()); // null if not active
	}

	private void applySheared(Sheep entity) {
		if (entity == null) return;
		entity.setSheared(sheared);
	}

	public void cycleSheared() {
		this.setSheared(!sheared);
	}

	private ItemStack getShearedEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SHEARS);
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}

	private EditorHandler.Button getShearedEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getShearedEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleSheared();
				return true;
			}
		};
	}
}
