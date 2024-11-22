package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Salmon;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
import com.nisovin.shopkeepers.compat.MC_1_21_3;
import com.nisovin.shopkeepers.compat.NMSManager;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.shopobjects.ShopObjectData;
import com.nisovin.shopkeepers.shopobjects.living.LivingShops;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;
import com.nisovin.shopkeepers.util.java.CollectionUtils;

public class SalmonShop extends SKLivingShopObject<Salmon> {

	// TODO Replace with the actual type once we only support MC 1.21.3+
	public static final Property<String> VARIANT = new BasicProperty<String>()
			.dataKeyAccessor("variant", StringSerializers.STRICT_NON_EMPTY)
			.defaultValue("MEDIUM")
			.build();

	private final PropertyValue<String> variantProperty = new PropertyValue<>(VARIANT)
			.onValueChanged(Unsafe.initialized(this)::applyVariant)
			.build(properties);

	public SalmonShop(
			LivingShops livingShops,
			SKLivingShopObjectType<SalmonShop> livingObjectType,
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
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		if (MC_1_21_3.isAvailable()) {
			editorButtons.add(this.getVariantEditorButton());
		}
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
		this.setVariant(CollectionUtils.cycleValue(MC_1_21_3.SALMON_VARIANTS, this.getVariant(), backwards));
	}

	private void applyVariant() {
		Salmon entity = this.getEntity();
		if (entity == null) return; // Not spawned

		NMSManager.getProvider().setSalmonVariant(entity, this.getVariant());
	}

	private ItemStack getVariantEditorItem() {
		ItemStack iconItem = new ItemStack(Material.SALMON);
		ItemUtils.setDisplayNameAndLore(
				iconItem,
				Messages.buttonSalmonVariant,
				Messages.buttonSalmonVariantLore
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
