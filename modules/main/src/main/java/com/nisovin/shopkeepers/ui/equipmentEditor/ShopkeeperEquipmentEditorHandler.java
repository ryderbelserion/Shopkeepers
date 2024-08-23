package com.nisovin.shopkeepers.ui.equipmentEditor;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.bukkit.inventory.EquipmentSlot;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.ui.SKDefaultUITypes;
import com.nisovin.shopkeepers.ui.ShopkeeperUIHandler;
import com.nisovin.shopkeepers.util.java.Validate;

public class ShopkeeperEquipmentEditorHandler extends AbstractEquipmentEditorHandler
		implements ShopkeeperUIHandler {

	private final AbstractShopkeeper shopkeeper;

	ShopkeeperEquipmentEditorHandler(
			AbstractShopkeeper shopkeeper,
			List<? extends EquipmentSlot> supportedSlots,
			Map<? extends EquipmentSlot, ? extends UnmodifiableItemStack> currentEquipment,
			BiConsumer<EquipmentSlot, @Nullable UnmodifiableItemStack> onEquipmentChanged
	) {
		super(
				SKDefaultUITypes.EQUIPMENT_EDITOR(),
				supportedSlots,
				currentEquipment,
				onEquipmentChanged
		);

		Validate.notNull(shopkeeper, "shopkeeper is null");
		this.shopkeeper = shopkeeper;
	}

	@Override
	public AbstractShopkeeper getShopkeeper() {
		return shopkeeper;
	}
}
