package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.Shulker;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.InvalidDataException;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class ShulkerShop extends SKLivingShopObject<Shulker> {

	public static final Property<DyeColor> COLOR = new BasicProperty<DyeColor>()
			.dataKeyAccessor("color", EnumSerializers.lenient(DyeColor.class))
			.nullable() // Null indicates default color
			.defaultValue(null)
			.build();

	private final PropertyValue<DyeColor> colorProperty = new PropertyValue<>(COLOR)
			.onValueChanged(this::applyColor)
			.build(properties);

	public ShulkerShop(	LivingShops livingShops, SKLivingShopObjectType<ShulkerShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		colorProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		colorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyColor();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getColorEditorButton());
		return editorButtons;
	}

	// COLOR

	public DyeColor getColor() {
		return colorProperty.getValue();
	}

	public void setColor(DyeColor color) {
		colorProperty.setValue(color);
	}

	public void cycleColor(boolean backwards) {
		this.setColor(EnumUtils.cycleEnumConstantNullable(DyeColor.class, this.getColor(), backwards));
	}

	private void applyColor() {
		Shulker entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setColor(this.getColor());
	}

	private ItemStack getColorEditorItem() {
		DyeColor color = this.getColor();
		ItemStack iconItem;
		if (color == null) {
			iconItem = new ItemStack(Material.PURPUR_BLOCK);
		} else {
			iconItem = new ItemStack(ItemUtils.getWoolType(color));
		}
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonShulkerColor, Messages.buttonShulkerColorLore);
		return iconItem;
	}

	private Button getColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getColorEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleColor(backwards);
				return true;
			}
		};
	}

	// TODO Attached block face
	// TODO Open state (Peek)
}
