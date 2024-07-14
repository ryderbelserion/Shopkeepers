package com.nisovin.shopkeepers.ui.equipmentEditor;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.bukkit.inventory.EquipmentSlot;
import org.checkerframework.checker.nullness.qual.NonNull;
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
			List<? extends @NonNull EquipmentSlot> supportedSlots,
			Map<? extends @NonNull EquipmentSlot, ? extends @NonNull UnmodifiableItemStack> currentEquipment,
			BiConsumer<@NonNull EquipmentSlot, @Nullable UnmodifiableItemStack> onEquipmentChanged
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
