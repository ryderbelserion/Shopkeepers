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
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.interaction.InteractionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ShopkeeperCreation {

	private final SKShopkeepersPlugin plugin;
	private final CreateListener createListener;

	// By player name:
	private final Map<String, List<String>> recentlyPlacedContainers = new HashMap<>();
	private final Map<String, Block> selectedContainer = new HashMap<>();

	public ShopkeeperCreation(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.createListener = new CreateListener(plugin, this);
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(new RecentlyPlacedContainersListener(this), plugin);
		createListener.onEnable();
	}

	public void onDisable() {
		createListener.onDisable();
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
		Validate.notNull(player, "player is null");
		Validate.notNull(container, "container is null");
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedContainers.computeIfAbsent(playerName, key -> new LinkedList<>());
		recentlyPlaced.add(TextUtils.getLocationString(container));
		if (recentlyPlaced.size() > 5) {
			recentlyPlaced.remove(0);
		}
	}

	public boolean isRecentlyPlacedContainer(Player player, Block container) {
		Validate.notNull(player, "player is null");
		Validate.notNull(container, "container is null");
		String playerName = player.getName();
		List<String> recentlyPlaced = recentlyPlacedContainers.get(playerName);
		return recentlyPlaced != null && recentlyPlaced.contains(TextUtils.getLocationString(container));
	}

	// SELECTED CONTAINER

	public void selectContainer(Player player, Block container) {
		Validate.notNull(player, "player is null");
		String playerName = player.getName();
		if (container == null) {
			selectedContainer.remove(playerName);
		} else {
			// This is validated once the container is actually used:
			assert ShopContainers.isSupportedContainer(container.getType());
			selectedContainer.put(playerName, container);
		}
	}

	public Block getSelectedContainer(Player player) {
		Validate.notNull(player, "player is null");
		return selectedContainer.get(player.getName());
	}

	// SHOPKEEPER CREATION

	// Checks if the player can use the given container for a player shopkeeper:
	public boolean handleCheckContainer(Player player, Block containerBlock) {
		Validate.notNull(player, "player is null");
		Validate.notNull(containerBlock, "containerBlock is null");
		// Check if the container is already used by some other shopkeeper:
		if (SKShopkeepersPlugin.getInstance().getProtectedContainers().isContainerProtected(containerBlock, null)) {
			TextUtils.sendMessage(player, Messages.containerAlreadyInUse);
			return false;
		}

		// Check for recently placed:
		if (Settings.requireContainerRecentlyPlaced && !this.isRecentlyPlacedContainer(player, containerBlock)) {
			// Container was not recently placed:
			TextUtils.sendMessage(player, Messages.containerNotPlaced);
			return false;
		}

		// Check if the player can access the container:
		if (!InteractionUtils.checkBlockInteract(player, containerBlock)) { // checks access via dummy interact event
			TextUtils.sendMessage(player, Messages.noContainerAccess);
			return false;
		}
		return true;
	}

	public Location determineSpawnLocation(Player player, Block targetBlock, BlockFace targetBlockFace) {
		Validate.notNull(player, "player is null");
		Validate.notNull(targetBlock, "targetBlock is null");
		Validate.notNull(targetBlockFace, "targetBlockFace is null");
		// If the target block is passable (and not a liquid, which can only come up as target block when we try to
		// place the shopkeeper on top of water or lava), spawn there, otherwise shift according to target block face:
		Block spawnBlock;
		if (targetBlock.isPassable() && !targetBlock.isLiquid()) {
			spawnBlock = targetBlock;
		} else {
			spawnBlock = targetBlock.getRelative(targetBlockFace);
		}
		Location spawnLocation = LocationUtils.getBlockCenterLocation(spawnBlock);
		if (targetBlockFace.getModY() == 0 && targetBlockFace != BlockFace.SELF) {
			// Set the yaw of the spawn location to match the direction of the target block face:
			// This is for example required for wall sign shopkeepers, but also allows placing living shopkeepers to be
			// rotated precisely into a specific direction.
			spawnLocation.setYaw(BlockFaceUtils.getYaw(targetBlockFace));
		} else {
			// Face towards the player:
			spawnLocation.setDirection(player.getEyeLocation().subtract(spawnLocation).toVector());
		}
		return spawnLocation;
	}
}
