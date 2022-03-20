package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.entity.Wolf;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
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
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.EnumUtils;

public class WolfShop extends SittableShop<@NonNull Wolf> {

	public static final Property<@NonNull Boolean> ANGRY = new BasicProperty<@NonNull Boolean>()
			.dataKeyAccessor("angry", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	public static final Property<@Nullable DyeColor> COLLAR_COLOR = new BasicProperty<@Nullable DyeColor>()
			.dataKeyAccessor("collarColor", EnumSerializers.lenient(DyeColor.class))
			.nullable() // Null indicates 'no collar' / untamed
			.defaultValue(null)
			.build();

	private final PropertyValue<@NonNull Boolean> angryProperty = new PropertyValue<>(ANGRY)
			.onValueChanged(Unsafe.initialized(this)::applyAngry)
			.build(properties);
	private final PropertyValue<@Nullable DyeColor> collarColorProperty = new PropertyValue<>(COLLAR_COLOR)
			.onValueChanged(Unsafe.initialized(this)::applyCollarColor)
			.build(properties);

	public WolfShop(
			LivingShops livingShops,
			SKLivingShopObjectType<@NonNull WolfShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		angryProperty.load(shopObjectData);
		collarColorProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		angryProperty.save(shopObjectData);
		collarColorProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyAngry();
		this.applyCollarColor();
	}

	@Override
	public List<@NonNull Button> createEditorButtons() {
		List<@NonNull Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getAngryEditorButton());
		editorButtons.add(this.getCollarColorEditorButton());
		return editorButtons;
	}

	// ANGRY

	public boolean isAngry() {
		return angryProperty.getValue();
	}

	public void setAngry(boolean angry) {
		angryProperty.setValue(angry);
	}

	public void cycleAngry() {
		this.setAngry(!this.isAngry());
	}

	private void applyAngry() {
		Wolf entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setAngry(this.isAngry());
	}

	private ItemStack getAngryEditorItem() {
		Material iconItemType = this.isAngry() ? Material.RED_WOOL : Material.WHITE_WOOL;
		ItemStack iconItem = new ItemStack(iconItemType);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonWolfAngry,
				Messages.buttonWolfAngryLore
		);
		return iconItem;
	}

	private Button getAngryEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getAngryEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				cycleAngry();
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
		Wolf entity = this.getEntity();
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
		ItemUtils.setDisplayNameAndLore(
				iconItem,
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
