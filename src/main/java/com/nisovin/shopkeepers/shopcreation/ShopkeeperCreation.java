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
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.ItemUtils;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Utils;

public class ShopkeeperCreation {

	private final SKShopkeepersPlugin plugin;
	private final Map<String, List<String>> recentlyPlacedChests = new HashMap<>();
	private final Map<String, Block> selectedChest = new HashMap<>();

	public ShopkeeperCreation(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(new RecentlyPlacedChestsListener(this), plugin);
		Bukkit.getPluginManager().registerEvents(new CreateListener(plugin, this), plugin);
	}

	public void onDisable() {
		selectedChest.clear();
		// note: recentlyPlacedChests does not get cleared here to persist across plugin reloads
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		String playerName = player.getName();
		selectedChest.remove(playerName);
		recentlyPlacedChests.remove(playerName);
	}

	// RECENTLY PLACED CHESTS

	public void addRecentlyPlacedChest(Player player, Block chest) {
		assert player != null && chest != null;
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedChests.get(playerName);
		if (recentlyPlaced == null) {
			recentlyPlaced = new LinkedList<>();
			recentlyPlacedChests.put(playerName, recentlyPlaced);
		}
		recentlyPlaced.add(TextUtils.getLocationString(chest));
		if (recentlyPlaced.size() > 5) {
			recentlyPlaced.remove(0);
		}
	}

	public boolean isRecentlyPlacedChest(Player player, Block chest) {
		assert player != null && chest != null;
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedChests.get(playerName);
		return recentlyPlaced != null && recentlyPlaced.contains(TextUtils.getLocationString(chest));
	}

	// SELECTED CHEST

	public void selectChest(Player player, Block chest) {
		assert player != null;
		String playerName = player.getName();
		if (chest == null) selectedChest.remove(playerName);
		else {
			assert ItemUtils.isChest(chest.getType());
			selectedChest.put(playerName, chest);
		}
	}

	public Block getSelectedChest(Player player) {
		assert player != null;
		return selectedChest.get(player.getName());
	}

	// SHOPKEEPER CREATION

	// checks if the player can use the given chest for a player shopkeeper:
	public boolean handleCheckChest(Player player, Block chestBlock) {
		// check if this chest is already used by some other shopkeeper:
		if (SKShopkeepersPlugin.getInstance().getProtectedChests().isChestProtected(chestBlock, null)) {
			TextUtils.sendMessage(player, Settings.msgChestAlreadyInUse);
			return false;
		}

		// check for recently placed:
		if (Settings.requireChestRecentlyPlaced && !plugin.getShopkeeperCreation().isRecentlyPlacedChest(player, chestBlock)) {
			// chest was not recently placed:
			TextUtils.sendMessage(player, Settings.msgChestNotPlaced);
			return false;
		}

		// check if the player can access the chest:
		if (!Utils.checkBlockInteract(player, chestBlock)) { // checks access via dummy interact event
			TextUtils.sendMessage(player, Settings.msgNoChestAccess);
			return false;
		}
		return true;
	}

	public Location determineSpawnLocation(Player player, Block targetBlock, BlockFace targetBlockFace) {
		assert player != null && targetBlock != null && targetBlockFace != null;
		// if the target block is passable, spawn there, otherwise shift according to target block face:
		Block spawnBlock;
		if (targetBlock.isPassable()) {
			spawnBlock = targetBlock;
		} else {
			spawnBlock = targetBlock.getRelative(targetBlockFace);
		}
		Location spawnLocation = Utils.getBlockCenterLocation(spawnBlock);
		// face towards player:
		spawnLocation.setDirection(player.getEyeLocation().subtract(spawnLocation).toVector());
		return spawnLocation;
	}
}
