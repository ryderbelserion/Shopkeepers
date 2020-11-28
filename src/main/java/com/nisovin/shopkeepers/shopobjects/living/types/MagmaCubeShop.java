package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.MagmaCube;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.IntegerProperty;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.defaults.EditorHandler;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.MathUtils;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Validate;

public class MagmaCubeShop extends SKLivingShopObject<MagmaCube> {

	// Note: Minecraft actually allows magma cubes with sizes up to 256 (internally stored as 0 - 255). However, at
	// these sizes the magma cube is not properly rendered anymore, cannot be interacted with, and it becomes laggy.
	// We limit it to 10 since this seems to be a more reasonable limit.
	private static final IntegerProperty PROPERTY_MAGMA_CUBE_SIZE = new IntegerProperty("magmaCubeSize", 1, 10, 1);

	private int magmaCubeSize = PROPERTY_MAGMA_CUBE_SIZE.getDefaultValue();

	public MagmaCubeShop(	LivingShops livingShops, SKLivingShopObjectType<MagmaCubeShop> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection configSection) {
		super.load(configSection);
		this.magmaCubeSize = PROPERTY_MAGMA_CUBE_SIZE.load(shopkeeper, configSection);
	}

	@Override
	public void save(ConfigurationSection configSection) {
		super.save(configSection);
		PROPERTY_MAGMA_CUBE_SIZE.save(shopkeeper, configSection, magmaCubeSize);
	}

	@Override
	protected void onSpawn(MagmaCube entity) {
		super.onSpawn(entity);
		this.applyMagmaCubeSize(entity);
	}

	@Override
	public List<EditorHandler.Button> getEditorButtons() {
		List<EditorHandler.Button> editorButtons = new ArrayList<>();
		editorButtons.addAll(super.getEditorButtons());
		editorButtons.add(this.getMagmaCubeSizeEditorButton());
		return editorButtons;
	}

	// MAGMA CUBE SIZE

	public void setMagmaCubeSize(int magmaCubeSize) {
		Validate.isTrue(PROPERTY_MAGMA_CUBE_SIZE.isInBounds(magmaCubeSize), "magmaCubeSize is out of bounds: " + magmaCubeSize);
		this.magmaCubeSize = magmaCubeSize;
		shopkeeper.markDirty();
		this.applyMagmaCubeSize(this.getEntity()); // Null if not active
	}

	private void applyMagmaCubeSize(MagmaCube entity) {
		if (entity == null) return;
		// Note: Minecraft will also adjust some of the magma cube's attributes, but these should not affect us.
		entity.setSize(magmaCubeSize);
	}

	public void cycleMagmaCubeSize(boolean backwards) {
		int nextSize;
		if (backwards) {
			nextSize = magmaCubeSize - 1;
		} else {
			nextSize = magmaCubeSize + 1;
		}
		nextSize = MathUtils.rangeModulo(nextSize, PROPERTY_MAGMA_CUBE_SIZE.getMinValue(), PROPERTY_MAGMA_CUBE_SIZE.getMaxValue());
		this.setMagmaCubeSize(nextSize);
	}

	private ItemStack getMagmaCubeSizeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SLIME_BLOCK);
		String displayName = StringUtils.replaceArguments(Messages.buttonMagmaCubeSize, "size", magmaCubeSize);
		List<String> lore = StringUtils.replaceArguments(Messages.buttonMagmaCubeSizeLore, "size", magmaCubeSize);
		ItemUtils.setItemStackNameAndLore(iconItem, displayName, lore);
		return iconItem;
	}

	private EditorHandler.Button getMagmaCubeSizeEditorButton() {
		return new EditorHandler.ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorHandler.Session session) {
				return getMagmaCubeSizeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleMagmaCubeSize(backwards);
				return true;
			}
		};
	}
}
