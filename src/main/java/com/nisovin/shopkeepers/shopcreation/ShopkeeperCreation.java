package com.nisovin.shopkeepers.shopcreation;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.ItemUtils;
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
		recentlyPlaced.add(Utils.getLocationString(chest));
		if (recentlyPlaced.size() > 5) {
			recentlyPlaced.remove(0);
		}
	}

	public boolean isRecentlyPlacedChest(Player player, Block chest) {
		assert player != null && chest != null;
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedChests.get(playerName);
		return recentlyPlaced != null && recentlyPlaced.contains(Utils.getLocationString(chest));
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
	public boolean handleCheckChest(Player player, Block chestBlock, boolean checkChestAccess) {
		// check if this chest is already used by some other shopkeeper:
		if (SKShopkeepersPlugin.getInstance().getProtectedChests().isChestProtected(chestBlock, null)) {
			Utils.sendMessage(player, Settings.msgChestAlreadyInUse);
			return false;
		}

		// check for recently placed:
		if (Settings.requireChestRecentlyPlaced && !plugin.getShopkeeperCreation().isRecentlyPlacedChest(player, chestBlock)) {
			// chest was not recently placed:
			Utils.sendMessage(player, Settings.msgChestNotPlaced);
			return false;
		}

		// check if the player can access the chest:
		if (checkChestAccess && !plugin.getShopkeeperCreation().checkChestAccess(player, chestBlock)) {
			Utils.sendMessage(player, Settings.msgNoChestAccess);
			return false;
		}
		return true;
	}

	// checks if the player can access the given chest:
	public boolean checkChestAccess(Player player, Block chestBlock) {
		// simulating right click on the chest to check if access is denied:
		// making sure that access is really denied, and that the event is not cancelled because of denying
		// usage with the items in hands:
		PlayerInventory playerInventory = player.getInventory();
		ItemStack itemInMainHand = playerInventory.getItemInMainHand();
		ItemStack itemInOffHand = playerInventory.getItemInOffHand();
		playerInventory.setItemInMainHand(null);
		playerInventory.setItemInOffHand(null);

		TestPlayerInteractEvent fakeInteractEvent = new TestPlayerInteractEvent(player, Action.RIGHT_CLICK_BLOCK, null, chestBlock, BlockFace.UP);
		Bukkit.getPluginManager().callEvent(fakeInteractEvent);
		boolean canAccessChest = (fakeInteractEvent.useInteractedBlock() != Result.DENY);

		// resetting items in main and off hand:
		playerInventory.setItemInMainHand(itemInMainHand);
		playerInventory.setItemInOffHand(itemInOffHand);
		return canAccessChest;
	}
}
