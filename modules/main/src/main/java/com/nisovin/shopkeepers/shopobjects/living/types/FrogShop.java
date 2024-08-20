package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import com.nisovin.shopkeepers.compat.NMSManager;
import org.bukkit.DyeColor;
import org.bukkit.entity.Animals;
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
import com.nisovin.shopkeepers.util.data.property.validation.java.StringValidators;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// TODO Use actual Frog type once we only support Bukkit 1.19 upwards
public class FrogShop extends BabyableShop<@NonNull Animals> {

	// TODO Use correct enum type once we only support Bukkit 1.19 upwards
	public static final Property<@NonNull String> VARIANT = new BasicProperty<@NonNull String>()
			.dataKeyAccessor("frogVariant", StringSerializers.STRICT)
			// TODO Validate that the value is a valid frog type
			.validator(StringValidators.NON_EMPTY)
			.defaultValue("TEMPERATE")
			.build();

	private final PropertyValue<@NonNull String> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);

	public FrogShop(
			LivingShops livingShops,
			SKLivingShopObjectType<@NonNull FrogShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		variantProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		variantProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyVariant();
	}

	@Override
	public List<@NonNull Button> createEditorButtons() {
		List<@NonNull Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getVariantEditorButton());
		return editorButtons;
	}

	// VARIANT

	public String getVariant() {
		return variantProperty.getValue();
	}

	public void setVariant(String variant) {
		variantProperty.setValue(variant);
	}

	public void cycleVariant(boolean backwards) {
		this.setVariant(NMSManager.getProvider().cycleFrogVariant(this.getVariant(), backwards));
		// this.setVariant(EnumUtils.cycleEnumConstant(Frog.Variant.class, this.getVariant(),
		// backwards));
	}

	private void applyVariant() {
		Animals entity = this.getEntity();
		if (entity == null) return; // Not spawned
		NMSManager.getProvider().setFrogVariant(entity, this.getVariant());
		// entity.setVariant(this.getVariant());
	}

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem;
		switch (this.getVariant()) {
		case "TEMPERATE":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.ORANGE));
			break;
		case "WARM":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.LIGHT_GRAY));
			break;
		case "COLD":
		default:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.GREEN));
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonFrogVariant,
				Messages.buttonFrogVariantLore
		);
		return iconItem;
	}

	private Button getVariantEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getVariantEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				boolean backwards = clickEvent.isRightClick();
				cycleVariant(backwards);
				return true;
			}
		};
	}
}
