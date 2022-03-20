package com.nisovin.shopkeepers.commands;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.commands.shopkeepers.ShopkeepersCommand;
import com.nisovin.shopkeepers.util.java.Validate;

public class Commands {

	private final SKShopkeepersPlugin plugin;
	private final Confirmations confirmations;

	private @Nullable ShopkeepersCommand shopkeepersCommand;

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
		return Validate.State.notNull(shopkeepersCommand, "The commands have not yet been set up!");
	}
}
