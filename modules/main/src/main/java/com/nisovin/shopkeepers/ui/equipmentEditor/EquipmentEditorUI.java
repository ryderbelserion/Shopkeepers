package com.nisovin.shopkeepers.ui.equipmentEditor;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.util.UnmodifiableItemStack;
import com.nisovin.shopkeepers.shopkeeper.AbstractShopkeeper;
import com.nisovin.shopkeepers.util.java.Validate;

public class EquipmentEditorUI {

	public static boolean request(
			AbstractShopkeeper shopkeeper,
			Player player,
			List<? extends EquipmentSlot> supportedSlots,
			Map<? extends EquipmentSlot, ? extends UnmodifiableItemStack> currentEquipment,
			BiConsumer<EquipmentSlot, @Nullable UnmodifiableItemStack> onEquipmentChanged
	) {
		Validate.notNull(shopkeeper, "shopkeeper is null");
		Validate.notNull(player, "player is null");
		Validate.notNull(currentEquipment, "currentEquipment is null");
		Validate.notNull(onEquipmentChanged, "onEquipmentChanged is null");

		ShopkeeperEquipmentEditorHandler uiHandler = new ShopkeeperEquipmentEditorHandler(
				shopkeeper,
				supportedSlots,
				currentEquipment,
				onEquipmentChanged
		);
		return SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(uiHandler, player);
	}

	private EquipmentEditorUI() {
	}
}
