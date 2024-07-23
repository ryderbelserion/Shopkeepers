package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.*;
import org.bukkit.entity.Cat;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class CatShop extends SittableShop<@NonNull Cat> {

	public static final Property<@Nullable DyeColor> COLLAR_COLOR = new BasicProperty<@Nullable DyeColor>()
			.dataKeyAccessor("collarColor", EnumSerializers.lenient(DyeColor.class))
			.nullable() // Null indicates 'no collar' / untamed
			.defaultValue(null)
			.build();

	private PropertyValue<Cat.@NonNull Type> catTypeProperty;
	private final PropertyValue<@Nullable DyeColor> collarColorProperty = new PropertyValue<>(COLLAR_COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyCollarColor)
			.build(properties);

	public CatShop(
			LivingShops livingShops,
			SKLivingShopObjectType<@NonNull CatShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		if (catTypeProperty == null) {
			Property<Cat.@NonNull Type> catType = new BasicProperty<Cat.@NonNull Type>()
					.dataKeyAccessor("catType", NMSManager.getProvider().getCatTypeSerializer())
					.defaultValue(Cat.Type.TABBY)
					.build();
			catTypeProperty = new PropertyValue<>(catType)
					.onValueChanged(Unsafe.initialized(this)::applyCatType)
					.build(properties);
		}
		catTypeProperty.load(shopObjectData);
		collarColorProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		catTypeProperty.save(shopObjectData);
		collarColorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyCatType();
		this.applyCollarColor();
	}

	@Override
	public List<@NonNull Button> createEditorButtons() {
		List<@NonNull Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getCatTypeEditorButton());
		editorButtons.add(this.getCollarColorEditorButton());
		return editorButtons;
	}

	// CAT TYPE

	public Cat.Type getCatType() {
		return catTypeProperty.getValue();
	}

	public void setCatType(Cat.Type catType) {
		catTypeProperty.setValue(catType);
	}

	public void cycleCatType(boolean backwards) {
		this.setCatType(NMSManager.getProvider().cycleCatType(this.getCatType(), backwards));
	}

	private void applyCatType() {
		Cat entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setCatType(this.getCatType());
	}

	private ItemStack getCatTypeEditorItem() {
		ItemStack iconItem = new ItemStack(Material.LEATHER_CHESTPLATE);
		switch (this.getCatType().getKey().getKey()) {
		case "tabby":
			ItemUtils.setLeatherColor(iconItem, Color.BLACK.mixColors(Color.ORANGE));
			break;
		case "all_black":
			ItemUtils.setLeatherColor(iconItem, Color.BLACK);
			break;
		case "black":
			ItemUtils.setLeatherColor(iconItem, Color.BLACK.mixDyes(DyeColor.GRAY));
			break;
		case "british_shorthair":
			ItemUtils.setLeatherColor(iconItem, Color.SILVER);
			break;
		case "calico":
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE.mixDyes(DyeColor.BROWN));
			break;
		case "jellie":
			ItemUtils.setLeatherColor(iconItem, Color.GRAY);
			break;
		case "persian":
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.ORANGE));
			break;
		case "ragdoll":
			ItemUtils.setLeatherColor(iconItem, Color.WHITE.mixDyes(DyeColor.BROWN));
			break;
		case "red":
			ItemUtils.setLeatherColor(iconItem, Color.ORANGE);
			break;
		case "siamese":
			ItemUtils.setLeatherColor(iconItem, Color.GRAY.mixDyes(DyeColor.BROWN));
			break;
		case "white":
			ItemUtils.setLeatherColor(iconItem, Color.WHITE);
			break;
		default:
			// Unknown type:
			ItemUtils.setLeatherColor(iconItem, Color.PURPLE);
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonCatVariant,
				Messages.buttonCatVariantLore
		);
		return iconItem;
	}

	private Button getCatTypeEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getCatTypeEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				boolean backwards = clickEvent.isRightClick();
				cycleCatType(backwards);
				return true;
			}
		};
	}

	// COLLAR COLOR

	public @Nullable DyeColor getCollarColor() {
		return collarColorProperty.getValue();
	}

	public void setCollarColor(@Nullable DyeColor collarColor) {
		collarColorProperty.setValue(collarColor);
	}

	public void cycleCollarColor(boolean backwards) {
		this.setCollarColor(
				EnumUtils.cycleEnumConstantNullable(
						DyeColor.class,
						this.getCollarColor(),
						backwards
				)
		);
	}

	private void applyCollarColor() {
		Cat entity = this.getEntity();
		if (entity == null) return; // Not spawned
		DyeColor collarColor = this.getCollarColor();
		if (collarColor == null) {
			// No collar / untamed:
			entity.setTamed(false);
		} else {
			entity.setTamed(true); // Only tamed cats will show the collar
			entity.setCollarColor(collarColor);
		}
	}

	private ItemStack getCollarColorEditorItem() {
		DyeColor collarColor = this.getCollarColor();
		ItemStack iconItem;
		if (collarColor == null) {
			iconItem = new ItemStack(Material.BARRIER);
		} else {
			iconItem = new ItemStack(ItemUtils.getWoolType(collarColor));
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonCollarColor,
				Messages.buttonCollarColorLore
		);
		return iconItem;
	}

	private Button getCollarColorEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getCollarColorEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				boolean backwards = clickEvent.isRightClick();
				cycleCollarColor(backwards);
				return true;
			}
		};
	}
}
