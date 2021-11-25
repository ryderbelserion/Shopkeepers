package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Ageable;
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
import com.nisovin.shopkeepers.ui.editor.EditorSession;
import com.nisovin.shopkeepers.ui.editor.ShopkeeperActionButton;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class BabyableShop<E extends Ageable> extends SKLivingShopObject<E> {

	public static final Property<Boolean> BABY = new BasicProperty<Boolean>()
			.dataKeyAccessor("baby", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> babyProperty = new PropertyValue<>(BABY)
			.onValueChanged(this::applyBaby)
			.build(properties);

	public BabyableShop(LivingShops livingShops, SKLivingShopObjectType<? extends BabyableShop<E>> livingObjectType,
						AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	// TODO Remove special case once this is resolved differently.
	protected boolean isBabyable() {
		// Some mobs don't support the baby variant even though they are Ageable.
		switch (this.getEntityType().name()) {
		case "PARROT":
		case "WANDERING_TRADER":
		case "PIGLIN_BRUTE": // Added in MC 1.16.2
			return false;
		default:
			return true;
		}
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		if (this.isBabyable()) {
			babyProperty.load(shopObjectData);
		}
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		if (this.isBabyable()) {
			babyProperty.save(shopObjectData);
		}
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyBaby();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		if (this.isBabyable()) {
			editorButtons.add(this.getBabyEditorButton());
		}
		return editorButtons;
	}

	// BABY

	public boolean isBaby() {
		return babyProperty.getValue();
	}

	public void setBaby(boolean baby) {
		if (!this.isBabyable()) return;
		babyProperty.setValue(baby);
	}

	public void cycleBaby() {
		this.setBaby(!this.isBaby());
	}

	private void applyBaby() {
		if (!this.isBabyable()) return;
		E entity = this.getEntity();
		if (entity == null) return; // Not spawned
		if (this.isBaby()) {
			entity.setBaby();
		} else {
			entity.setAdult();
			// TODO: MC-9568: Growing up mobs get moved.
			this.teleportBack();
		}
	}

	private ItemStack getBabyEditorItem() {
		ItemStack iconItem = new ItemStack(Material.EGG);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonBaby, Messages.buttonBabyLore);
		return iconItem;
	}

	private Button getBabyEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(EditorSession editorSession) {
				return getBabyEditorItem();
			}

			@Override
			protected boolean runAction(EditorSession editorSession, InventoryClickEvent clickEvent) {
				cycleBaby();
				return true;
			}
		};
	}
}
