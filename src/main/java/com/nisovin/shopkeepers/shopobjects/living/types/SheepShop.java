package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.apache.commons.lang.Validate;
import org.bukkit.DyeColor;
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

	private DyeColor color = DyeColor.WHITE; // default white

	public SheepShop(	LivingShops livingShops, SKLivingShopObjectType<SheepShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.loadColor(configSection);
	}

	private void loadColor(ConfigurationSection configSection) {
		String colorName = configSection.getString("color");
		DyeColor color = parseColor(colorName);
		if (color == null) {
			// fallback to default white:
			Log.warning("Missing or invalid sheep color '" + colorName + "' for shopkeeper " + shopkeeper.getId()
					+ ". Using '" + DyeColor.WHITE + "' now.");
			color = DyeColor.WHITE;
			shopkeeper.markDirty();
		}
		this.color = color;
	}

	private static DyeColor parseColor(String colorName) {
		if (colorName != null) {
			try {
				return DyeColor.valueOf(colorName);
			} catch (IllegalArgumentException e) {
			}
		}
		return null;
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		configSection.set("color", color.name());
	}

	@Override
	protected void onSpawn(Sheep entity) {
		super.onSpawn(entity);
		this.applyColor(entity);
	}

	// EDITOR ACTIONS

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = super.getEditorButtons(); // assumes modifiable
		editorButtons.add(new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleColor();
				return true;
			}
		});
		return editorButtons;
	}

	// COLOR

	public void setColor(DyeColor color) {
		Validate.notNull(color, "Color is null!");
		this.color = color;
		shopkeeper.markDirty();
		this.applyColor(this.getEntity()); // null if not active
	}

	protected void applyColor(Sheep entity) {
		if (entity == null) return;
		entity.setColor(color);
	}

	public void cycleColor() {
		this.setColor(Utils.getNextEnumConstant(DyeColor.class, color));
	}

	protected ItemStack getColorEditorItem() {
		ItemStack iconItem = new ItemStack(ItemUtils.getWoolType(color), 1);
		// TODO use more specific text
		ItemUtils.setItemStackNameAndLore(iconItem, Settings.msgButtonType, Settings.msgButtonTypeLore);
		return iconItem;
	}
}
