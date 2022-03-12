package com.nisovin.shopkeepers.commands;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.shopkeepers.ShopkeepersCommand;

public class Commands {

	private final SKShopkeepersPlugin plugin;
	private final Confirmations confirmations;
	private ShopkeepersCommand shopkeepersCommand;

	public Commands(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.confirmations = new Confirmations(plugin);
	}

	public void onEnable() {
		confirmations.onEnable();
		// Register command executor:
		shopkeepersCommand = new ShopkeepersCommand(plugin, confirmations);
	}

	public void onDisable() {
		confirmations.onDisable();
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		confirmations.onPlayerQuit(player);
	}

	public ShopkeepersCommand getShopkeepersCommand() {
		return shopkeepersCommand;
	}
}
