package com.nisovin.shopkeepers.shopcreation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.container.ShopContainers;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils;
import com.nisovin.shopkeepers.util.bukkit.BlockLocation;
import com.nisovin.shopkeepers.util.bukkit.LocationUtils;
import com.nisovin.shopkeepers.util.bukkit.MutableBlockLocation;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.interaction.InteractionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class ShopkeeperCreation {

	private static final int MAX_TRACKED_CONTAINERS = 5;

	// This BlockLocation object is reused for lookups. It does not need to be reset after every
	// use.
	private static final MutableBlockLocation sharedBlockLocation = new MutableBlockLocation();

	private final SKShopkeepersPlugin plugin;
	private final CreateListener createListener;

	// By player name:
	private final Map<@NonNull String, @NonNull Deque<@NonNull BlockLocation>> recentlyPlacedContainers = new HashMap<>();
	private final Map<@NonNull String, @NonNull Block> selectedContainer = new HashMap<>();

	public ShopkeeperCreation(SKShopkeepersPlugin plugin) {
		this.plugin = plugin;
		this.createListener = new CreateListener(plugin, Unsafe.initialized(this));
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(new RecentlyPlacedContainersListener(this), plugin);
		createListener.onEnable();
	}

	public void onDisable() {
		createListener.onDisable();
		selectedContainer.clear();
		// Note: recentlyPlacedContainers does not get cleared here to persist across plugin
		// reloads.
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		String playerName = Unsafe.assertNonNull(player.getName());
		selectedContainer.remove(playerName);
		recentlyPlacedContainers.remove(playerName);
	}

	// RECENTLY PLACED CONTAINERS

	private BlockLocation getSharedKey(Block block) {
		sharedBlockLocation.set(block);
		return sharedBlockLocation;
	}

	public void addRecentlyPlacedContainer(Player player, Block container) {
		Validate.notNull(player, "player is null");
		Validate.notNull(container, "container is null");
		String playerName = Unsafe.assertNonNull(player.getName());
		Deque<@NonNull BlockLocation> recentlyPlaced = recentlyPlacedContainers.computeIfAbsent(
				playerName,
				key -> new ArrayDeque<>(MAX_TRACKED_CONTAINERS + 1)
		);
		assert recentlyPlaced != null;
		if (recentlyPlaced.size() == MAX_TRACKED_CONTAINERS) {
			recentlyPlaced.removeFirst();
		}
		recentlyPlaced.addLast(BlockLocation.of(container));
	}

	public boolean isRecentlyPlacedContainer(Player player, Block container) {
		Validate.notNull(player, "player is null");
		Validate.notNull(container, "container is null");
		String playerName = Unsafe.assertNonNull(player.getName());
		Deque<@NonNull BlockLocation> recentlyPlaced = recentlyPlacedContainers.get(playerName);
		if (recentlyPlaced == null) return false;
		BlockLocation containerLocation = this.getSharedKey(container);
		return recentlyPlaced.contains(containerLocation);
	}

	// SELECTED CONTAINER

	public void selectContainer(Player player, @Nullable Block container) {
		Validate.notNull(player, "player is null");
		String playerName = Unsafe.assertNonNull(player.getName());
		if (container == null) {
			selectedContainer.remove(playerName);
		} else {
			// This is validated once the container is actually used:
			assert ShopContainers.isSupportedContainer(container.getType());
			selectedContainer.put(playerName, container);
		}
	}

	public @Nullable Block getSelectedContainer(Player player) {
		Validate.notNull(player, "player is null");
		String playerName = Unsafe.assertNonNull(player.getName());
		return selectedContainer.get(playerName);
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
		if (Settings.requireContainerRecentlyPlaced
				&& !this.isRecentlyPlacedContainer(player, containerBlock)) {
			// Container was not recently placed:
			TextUtils.sendMessage(player, Messages.containerNotPlaced);
			return false;
		}

		// Check if the player can access the container by triggering a dummy interact event:
		if (!InteractionUtils.checkBlockInteract(player, containerBlock)) {
			TextUtils.sendMessage(player, Messages.noContainerAccess);
			return false;
		}
		return true;
	}

	public Location determineSpawnLocation(
			Player player,
			Block targetBlock,
			BlockFace targetBlockFace
	) {
		Validate.notNull(player, "player is null");
		Validate.notNull(targetBlock, "targetBlock is null");
		Validate.notNull(targetBlockFace, "targetBlockFace is null");
		// If the target block is passable (and not a liquid, which can only come up as target block
		// when we try to place the shopkeeper on top of water or lava), spawn there, otherwise
		// shift according to target block face:
		Block spawnBlock;
		if (targetBlock.isPassable() && !targetBlock.isLiquid()) {
			spawnBlock = targetBlock;
		} else {
			spawnBlock = targetBlock.getRelative(targetBlockFace);
		}
		Location spawnLocation = LocationUtils.getBlockCenterLocation(spawnBlock);
		if (targetBlockFace.getModY() == 0 && targetBlockFace != BlockFace.SELF) {
			// Set the yaw of the spawn location to match the direction of the target block face:
			// This is for example required for wall sign shopkeepers, but also allows placing
			// living shopkeepers to be rotated precisely into a specific direction.
			spawnLocation.setYaw(BlockFaceUtils.getYaw(targetBlockFace));
		} else {
			// Face towards the player:
			spawnLocation.setDirection(player.getEyeLocation().subtract(spawnLocation).toVector());
		}
		return spawnLocation;
	}
}
