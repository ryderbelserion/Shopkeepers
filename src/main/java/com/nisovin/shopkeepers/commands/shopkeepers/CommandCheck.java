package com.nisovin.shopkeepers.commands.shopkeepers;

import java.util.Arrays;
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
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.api.util.ChunkCoords;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandException;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.LiteralArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.OptionalArgument;
import com.nisovin.shopkeepers.shopobjects.living.LivingEntityAI;
import com.nisovin.shopkeepers.util.TextUtils;

class CommandCheck extends Command {

	private static final String ARGUMENT_CHUNKS = "chunks";
	private static final String ARGUMENT_ACTIVE = "active";

	private final SKShopkeepersPlugin plugin;
	private final ShopkeeperRegistry shopkeeperRegistry;

	CommandCheck(SKShopkeepersPlugin plugin) {
		super("check");
		this.plugin = plugin;
		this.shopkeeperRegistry = plugin.getShopkeeperRegistry();

		// set permission:
		this.setPermission(ShopkeepersPlugin.DEBUG_PERMISSION);

		// set description:
		this.setDescription("Shows various debugging information.");

		// hidden debugging command:
		this.setHiddenInParentHelp(true);

		// arguments:
		this.addArgument(new OptionalArgument<>(new FirstOfArgument("context", Arrays.asList(
				new LiteralArgument(ARGUMENT_CHUNKS),
				new LiteralArgument(ARGUMENT_ACTIVE)),
				true))); // join formats
	}

	@Override
	protected void execute(CommandInput input, CommandContext context, CommandArgs args) throws CommandException {
		CommandSender sender = input.getSender();
		boolean isConsole = (sender instanceof ConsoleCommandSender);

		boolean listChunks = context.has(ARGUMENT_CHUNKS);
		boolean listActive = context.has(ARGUMENT_ACTIVE);

		Map<ChunkCoords, ? extends List<?>> shopsByChunk = shopkeeperRegistry.getAllShopkeepersByChunks();
		LivingEntityAI livingEntityAI = plugin.getLivingShops().getLivingEntityAI();

		sender.sendMessage(ChatColor.YELLOW + "All shopkeepers:");
		sender.sendMessage("  Total: " + shopkeeperRegistry.getAllShopkeepers().size());
		sender.sendMessage("  Unsaved dirty | deleted | dirty storage: "
				+ plugin.getShopkeeperStorage().getDirtyCount()
				+ " | " + plugin.getShopkeeperStorage().getUnsavedDeletedCount()
				+ " | " + plugin.getShopkeeperStorage().isDirty());
		sender.sendMessage("  Chunks with shopkeepers: " + shopsByChunk.size());
		sender.sendMessage("    With active AI: " + livingEntityAI.getActiveAIChunksCount());
		sender.sendMessage("    With active gravity: " + livingEntityAI.getActiveGravityChunksCount());
		sender.sendMessage("  Active shopkeepers: " + shopkeeperRegistry.getActiveShopkeepers().size());
		sender.sendMessage("    With AI: " + livingEntityAI.getEntityCount());
		sender.sendMessage("    With active AI: " + livingEntityAI.getActiveAIEntityCount());
		sender.sendMessage("    With active gravity: " + livingEntityAI.getActiveGravityEntityCount());

		double avgTotalAITimings = livingEntityAI.getTotalTimings().getAverageTimeMillis();
		double maxTotalAITiming = livingEntityAI.getTotalTimings().getMaxTimeMillis();
		sender.sendMessage("  Total AI timings (avg | max): "
				+ TextUtils.DECIMAL_FORMAT.format(avgTotalAITimings) + " ms" + " | "
				+ TextUtils.DECIMAL_FORMAT.format(maxTotalAITiming) + " ms");

		// note: these are per activation, which happens only every 20 ticks (not per tick)
		double avgAIActivationTimings = livingEntityAI.getActivationTimings().getAverageTimeMillis();
		double maxAIActivationTiming = livingEntityAI.getActivationTimings().getMaxTimeMillis();
		sender.sendMessage("    AI activation timings (per " + LivingEntityAI.AI_ACTIVATION_TICK_RATE + " ticks) (avg | max): "
				+ TextUtils.DECIMAL_FORMAT.format(avgAIActivationTimings) + " ms" + " | "
				+ TextUtils.DECIMAL_FORMAT.format(maxAIActivationTiming) + " ms");

		double avgGravityTimings = livingEntityAI.getGravityTimings().getAverageTimeMillis();
		double maxGravityTiming = livingEntityAI.getGravityTimings().getMaxTimeMillis();
		sender.sendMessage("    Gravity timings (avg | max): "
				+ TextUtils.DECIMAL_FORMAT.format(avgGravityTimings) + " ms" + " | "
				+ TextUtils.DECIMAL_FORMAT.format(maxGravityTiming) + " ms");

		double avgAITimings = livingEntityAI.getAITimings().getAverageTimeMillis();
		double maxAITiming = livingEntityAI.getAITimings().getMaxTimeMillis();
		sender.sendMessage("    AI timings (avg | max): "
				+ TextUtils.DECIMAL_FORMAT.format(avgAITimings) + " ms" + " | "
				+ TextUtils.DECIMAL_FORMAT.format(maxAITiming) + " ms");

		for (World world : Bukkit.getWorlds()) {
			String worldName = world.getName();
			Chunk[] loadedChunks = world.getLoadedChunks();
			List<Entity> entities = world.getEntities();
			int deadEntities = 0;
			int invalidEntities = 0;
			for (Entity entity : entities) {
				if (entity.isDead()) ++deadEntities;
				if (!entity.isValid()) ++invalidEntities;
			}
			int deadEntitiesInChunks = 0;
			int invalidEntitiesInChunks = 0;
			for (Chunk chunk : loadedChunks) {
				for (Entity entity : chunk.getEntities()) {
					if (entity.isDead()) ++deadEntitiesInChunks;
					if (!entity.isValid()) ++invalidEntitiesInChunks;
				}
			}

			int totalShopkeepers = 0;
			int chunksWithShopkeepers = 0;
			int loadedChunksWithShopkeepers = 0;
			int shopkeepersInLoadedChunks = 0;

			for (Entry<ChunkCoords, ? extends List<?>> chunkEntry : shopsByChunk.entrySet()) {
				ChunkCoords chunkCoords = chunkEntry.getKey();
				if (!chunkCoords.getWorldName().equals(worldName)) continue;
				List<?> inChunk = chunkEntry.getValue();
				chunksWithShopkeepers++;
				totalShopkeepers += inChunk.size();
				if (chunkCoords.isChunkLoaded()) {
					loadedChunksWithShopkeepers++;
					shopkeepersInLoadedChunks += inChunk.size();
				}
			}

			sender.sendMessage(ChatColor.YELLOW + "Shopkeepers in world '" + world.getName() + "':");
			sender.sendMessage("  Total: " + totalShopkeepers);
			sender.sendMessage("  Entities | invalid | dead: " + entities.size() + " | " + invalidEntities + " | " + deadEntities);
			sender.sendMessage("  Entities in chunks (invalid | dead): " + invalidEntitiesInChunks + " | " + deadEntitiesInChunks);
			sender.sendMessage("  Loaded chunks: " + loadedChunks.length);
			if (totalShopkeepers > 0) {
				sender.sendMessage("  Chunks with shopkeepers | loaded: " + chunksWithShopkeepers + " | " + loadedChunksWithShopkeepers);
				sender.sendMessage("  Shopkeepers in loaded chunks: " + shopkeepersInLoadedChunks);
			}

			// list all chunks containing shopkeepers:
			if (isConsole && listChunks && totalShopkeepers > 0) {
				sender.sendMessage("  Listing of all chunks with shopkeepers:");
				int line = 0;
				for (Entry<ChunkCoords, ? extends List<?>> chunkEntry : shopsByChunk.entrySet()) {
					ChunkCoords chunkCoords = chunkEntry.getKey();
					if (!chunkCoords.getWorldName().equals(worldName)) continue;
					List<?> inChunk = chunkEntry.getValue();
					line++;
					ChatColor lineColor = (line % 2 == 0 ? ChatColor.WHITE : ChatColor.GRAY);
					sender.sendMessage("    (" + lineColor + chunkCoords.getChunkX() + "," + chunkCoords.getChunkZ() + ChatColor.RESET + ") ["
							+ (chunkCoords.isChunkLoaded() ? ChatColor.GREEN + "loaded" : ChatColor.DARK_GRAY + "unloaded") + ChatColor.RESET
							+ "]: " + inChunk.size());
				}
			}
		}

		// list all active shopkeepers:
		if (isConsole && listActive) {
			sender.sendMessage("All active shopkeepers:");
			for (Shopkeeper shopkeeper : shopkeeperRegistry.getActiveShopkeepers()) {
				if (shopkeeper.isActive()) {
					Location loc = shopkeeper.getObjectLocation();
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": active (" + (loc != null ? loc.toString() : "maybe not?!?") + ")");
				} else {
					sender.sendMessage("Shopkeeper at " + shopkeeper.getPositionString() + ": INACTIVE!");
				}
			}
		}

		if (!isConsole && (listChunks || listActive)) {
			sender.sendMessage("There might be more information getting printed if the command is run from the console.");
		}
	}
}
