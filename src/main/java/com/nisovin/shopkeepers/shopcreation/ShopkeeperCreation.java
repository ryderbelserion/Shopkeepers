package com.nisovin.shopkeepers.shopcreation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Utils;

public class ShopkeeperCreation {

	private final SKShopkeepersPlugin plugin;
	private final Map<String, List<String>> recentlyPlacedContainers = new HashMap<>();
	private final Map<String, Block> selectedContainer = new HashMap<>();

	public ShopkeeperCreation(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(new RecentlyPlacedContainersListener(this), plugin);
		Bukkit.getPluginManager().registerEvents(new CreateListener(plugin, this), plugin);
	}

	public void onDisable() {
		selectedContainer.clear();
		// Note: recentlyPlacedContainers does not get cleared here to persist across plugin reloads.
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		String playerName = player.getName();
		selectedContainer.remove(playerName);
		recentlyPlacedContainers.remove(playerName);
	}

	// RECENTLY PLACED CONTAINERS

	public void addRecentlyPlacedContainer(Player player, Block container) {
		assert player != null && container != null;
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedContainers.get(playerName);
		if (recentlyPlaced == null) {
			recentlyPlaced = new LinkedList<>();
			recentlyPlacedContainers.put(playerName, recentlyPlaced);
		}
		recentlyPlaced.add(TextUtils.getLocationString(container));
		if (recentlyPlaced.size() > 5) {
			recentlyPlaced.remove(0);
		}
	}

	public boolean isRecentlyPlacedContainer(Player player, Block container) {
		assert player != null && container != null;
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedContainers.get(playerName);
		return recentlyPlaced != null && recentlyPlaced.contains(TextUtils.getLocationString(container));
	}

	// SELECTED CONTAINER

	public void selectContainer(Player player, Block container) {
		assert player != null;
		String playerName = player.getName();
		if (container == null) {
			selectedContainer.remove(playerName);
		} else {
			assert ShopContainers.isSupportedContainer(container.getType());
			selectedContainer.put(playerName, container);
		}
	}

	public Block getSelectedContainer(Player player) {
		assert player != null;
		return selectedContainer.get(player.getName());
	}

	// SHOPKEEPER CREATION

	// Checks if the player can use the given container for a player shopkeeper:
	public boolean handleCheckContainer(Player player, Block containerBlock) {
		// Check if the container is already used by some other shopkeeper:
		if (SKShopkeepersPlugin.getInstance().getProtectedContainers().isContainerProtected(containerBlock, null)) {
			TextUtils.sendMessage(player, Messages.containerAlreadyInUse);
			return false;
		}

		// Check for recently placed:
		if (Settings.requireContainerRecentlyPlaced && !plugin.getShopkeeperCreation().isRecentlyPlacedContainer(player, containerBlock)) {
			// Container was not recently placed:
			TextUtils.sendMessage(player, Messages.containerNotPlaced);
			return false;
		}

		// Check if the player can access the container:
		if (!Utils.checkBlockInteract(player, containerBlock)) { // checks access via dummy interact event
			TextUtils.sendMessage(player, Messages.noContainerAccess);
			return false;
		}
		return true;
	}

	public Location determineSpawnLocation(Player player, Block targetBlock, BlockFace targetBlockFace) {
		assert player != null && targetBlock != null && targetBlockFace != null;
		// If the target block is passable, spawn there, otherwise shift according to target block face:
		Block spawnBlock;
		if (targetBlock.isPassable()) {
			spawnBlock = targetBlock;
		} else {
			spawnBlock = targetBlock.getRelative(targetBlockFace);
		}
		Location spawnLocation = Utils.getBlockCenterLocation(spawnBlock);
		// Face towards player:
		spawnLocation.setDirection(player.getEyeLocation().subtract(spawnLocation).toVector());
		return spawnLocation;
	}
}
