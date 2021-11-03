package com.nisovin.shopkeepers.shopobjects.living.types;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.nisovin.shopkeepers.api.shopkeeper.ShopCreationData;
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
import com.nisovin.shopkeepers.util.data.property.value.PropertyValue;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.BooleanSerializers;
import com.nisovin.shopkeepers.util.inventory.ItemUtils;

public class ChestedHorseShop<E extends ChestedHorse> extends BabyableShop<E> {

	public static final Property<Boolean> CARRYING_CHEST = new BasicProperty<Boolean>()
			.dataKeyAccessor("carryingChest", BooleanSerializers.LENIENT)
			.defaultValue(false)
			.build();

	private final PropertyValue<Boolean> carryingChestProperty = new PropertyValue<>(CARRYING_CHEST)
			.onValueChanged(this::applyCarryingChest)
			.build(properties);

	public ChestedHorseShop(LivingShops livingShops, SKLivingShopObjectType<? extends ChestedHorseShop<E>> livingObjectType,
							AbstractShopkeeper shopkeeper, ShopCreationData creationData) {
		super(livingShops, livingObjectType, shopkeeper, creationData);
	}

	@Override
	public void load(ShopObjectData shopObjectData) throws InvalidDataException {
		super.load(shopObjectData);
		carryingChestProperty.load(shopObjectData);
	}

	@Override
	public void save(ShopObjectData shopObjectData) {
		super.save(shopObjectData);
		carryingChestProperty.save(shopObjectData);
	}

	@Override
	protected void onSpawn() {
		super.onSpawn();
		this.applyCarryingChest();
	}

	@Override
	public List<Button> createEditorButtons() {
		List<Button> editorButtons = super.createEditorButtons();
		editorButtons.add(this.getCarryingChestEditorButton());
		return editorButtons;
	}

	// CARRYING CHEST

	public boolean isCarryingChest() {
		return carryingChestProperty.getValue();
	}

	public void setCarryingChest(boolean carryingChest) {
		carryingChestProperty.setValue(carryingChest);
	}

	public void cycleCarryingChest() {
		this.setCarryingChest(!this.isCarryingChest());
	}

	private void applyCarryingChest() {
		E entity = this.getEntity();
		if (entity == null) return; // Not spawned
		entity.setCarryingChest(this.isCarryingChest());
	}

	private ItemStack getCarryingChestEditorItem() {
		ItemStack iconItem = new ItemStack(Material.CHEST);
		ItemUtils.setDisplayNameAndLore(iconItem, Messages.buttonCarryingChest, Messages.buttonCarryingChestLore);
		return iconItem;
	}

	private Button getCarryingChestEditorButton() {
		return new ShopkeeperActionButton() {
			@Override
			public ItemStack getIcon(Session session) {
				return getCarryingChestEditorItem();
			}

			@Override
			protected boolean runAction(InventoryClickEvent clickEvent, Player player) {
				cycleCarryingChest();
				return true;
			}
		};
	}
}
