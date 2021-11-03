package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.DyeColor;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.Session;
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
public class AxolotlShop extends BabyableShop<Animals> {

	// TODO Use correct enum type once we only support Bukkit 1.17 upwards
	public static final Property<String> VARIANT = new BasicProperty<String>()
			.dataKeyAccessor("axolotlVariant", StringSerializers.STRICT)
			.validator(StringValidators.NON_EMPTY) // TODO Validate that the value is a valid axolotl type
			.defaultValue("LUCY")
			.build();

	private final PropertyValue<String> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(this::applyVariant)
			.build(properties);

	public AxolotlShop(	LivingShops livingShops, SKLivingShopObjectType<AxolotlShop> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		variantProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		variantProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyVariant();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
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
		// this.setVariant(EnumUtils.cycleEnumConstant(Axolotl.Variant.class, this.getVariant(), backwards));
	}

	private void applyVariant() {
		Animals entity = this.getEntity();
		if (entity == null) return; // Not spawned
		NMSManager.getProvider().setAxolotlVariant(this.getEntity(), this.getVariant());
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
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonAxolotlVariant, Messages.buttonAxolotlVariantLore);
		return iconItem;
	}

	private Button getVariantEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getVariantEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				boolean backwards = clickEvent.isRightClick();
				cycleVariant(backwards);
				return true;
			}
		};
	}
}
