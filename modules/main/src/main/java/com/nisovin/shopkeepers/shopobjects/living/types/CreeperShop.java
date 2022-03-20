package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Creeper;
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
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObject;
import com.nisovin.shopkeepers.shopobjects.living.SKLivingShopObjectType;
import com.nisovin.shopkeepers.ui.editor.Button;
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class CreeperShop extends SKLivingShopObject<@NonNull Creeper> {

	public static final Property<@NonNull Boolean> POWERED = new BasicProperty<@NonNull Boolean>()
			.dataKeyAccessor("powered", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<@NonNull Boolean> poweredProperty = new PropertyValue<>(POWERED)
			.onValueChanged(Unsafe.initialized(this)::applyPowered)
			.build(properties);

	public CreeperShop(
			LivingShops livingShops,
			SKLivingShopObjectType<@NonNull CreeperShop> livingObjectType,
			AbstractShopkeeper shopkeeper,
			@Nullable ShopCreationData creationData
	) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		poweredProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData, boolean saveAll) {
		super.save(shopObjectData, saveAll);
		poweredProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyPowered();
	}

	@Override
	public List<@NonNull Button> createEditorButtons() {
		List<@NonNull Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getPoweredEditorButton());
		return editorButtons;
	}

	// POWERED STATE

	public boolean isPowered() {
		return poweredProperty.getValue();
	}

	public void setPowered(boolean powered) {
		poweredProperty.setValue(powered);
	}

	public void cyclePowered() {
		this.setPowered(!this.isPowered());
	}

	private void applyPowered() {
		Creeper entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setPowered(this.isPowered());
	}

	private ItemStack getPoweredEditorItem() {
		ItemStack iconItem;
		if (this.isPowered()) {
			iconItem = new ItemStack(Material.LIGHT_BLUE_WOOL);
		} else {
			iconItem = new ItemStack(Material.LIME_WOOL);
		}
		ItemUtils.setDisplayNameAndLore(iconItem,
				Messages.buttonCreeperCharged,
				Messages.buttonCreeperChargedLore
		);
		return iconItem;
	}

	private Button getPoweredEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public @Nullable ItemStack getIcon(EditorSession editorSession) {
				return getPoweredEditorItem();
			}

			@Override
			protected boolean runAction(
					EditorSession editorSession,
					InventoryClickEvent clickEvent
			) {
				cyclePowered();
				return true;
			}
		};
	}
}
