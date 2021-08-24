package com.nisovin.shopkeepers.shopobjects.living.types;

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
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class MagmaCubeShop extends SKLivingShopObject<MagmaCube> {

	// Note: Minecraft actually allows magma cubes with sizes up to 256 (internally stored as 0 - 255). However, at
	// these sizes the magma cube is not properly rendered anymore, cannot be interacted with, and it becomes laggy.
	// We limit it to 10 since this seems to be a more reasonable limit.
	private final IntegerProperty sizeProperty = new IntegerProperty(shopkeeper, "magmaCubeSize", 1, 10, 1);

	public MagmaCubeShop(	LivingShops livingShops, SKLivingShopObjectType<MagmaCubeShop> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ConfigurationSection shopObjectData) {
		super.load(shopObjectData);
		sizeProperty.load(shopObjectData);
	}

	@Override
	public void save(ConfigurationSection shopObjectData) {
		super.save(shopObjectData);
		sizeProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn(MagmaCube entity) {
		super.onSpawn(entity);
		this.applySize(entity);
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getSizeEditorButton());
		return editorButtons;
	}

	// SIZE

	public int getSize() {
		return sizeProperty.getValue();
	}

	public void setSize(int size) {
		Validate.isTrue(sizeProperty.isInBounds(size), () -> "size is out of bounds: " + size);
		sizeProperty.setValue(size);
		shopkeeper.markDirty();
		this.applySize(this.getEntity()); // Null if not spawned
	}

	public void cycleSize(boolean backwards) {
		int size = this.getSize();
		int nextSize;
		if (backwards) {
			nextSize = size - 1;
		} else {
			nextSize = size + 1;
		}
		nextSize = MathUtils.rangeModulo(nextSize, sizeProperty.getMinValue(), sizeProperty.getMaxValue());
		this.setSize(nextSize);
	}

	private void applySize(MagmaCube entity) {
		if (entity == null) return;
		// Note: Minecraft will also adjust some of the magma cube's attributes, but these should not affect us.
		entity.setSize(this.getSize());
	}

	private ItemStack getSizeEditorItem() {
		int size = this.getSize();
		ItemStack iconItem = new ItemStack(Material.SLIME_BLOCK);
		String displayName = StringUtils.replaceArguments(Messages.buttonMagmaCubeSize, "size", size);
		List<String> lore = StringUtils.replaceArguments(Messages.buttonMagmaCubeSizeLore, "size", size);
		ItemUtils.setDisplayNameAndLore(iconItem, displayName, lore);
		return iconItem;
	}

	private Button getSizeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getSizeEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleSize(backwards);
				return true;
			}
		};
	}
}
