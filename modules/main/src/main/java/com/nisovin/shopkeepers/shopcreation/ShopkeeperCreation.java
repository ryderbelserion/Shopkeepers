package com.nisovin.shopkeepers.shopcreation;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.container.protection.ProtectedContainers;
import com.nisovin.shopkeepers.util.java.Validate;

public class ShopkeeperCreation {

	private final ContainerSelection containerSelection;
	private final ShopkeeperPlacement shopkeeperPlacement;

	private final CreateListener createListener;

	public ShopkeeperCreation(
			SKShopkeepersPlugin plugin,
			ShopkeeperRegistry shopkeeperRegistry,
			ProtectedContainers protectedContainers
	) {
		Validate.notNull(plugin, "plugin is null");

		this.containerSelection = new ContainerSelection(plugin, protectedContainers);
		this.shopkeeperPlacement = new ShopkeeperPlacement(shopkeeperRegistry);

		this.createListener = new CreateListener(plugin, containerSelection, shopkeeperPlacement);
	}

	public void onEnable() {
		containerSelection.onEnable();
		createListener.onEnable();
	}

	public void onDisable() {
		createListener.onDisable();
		containerSelection.onDisable();
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		containerSelection.onPlayerQuit(player);
	}

	public ContainerSelection getContainerSelection() {
		return containerSelection;
	}

	public ShopkeeperPlacement getShopkeeperPlacement() {
		return shopkeeperPlacement;
	}
}
