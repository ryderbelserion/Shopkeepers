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
import com.nisovin.shopkeepers.util.Utils;

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
		this.addArgument(new OptionalArgument(new FirstOfArgument("context", Arrays.asList(
				new LiteralArgument(ARGUMENT_CHUNKS),
				new LiteralArgument(ARGUMENT_ACTIVE)))));
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
		sender.sendMessage("  Total chunks with shopkeepers: " + shopsByChunk.size());
		sender.sendMessage("  Active: " + shopkeeperRegistry.getActiveShopkeepers().size());
		sender.sendMessage("  Active with AI: " + livingEntityAI.getEntityCount());
		sender.sendMessage("  Active AI chunks: " + livingEntityAI.getActiveAIChunksCount());
		sender.sendMessage("  Active with active AI: " + livingEntityAI.getActiveAIEntityCount());
		sender.sendMessage("  Active gravity chunks: " + livingEntityAI.getActiveGravityChunksCount());
		sender.sendMessage("  Active with active gravity: " + livingEntityAI.getActiveGravityEntityCount());

		double avgTotalAITimings = livingEntityAI.getTotalTimings().getAverageTimeMillis();
		double maxTotalAITiming = livingEntityAI.getTotalTimings().getMaxTimeMillis();
		sender.sendMessage("  Avg. total AI timings: " + Utils.DECIMAL_FORMAT.format(avgTotalAITimings) + " ms");
		sender.sendMessage("  Max. total AI timing: " + Utils.DECIMAL_FORMAT.format(maxTotalAITiming) + " ms");

		double avgAIActivationTimings = livingEntityAI.getActivationTimings().getAverageTimeMillis();
		double maxAIActivationTiming = livingEntityAI.getActivationTimings().getMaxTimeMillis();
		sender.sendMessage("    Avg. AI activation timings: " + Utils.DECIMAL_FORMAT.format(avgAIActivationTimings) + " ms");
		sender.sendMessage("    Max. AI activation timing: " + Utils.DECIMAL_FORMAT.format(maxAIActivationTiming) + " ms");

		double avgGravityTimings = livingEntityAI.getGravityTimings().getAverageTimeMillis();
		double maxGravityTiming = livingEntityAI.getGravityTimings().getMaxTimeMillis();
		sender.sendMessage("    Avg. gravity timings: " + Utils.DECIMAL_FORMAT.format(avgGravityTimings) + " ms");
		sender.sendMessage("    Max. gravity timing: " + Utils.DECIMAL_FORMAT.format(maxGravityTiming) + " ms");

		double avgAITimings = livingEntityAI.getAITimings().getAverageTimeMillis();
		double maxAITiming = livingEntityAI.getAITimings().getMaxTimeMillis();
		sender.sendMessage("    Avg. AI timings: " + Utils.DECIMAL_FORMAT.format(avgAITimings) + " ms");
		sender.sendMessage("    Max. AI timing: " + Utils.DECIMAL_FORMAT.format(maxAITiming) + " ms");

		for (World world : Bukkit.getWorlds()) {
			String worldName = world.getName();
			Chunk[] loadedChunks = world.getLoadedChunks();
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
			sender.sendMessage("  Loaded chunks: " + loadedChunks.length);
			if (totalShopkeepers > 0) {
				sender.sendMessage("  Chunks with shopkeepers: " + chunksWithShopkeepers);
				sender.sendMessage("  Loaded chunks with shopkeepers: " + loadedChunksWithShopkeepers);
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
