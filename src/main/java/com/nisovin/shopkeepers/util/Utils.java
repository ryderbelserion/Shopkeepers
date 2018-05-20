package com.nisovin.shopkeepers.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.util.Vector;

public class Utils {

	/**
	 * Checks if the given {@link BlockFace} is valid to be used for a wall sign.
	 * 
	 * @param blockFace
	 * @return
	 */
	public static boolean isWallSignFace(BlockFace blockFace) {
		return blockFace == BlockFace.NORTH || blockFace == BlockFace.SOUTH || blockFace == BlockFace.EAST || blockFace == BlockFace.WEST;
	}

	/**
	 * Determines the axis-aligned {@link BlockFace} for the given direction.
	 * If modY is zero only {@link BlockFace}s facing horizontal will be returned.
	 * This method takes into account that the values for EAST/WEST and NORTH/SOUTH
	 * were switched in some past version of bukkit. So it should also properly work
	 * with older bukkit versions.
	 * 
	 * @param modX
	 * @param modY
	 * @param modZ
	 * @return
	 */
	public static BlockFace getAxisBlockFace(double modX, double modY, double modZ) {
		double xAbs = Math.abs(modX);
		double yAbs = Math.abs(modY);
		double zAbs = Math.abs(modZ);

		if (xAbs >= zAbs) {
			if (xAbs >= yAbs) {
				if (modX >= 0.0D) {
					// EAST/WEST and NORTH/SOUTH values were switched in some past bukkit version:
					// with this additional checks it should work across different versions
					if (BlockFace.EAST.getModX() == 1) {
						return BlockFace.EAST;
					} else {
						return BlockFace.WEST;
					}
				} else {
					if (BlockFace.EAST.getModX() == 1) {
						return BlockFace.WEST;
					} else {
						return BlockFace.EAST;
					}
				}
			} else {
				if (modY >= 0.0D) {
					return BlockFace.UP;
				} else {
					return BlockFace.DOWN;
				}
			}
		} else {
			if (zAbs >= yAbs) {
				if (modZ >= 0.0D) {
					if (BlockFace.SOUTH.getModZ() == 1) {
						return BlockFace.SOUTH;
					} else {
						return BlockFace.NORTH;
					}
				} else {
					if (BlockFace.SOUTH.getModZ() == 1) {
						return BlockFace.NORTH;
					} else {
						return BlockFace.SOUTH;
					}
				}
			} else {
				if (modY >= 0.0D) {
					return BlockFace.UP;
				} else {
					return BlockFace.DOWN;
				}
			}
		}
	}

	/**
	 * Tries to find the nearest wall sign {@link BlockFace} facing towards the given direction.
	 * 
	 * @param direction
	 * @return a valid wall sign face
	 */
	public static BlockFace toWallSignFace(Vector direction) {
		assert direction != null;
		return getAxisBlockFace(direction.getX(), 0.0D, direction.getZ());
	}

	/**
	 * Gets the block face a player is looking at.
	 * 
	 * @param player
	 *            the player
	 * @param targetBlock
	 *            the block the player is looking at
	 * @return the block face, or <code<null</code> if none was found
	 */
	public static BlockFace getTargetBlockFace(Player player, Block targetBlock) {
		Location intersection = getBlockIntersection(player, targetBlock);
		if (intersection == null) return null;
		Location blockCenter = targetBlock.getLocation().add(0.5D, 0.5D, 0.5D);
		Vector centerToIntersection = intersection.subtract(blockCenter).toVector();
		double x = centerToIntersection.getX();
		double y = centerToIntersection.getY();
		double z = centerToIntersection.getZ();
		return getAxisBlockFace(x, y, z);
	}

	/**
	 * Determines the exact intersection point of a players view and a targeted block.
	 * 
	 * @param player
	 *            the player
	 * @param targetBlock
	 *            the block the player is looking at
	 * @return the intersection point of the players view and the target block,
	 *         or null if no intersection was found
	 */
	public static Location getBlockIntersection(Player player, Block targetBlock) {
		if (player == null || targetBlock == null) return null;

		// block bounds:
		double minX = targetBlock.getX();
		double minY = targetBlock.getY();
		double minZ = targetBlock.getZ();

		double maxX = minX + 1.0D;
		double maxY = minY + 1.0D;
		double maxZ = minZ + 1.0D;

		// ray origin:
		Location origin = player.getEyeLocation();
		double originX = origin.getX();
		double originY = origin.getY();
		double originZ = origin.getZ();

		// ray direction
		Vector dir = origin.getDirection();
		double dirX = dir.getX();
		double dirY = dir.getY();
		double dirZ = dir.getZ();

		// tiny improvement to save a few divisions below:
		double divX = 1.0D / dirX;
		double divY = 1.0D / dirY;
		double divZ = 1.0D / dirZ;

		// intersection interval:
		double t0 = 0.0D;
		double t1 = Double.MAX_VALUE;

		double tmin;
		double tmax;

		double tymin;
		double tymax;

		double tzmin;
		double tzmax;

		if (dirX >= 0.0D) {
			tmin = (minX - originX) * divX;
			tmax = (maxX - originX) * divX;
		} else {
			tmin = (maxX - originX) * divX;
			tmax = (minX - originX) * divX;
		}

		if (dirY >= 0.0D) {
			tymin = (minY - originY) * divY;
			tymax = (maxY - originY) * divY;
		} else {
			tymin = (maxY - originY) * divY;
			tymax = (minY - originY) * divY;
		}

		if ((tmin > tymax) || (tymin > tmax)) {
			return null;
		}

		if (tymin > tmin) tmin = tymin;
		if (tymax < tmax) tmax = tymax;

		if (dirZ >= 0.0D) {
			tzmin = (minZ - originZ) * divZ;
			tzmax = (maxZ - originZ) * divZ;
		} else {
			tzmin = (maxZ - originZ) * divZ;
			tzmax = (minZ - originZ) * divZ;
		}

		if ((tmin > tzmax) || (tzmin > tmax)) {
			return null;
		}

		if (tzmin > tmin) tmin = tzmin;
		if (tzmax < tmax) tmax = tzmax;

		if ((tmin >= t1) || (tmax <= t0)) {
			return null;
		}

		// intersection:
		Location intersection = origin.add(dir.multiply(tmin));
		return intersection;
	}

	// messages:

	public static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.##", new DecimalFormatSymbols(Locale.US));
	static {
		DECIMAL_FORMAT.setGroupingUsed(false);
	}

	public static String getLocationString(Location location) {
		return getLocationString(location.getWorld().getName(), location.getX(), location.getY(), location.getZ());
	}

	public static String getLocationString(Block block) {
		return getLocationString(block.getWorld().getName(), block.getX(), block.getY(), block.getZ());
	}

	public static String getLocationString(String worldName, double x, double y, double z) {
		return worldName + "," + DECIMAL_FORMAT.format(x) + "," + DECIMAL_FORMAT.format(y) + "," + DECIMAL_FORMAT.format(z);
	}

	public static String getPlayerAsString(Player player) {
		return getPlayerAsString(player.getName(), player.getUniqueId());
	}

	public static String getPlayerAsString(String playerName, UUID uniqueId) {
		return playerName + (uniqueId == null ? "" : "(" + uniqueId.toString() + ")");
	}

	public static String translateColorCodesToAlternative(char altColorChar, String textToTranslate) {
		char[] b = textToTranslate.toCharArray();
		for (int i = 0; i < b.length - 1; i++) {
			if (b[i] == ChatColor.COLOR_CHAR && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
				b[i] = altColorChar;
				b[i + 1] = Character.toLowerCase(b[i + 1]);
			}
		}
		return new String(b);
	}

	public static String decolorize(String colored) {
		if (colored == null) return null;
		return Utils.translateColorCodesToAlternative('&', colored);
	}

	public static List<String> decolorize(List<String> colored) {
		if (colored == null) return null;
		List<String> decolored = new ArrayList<String>(colored.size());
		for (String string : colored) {
			decolored.add(Utils.translateColorCodesToAlternative('&', string));
		}
		return decolored;
	}

	public static String colorize(String message) {
		if (message == null || message.isEmpty()) return message;
		return ChatColor.translateAlternateColorCodes('&', message);
	}

	public static List<String> colorize(List<String> messages) {
		if (messages == null) return messages;
		List<String> colored = new ArrayList<String>(messages.size());
		for (String message : messages) {
			colored.add(Utils.colorize(message));
		}
		return colored;
	}

	public static void sendMessage(CommandSender sender, String message, String... args) {
		// skip if sender is null or message is "empty":
		if (sender == null || message == null || message.isEmpty()) return;
		if (args != null && args.length >= 2) {
			// replace arguments (key-value replacement):
			String key;
			String value;
			for (int i = 1; i < args.length; i += 2) {
				key = args[i - 1];
				value = args[i];
				if (key == null || value == null) continue; // skip invalid arguments
				message = message.replace(key, value);
			}
		}

		String[] msgs = message.split("\n");
		for (String msg : msgs) {
			sender.sendMessage(msg);
		}
	}

	public static boolean isEmpty(String string) {
		return string == null || string.isEmpty();
	}

	public static String normalize(String identifier) {
		if (identifier == null) return null;
		return identifier.trim().replace('_', '-').replace(' ', '-').toLowerCase(Locale.ROOT);
	}

	public static List<String> normalize(List<String> identifiers) {
		if (identifiers == null) return null;
		List<String> normalized = new ArrayList<String>(identifiers.size());
		for (String identifier : identifiers) {
			normalized.add(normalize(identifier));
		}
		return normalized;
	}

	/**
	 * Performs a permissions check and logs debug information about it.
	 * 
	 * @param permissible
	 * @param permission
	 * @return
	 */
	public static boolean hasPermission(Permissible permissible, String permission) {
		assert permissible != null;
		boolean hasPerm = permissible.hasPermission(permission);
		if (!hasPerm && (permissible instanceof Player)) {
			Log.debug("Player '" + ((Player) permissible).getName() + "' does not have permission '" + permission + "'.");
		}
		return hasPerm;
	}

	// entity utilities:

	public static boolean isNPC(Entity entity) {
		return entity.hasMetadata("NPC");
	}

	public static List<Entity> getNearbyEntities(Location location, double radius, EntityType... types) {
		List<Entity> entities = new ArrayList<Entity>();
		if (location == null) return entities;
		if (radius <= 0.0D) return entities;

		List<EntityType> typesList = (types == null) ? Collections.<EntityType>emptyList() : Arrays.asList(types);
		double radius2 = radius * radius;
		int chunkRadius = ((int) (radius / 16)) + 1;
		Chunk center = location.getChunk();
		int startX = center.getX() - chunkRadius;
		int endX = center.getX() + chunkRadius;
		int startZ = center.getZ() - chunkRadius;
		int endZ = center.getZ() + chunkRadius;
		World world = location.getWorld();
		for (int chunkX = startX; chunkX <= endX; chunkX++) {
			for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
				if (!world.isChunkLoaded(chunkX, chunkZ)) continue;
				Chunk chunk = world.getChunkAt(chunkX, chunkZ);
				for (Entity entity : chunk.getEntities()) {
					Location entityLoc = entity.getLocation();
					// TODO: this is a workaround: for some yet unknown reason entities sometimes report to be in a
					// different world..
					if (!entityLoc.getWorld().equals(world)) {
						Log.debug("Found an entity which reports to be in a different world than the chunk we got it from:");
						Log.debug("Location=" + location + ", Chunk=" + chunk + ", ChunkWorld=" + chunk.getWorld()
								+ ", entityType=" + entity.getType() + ", entityLocation=" + entityLoc);
						continue; // skip this entity
					}

					if (entityLoc.distanceSquared(location) <= radius2) {
						if (typesList.isEmpty() || typesList.contains(entity.getType())) {
							entities.add(entity);
						}
					}
				}
			}
		}
		return entities;
	}

	public static List<Entity> getNearbyChunkEntities(Chunk chunk, int chunkRadius, boolean loadChunks, EntityType... types) {
		List<Entity> entities = new ArrayList<Entity>();
		if (chunk == null) return entities;
		if (chunkRadius < 0) return entities;

		List<EntityType> typesList = (types == null) ? Collections.<EntityType>emptyList() : Arrays.asList(types);
		int startX = chunk.getX() - chunkRadius;
		int endX = chunk.getX() + chunkRadius;
		int startZ = chunk.getZ() - chunkRadius;
		int endZ = chunk.getZ() + chunkRadius;
		World world = chunk.getWorld();
		for (int chunkX = startX; chunkX <= endX; chunkX++) {
			for (int chunkZ = startZ; chunkZ <= endZ; chunkZ++) {
				if (!loadChunks && !world.isChunkLoaded(chunkX, chunkZ)) continue;
				Chunk currentChunk = world.getChunkAt(chunkX, chunkZ);
				for (Entity entity : currentChunk.getEntities()) {
					Location entityLoc = entity.getLocation();
					// TODO: this is a workaround: for some yet unknown reason entities sometimes report to be in a
					// different world..
					if (!entityLoc.getWorld().equals(world)) {
						Log.debug("Found an entity which reports to be in a different world than the chunk we got it from:");
						Log.debug("Chunk=" + currentChunk + ", ChunkWorld=" + currentChunk.getWorld() + ", entityType=" + entity.getType()
								+ ", entityLocation=" + entityLoc);
						continue; // skip this entity
					}

					if (typesList.isEmpty() || typesList.contains(entity.getType())) {
						entities.add(entity);
					}
				}
			}
		}
		return entities;
	}

	// value conversion utilities:

	public static Integer parseInt(String intString) {
		try {
			return Integer.parseInt(intString);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	// collections utilities:

	// shortcut map initializers:

	// maximum capacity of a HashMap (largest power of two fitting into an int)
	private static final int MAX_CAPACITY = (1 << 30);

	// capacity for a HashMap with the specified expected size and a loading-factor of >= 0.75,
	// that prevents the map from resizing
	private static int capacity(int expectedSize) {
		assert expectedSize >= 0;
		if (expectedSize < 3) {
			return expectedSize + 1;
		}
		if (expectedSize < MAX_CAPACITY) {
			return (int) ((float) expectedSize / 0.75F + 1.0F);
		}
		return Integer.MAX_VALUE;
	}

	public static <K, V> Map<K, V> createMap(K key, V value) {
		Map<K, V> map = new LinkedHashMap<>(capacity(1));
		map.put(key, value);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2) {
		Map<K, V> map = new LinkedHashMap<>(capacity(2));
		map.put(key1, value1);
		map.put(key2, value2);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2, K key3, V value3) {
		Map<K, V> map = new LinkedHashMap<>(capacity(3));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4) {
		Map<K, V> map = new LinkedHashMap<>(capacity(4));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		map.put(key4, value4);
		return map;
	}

	public static <K, V> Map<K, V> createMap(K key1, V value1, K key2, V value2, K key3, V value3, K key4, V value4, K key5, V value5) {
		Map<K, V> map = new LinkedHashMap<>(capacity(5));
		map.put(key1, value1);
		map.put(key2, value2);
		map.put(key3, value3);
		map.put(key4, value4);
		map.put(key5, value5);
		return map;
	}
}
