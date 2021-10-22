package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.property.IntegerProperty;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.MathUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public class SlimeShop extends SKLivingShopObject<Slime> {

	// Note: Minecraft actually allows slimes with sizes up to 256 (internally stored as 0 - 255). However, at these
	// sizes the slime is not properly rendered anymore, cannot be interacted with, and it becomes laggy.
	// We limit it to 10 since this seems to be a more reasonable limit.
	private final IntegerProperty sizeProperty = new IntegerProperty()
			.<IntegerProperty>key("slimeSize")
			.minValue(1)
			.maxValue(10)
			.defaultValue(1)
			.onValueChanged(this::applySize)
			.build(properties);

	public SlimeShop(	LivingShops livingShops, SKLivingShopObjectType<SlimeShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		sizeProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		sizeProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applySize();
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
		sizeProperty.setValue(size);
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

	private void applySize() {
		Slime entity = this.getEntity();
		if (entity == null) return; // Not spawned
		// Note: Minecraft will also adjust some of the slime's attributes, but these should not affect us.
		entity.setSize(this.getSize());
	}

	private ItemStack getSizeEditorItem() {
		int size = this.getSize();
		ItemStack iconItem = new ItemStack(Material.SLIME_BLOCK);
		String displayName = StringUtils.replaceArguments(Messages.buttonSlimeSize, "size", size);
		List<String> lore = StringUtils.replaceArguments(Messages.buttonSlimeSizeLore, "size", size);
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
