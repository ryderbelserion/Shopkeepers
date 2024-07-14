package com.nisovin.shopkeepers.ui.villager.equipmentEditor;

import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.util.java.Validate;

public class VillagerEquipmentEditorUI {

	public static boolean request(AbstractVillager entity, Player player) {
		Validate.notNull(entity, "entity is null");
		Validate.notNull(player, "player is null");

		VillagerEquipmentEditorHandler ui = new VillagerEquipmentEditorHandler(entity);
		return SKShopkeepersPlugin.getInstance().getUIRegistry().requestUI(ui, player);
	}

	private VillagerEquipmentEditorUI() {
	}
}
