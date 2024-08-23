package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.SKShopkeepersPlugin;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopobjects.ShopObject;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.config.Settings;
import com.nisovin.shopkeepers.shopkeeper.activation.ShopkeeperChunkActivator;
import com.nisovin.shopkeepers.shopkeeper.registry.SKShopkeeperRegistry;
import com.nisovin.shopkeepers.shopkeeper.spawning.ShopkeeperSpawner;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityAI;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.taskqueue.TaskQueueStatistics;
import com.nisovin.shopkeepers.util.timer.Timings;

class CommandCheck extends Command {

	private static final String ARGUMENT_CHUNKS = "chunks";
	private static final String ARGUMENT_ACTIVE = "active";

	private final SKShopkeepersPlugin plugin;
	private final SKShopkeeperRegistry shopkeeperRegistry;
	private final ShopkeeperSpawner shopkeeperSpawner;
	private final ShopkeeperChunkActivator chunkActivator;

	CommandCheck(SKShopkeepersPlugin plugin) {
		super("check");
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();
		this.shopkeeperSpawner = shopkeeperRegistry.getShopkeeperSpawner();
		this.chunkActivator = shopkeeperRegistry.getChunkActivator();

		// Set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// Set description:
		this.setDescription(Text.of("Shows various debugging information."));

		// Hidden debugging command:
		this.setHiddenInParentHelp(true);

		// Arguments:
		this.addArgument(new FirstOfArgument("context", Arrays.asList(
				new LiteralArgument(ARGUMENT_CHUNKS),
				new LiteralArgument(ARGUMENT_ACTIVE)
		), true).optional()); // Join formats
	}

	@Override
	protected void execute(CommandInput input, CommandContextView context) throws CommandException {
		CommandSender sender = input.getSender();
		boolean isConsole = (sender instanceof ConsoleCommandSender);

		boolean listChunks = context.has(ARGUMENT_CHUNKS);
		boolean listActive = context.has(ARGUMENT_ACTIVE);

		LivingEntityAI livingEntityAI = plugin.getLivingShops().getLivingEntityAI();

		int totalChunksWithShopkeepers = shopkeeperRegistry.getWorldsWithShopkeepers().stream()
				.map(worldName -> shopkeeperRegistry.getShopkeepersByChunks(worldName))
				.mapToInt(byChunk -> byChunk.size())
				.sum();

		sender.sendMessage(ChatColor.YELLOW + "All shopkeepers:");
		sender.sendMessage("  Total: " + shopkeeperRegistry.getAllShopkeepers().size()
				+ "    (Virtual: " + shopkeeperRegistry.getVirtualShopkeepers().size() + ")");
		sender.sendMessage("  Unsaved dirty | deleted | dirty storage: "
				+ plugin.getShopkeeperStorage().getUnsavedDirtyShopkeepersCount()
				+ " | " + plugin.getShopkeeperStorage().getUnsavedDeletedShopkeepersCount()
				+ " | " + plugin.getShopkeeperStorage().isDirty());
		sender.sendMessage("  Chunks with shopkeepers: " + totalChunksWithShopkeepers);
		sender.sendMessage("    With active AI: " + livingEntityAI.getActiveAIChunksCount());
		sender.sendMessage("    With active gravity: " + livingEntityAI.getActiveGravityChunksCount());
		sender.sendMessage("  Active shopkeepers: " + shopkeeperRegistry.getActiveShopkeepers().size());
		sender.sendMessage("    With AI: " + livingEntityAI.getEntityCount());
		sender.sendMessage("    With active AI: " + livingEntityAI.getActiveAIEntityCount());
		sender.sendMessage("    With active gravity: " + livingEntityAI.getActiveGravityEntityCount());

		TaskQueueStatistics spawnQueueStatistics = shopkeeperSpawner.getSpawnQueueStatistics();
		sender.sendMessage("  Pending shopkeeper spawns | max: " + spawnQueueStatistics.getPendingCount()
				+ " | " + spawnQueueStatistics.getMaxPendingCount());

		Timings chunkActivationTimings = chunkActivator.getChunkActivationTimings();
		double avgChunkActivationTimings = chunkActivationTimings.getAverageTimeMillis();
		double maxChunkActivationTimings = chunkActivationTimings.getMaxTimeMillis();
		sender.sendMessage("  Chunk activation timings (avg | max | cnt): "
				+ TextUtils.format(avgChunkActivationTimings) + " ms"
				+ " | " + TextUtils.format(maxChunkActivationTimings) + " ms"
				+ " | " + chunkActivationTimings.getCounter());

		double avgTotalAITimings = livingEntityAI.getTotalTimings().getAverageTimeMillis();
		double maxTotalAITiming = livingEntityAI.getTotalTimings().getMaxTimeMillis();
		sender.sendMessage("  Total AI timings (per " + Settings.mobBehaviorTickPeriod
				+ " ticks) (avg | max): "
				+ TextUtils.format(avgTotalAITimings) + " ms"
				+ " | " + TextUtils.format(maxTotalAITiming) + " ms");

		// Note: These are per activation, which happens only every 20 ticks (not per tick).
		double avgAIActivationTimings = livingEntityAI.getActivationTimings().getAverageTimeMillis();
		double maxAIActivationTiming = livingEntityAI.getActivationTimings().getMaxTimeMillis();
		sender.sendMessage("    AI activation timings (per "
				+ LivingEntityAI.AI_ACTIVATION_TICK_RATE + " ticks) (avg | max): "
				+ TextUtils.format(avgAIActivationTimings) + " ms"
				+ " | " + TextUtils.format(maxAIActivationTiming) + " ms");

		double avgGravityTimings = livingEntityAI.getGravityTimings().getAverageTimeMillis();
		double maxGravityTiming = livingEntityAI.getGravityTimings().getMaxTimeMillis();
		sender.sendMessage("    Gravity timings (per " + Settings.mobBehaviorTickPeriod
				+ " ticks) (avg | max): "
				+ TextUtils.format(avgGravityTimings) + " ms"
				+ " | " + TextUtils.format(maxGravityTiming) + " ms");

		double avgAITimings = livingEntityAI.getAITimings().getAverageTimeMillis();
		double maxAITiming = livingEntityAI.getAITimings().getMaxTimeMillis();
		sender.sendMessage("    AI timings (per " + Settings.mobBehaviorTickPeriod
				+ " ticks) (avg | max): "
				+ TextUtils.format(avgAITimings) + " ms"
				+ " | " + TextUtils.format(maxAITiming) + " ms");

		for (World world : Bukkit.getWorlds()) {
			String worldName = world.getName();
			Chunk[] worldLoadedChunks = world.getLoadedChunks();
			int chunksWithLoadedEntities = 0;
			List<Entity> worldEntities = world.getEntities();
			int worldDeadEntities = 0;
			int worldInvalidEntities = 0;
			for (Entity entity : worldEntities) {
				if (entity.isDead()) ++worldDeadEntities;
				if (!entity.isValid()) ++worldInvalidEntities;
			}
			int worldDeadEntitiesInChunks = 0;
			int worldInvalidEntitiesInChunks = 0;
			for (Chunk chunk : worldLoadedChunks) {
				if (!chunk.isEntitiesLoaded()) continue;

				chunksWithLoadedEntities++;
				for (Entity entity : chunk.getEntities()) {
					if (entity.isDead()) ++worldDeadEntitiesInChunks;
					if (!entity.isValid()) ++worldInvalidEntitiesInChunks;
				}
			}

			int worldTotalShopkeepers = shopkeeperRegistry.getShopkeepersInWorld(worldName).size();
			int worldActiveChunks = shopkeeperRegistry.getActiveChunks(worldName).size();
			int worldShopkeepersInActiveChunks = shopkeeperRegistry.getActiveShopkeepers(worldName).size();

			int worldChunksWithShopkeepers = 0;
			int worldLoadedChunksWithShopkeepers = 0;
			int worldShopkeepersInLoadedChunks = 0;

			Map<? extends ChunkCoords, ? extends Collection<? extends Shopkeeper>> shopkeepersByChunks = shopkeeperRegistry.getShopkeepersByChunks(worldName);
			for (Entry<? extends ChunkCoords, ? extends Collection<? extends Shopkeeper>> chunkEntry : shopkeepersByChunks.entrySet()) {
				ChunkCoords chunkCoords = chunkEntry.getKey();
				Collection<? extends Shopkeeper> chunkShopkeepers = chunkEntry.getValue();

				worldChunksWithShopkeepers++;
				if (chunkCoords.isChunkLoaded()) {
					worldLoadedChunksWithShopkeepers++;
					worldShopkeepersInLoadedChunks += chunkShopkeepers.size();
				}
			}

			sender.sendMessage(ChatColor.YELLOW + "Shopkeepers in world '" + world.getName() + "':");
			sender.sendMessage("  Total: " + worldTotalShopkeepers);
			sender.sendMessage("  Entities | invalid | dead: " + worldEntities.size() + " | "
					+ worldInvalidEntities + " | " + worldDeadEntities);
			sender.sendMessage("  Entities in chunks (invalid | dead): "
					+ worldInvalidEntitiesInChunks + " | " + worldDeadEntitiesInChunks);
			sender.sendMessage("  Loaded chunks: " + worldLoadedChunks.length
					+ " (with loaded entities: " + chunksWithLoadedEntities + ")");
			if (worldTotalShopkeepers > 0) {
				sender.sendMessage("  Chunks with shopkeepers | loaded | active: "
						+ worldChunksWithShopkeepers + " | " + worldLoadedChunksWithShopkeepers
						+ " | " + worldActiveChunks);
				sender.sendMessage("  Shopkeepers in chunks (loaded | active): "
						+ worldShopkeepersInLoadedChunks + " | " + worldShopkeepersInActiveChunks);
			}

			// List all chunks containing shopkeepers:
			if (isConsole && listChunks && worldTotalShopkeepers > 0) {
				sender.sendMessage("  Listing of all chunks with shopkeepers:");
				int line = 0;
				for (Entry<? extends ChunkCoords, ? extends Collection<? extends Shopkeeper>> chunkEntry : shopkeepersByChunks.entrySet()) {
					ChunkCoords chunkCoords = chunkEntry.getKey();
					Collection<? extends Shopkeeper> chunkShopkeepers = chunkEntry.getValue();

					line++;
					ChatColor lineColor = (line % 2 == 0 ? ChatColor.WHITE : ChatColor.GRAY);
					sender.sendMessage("    (" + lineColor
							+ chunkCoords.getChunkX() + "," + chunkCoords.getChunkZ()
							+ ChatColor.RESET + ") ["
							+ (chunkCoords.isChunkLoaded() ? ChatColor.GREEN + "loaded" : ChatColor.DARK_GRAY + "unloaded")
							+ ChatColor.RESET + " | "
							+ (shopkeeperRegistry.isChunkActive(chunkCoords) ? ChatColor.GREEN + "active" : ChatColor.DARK_GRAY + "inactive")
							+ ChatColor.RESET + "]: " + chunkShopkeepers.size());
				}
			}
		}

		// List all active shopkeepers:
		if (isConsole && listActive) {
			sender.sendMessage("All active shopkeepers:");
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getActiveShopkeepers()) {
				ShopObject shopObject = shopkeeper.getShopObject();
				if (shopObject.isActive()) {
					Location location = Unsafe.assertNonNull(shopObject.getLocation());
					sender.sendMessage(shopkeeper.getLocatedLogPrefix() + "Active ("
							+ TextUtils.getLocationString(location) + ")");
				} else {
					sender.sendMessage(shopkeeper.getLocatedLogPrefix() + "INACTIVE");
				}
			}
		}

		if (!isConsole && (listChunks || listActive)) {
			sender.sendMessage("More information is printed when the command is run from console.");
		}
	}
}
