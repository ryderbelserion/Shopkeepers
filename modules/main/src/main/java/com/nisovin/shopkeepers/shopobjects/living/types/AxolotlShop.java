package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.entity.Animals;
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
import com.nisovin.shopkeepers.util.data.property.validation.java.StringValidators;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

// TODO Use actual Axolotl type once we only support Bukkit 1.17 upwards
// TODO Editor option to play dead?
public class AxolotlShop extends BabyableShop<@NonNull Animals> {

	// TODO Use correct enum type once we only support Bukkit 1.17 upwards
	public static final Property<@NonNull String> VARIANT = new BasicProperty<@NonNull String>()
			.dataKeyAccessor("axolotlVariant", StringSerializers.STRICT)
			// TODO Validate that the value is a valid axolotl type
			.validator(StringValidators.NON_EMPTY)
			.defaultValue("LUCY")
			.build();

	private final PropertyValue<@NonNull String> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);

	public AxolotlShop(
			LivingShops livingShops,
			SKLivingShopObjectType<@NonNull AxolotlShop> livingObjectType,
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
		this.setVariant(NMSManager.getProvider().cycleAxolotlVariant(this.getVariant(), backwards));
		// this.setVariant(EnumUtils.cycleEnumConstant(Axolotl.Variant.class, this.getVariant(),
		// backwards));
	}

	private void applyVariant() {
		Animals entity = this.getEntity();
		if (entity == null) return; // Not spawned
		NMSManager.getProvider().setAxolotlVariant(entity, this.getVariant());
		// entity.setVariant(this.getVariant());
	}

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem;
		switch (this.getVariant()) {
		case "LUCY":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.PINK));
			break;
		case "WILD":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.BROWN));
			break;
		case "GOLD":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.YELLOW));
			break;
		case "CYAN":
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.WHITE));
			break;
		case "BLUE":
		default:
			iconItem = new ItemStack(ItemUtils.getWoolType(DyeColor.BLUE));
			break;
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonAxolotlVariant,
				Messages.buttonAxolotlVariantLore
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
