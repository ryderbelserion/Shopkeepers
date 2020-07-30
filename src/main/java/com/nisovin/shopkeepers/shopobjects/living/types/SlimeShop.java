package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.property.IntegerProperty;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.MathUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

public class SlimeShop extends SKLivingShopObject<Slime> {

	// Note: Minecraft actually allows slimes with sizes up to 256 (internally stored as 0 - 255). However, at these
	// sizes the slime is not properly rendered anymore, cannot be interacted with, and it becomes laggy.
	// We limit it to 10 since this seems to be a more reasonable limit.
	private static final IntegerProperty PROPERTY_SLIME_SIZE = new IntegerProperty("slimeSize", 1, 10, 1);

	private int slimeSize = PROPERTY_SLIME_SIZE.getDefaultValue();

	public SlimeShop(	LivingShops livingShops, SKLivingShopObjectType<SlimeShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.slimeSize = PROPERTY_SLIME_SIZE.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_SLIME_SIZE.save(shopkeeper, configSection, slimeSize);
	}

	@Override
	protected void onSpawn(Slime entity) {
		super.onSpawn(entity);
		this.applySlimeSize(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getSlimeSizeEditorButton());
		return editorButtons;
	}

	// SLIME SIZE

	public void setSlimeSize(int slimeSize) {
		Validate.isTrue(PROPERTY_SLIME_SIZE.isInBounds(slimeSize), "slimeSize is out of bounds: " + slimeSize);
		this.slimeSize = slimeSize;
		shopkeeper.markDirty();
		this.applySlimeSize(this.getEntity()); // Null if not active
	}

	private void applySlimeSize(Slime entity) {
		if (entity == null) return;
		// Note: Minecraft will also adjust some of the slime's attributes, but these should not affect us.
		entity.setSize(slimeSize);
	}

	public void cycleSlimeSize(boolean backwards) {
		int nextSize;
		if (backwards) {
			nextSize = slimeSize - 1;
		} else {
			nextSize = slimeSize + 1;
		}
		nextSize = MathUtils.rangeModulo(nextSize, PROPERTY_SLIME_SIZE.getMinValue(), PROPERTY_SLIME_SIZE.getMaxValue());
		this.setSlimeSize(nextSize);
	}

	private ItemStack getSlimeSizeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SLIME_BLOCK);
		String displayName = TextUtils.replaceArguments(Settings.msgButtonSlimeSize, "size", slimeSize);
		List<String> lore = TextUtils.replaceArguments(Settings.msgButtonSlimeSizeLore, "size", slimeSize);
		ItemUtils.setItemStackNameAndLore(iconItem, displayName, lore);
		return iconItem;
	}

	private EditorHandler.Button getSlimeSizeEditorButton() {
		return new EditorHandler.ActionButton(shopkeeper) {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getSlimeSizeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleSlimeSize(backwards);
				return true;
			}
		};
	}
}
