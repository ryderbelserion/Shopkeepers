package com.nisovin.shopkeepers.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public final class Utils {

	private Utils() {
	}

	public static String getServerCBVersion() {
		String packageName = Bukkit.getServer().getClass().getPackage().getName();
		String cbVersion = packageName.substring(packageName.lastIndexOf('.') + 1);
		return cbVersion;
	}

	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPERS;
	static {
		Map<Class<?>, Class<?>> primitiveWrappers = new HashMap<>();
		primitiveWrappers.put(boolean.class, Boolean.class);
		primitiveWrappers.put(byte.class, Byte.class);
		primitiveWrappers.put(char.class, Character.class);
		primitiveWrappers.put(double.class, Double.class);
		primitiveWrappers.put(float.class, Float.class);
		primitiveWrappers.put(int.class, Integer.class);
		primitiveWrappers.put(long.class, Long.class);
		primitiveWrappers.put(short.class, Short.class);
		PRIMITIVE_WRAPPERS = Collections.unmodifiableMap(primitiveWrappers);
	}

	public static boolean isPrimitiveWrapperOf(Class<?> targetClass, Class<?> primitive) {
		Validate.isTrue(primitive.isPrimitive(), "Second argument has to be a primitive!");
		return (PRIMITIVE_WRAPPERS.get(primitive) == targetClass);
	}

	public static boolean isAssignableFrom(Class<?> to, Class<?> from) {
		if (to.isAssignableFrom(from)) {
			return true;
		}
		if (to.isPrimitive()) {
			return isPrimitiveWrapperOf(from, to);
		}
		if (from.isPrimitive()) {
			return isPrimitiveWrapperOf(to, from);
		}
		return false;
	}

	public static <T extends Enum<T>> T cycleEnumConstant(Class<T> enumClass, T current, boolean backwards) {
		return cycleEnumConstant(enumClass, current, backwards, null);
	}

	public static <T extends Enum<T>> T cycleEnumConstant(Class<T> enumClass, T current, boolean backwards, Predicate<T> predicate) {
		return cycleEnumConstant(enumClass, false, current, backwards, predicate);
	}

	public static <T extends Enum<T>> T cycleEnumConstantNullable(Class<T> enumClass, T current, boolean backwards) {
		return cycleEnumConstantNullable(enumClass, current, backwards, null);
	}

	public static <T extends Enum<T>> T cycleEnumConstantNullable(Class<T> enumClass, T current, boolean backwards, Predicate<T> predicate) {
		return cycleEnumConstant(enumClass, true, current, backwards, predicate);
	}

	// nullable: uses null as first value
	// current==null: nullable has to be true
	// cycled through all values but none got accepted: returns current value (can be null)
	private static <T extends Enum<T>> T cycleEnumConstant(Class<T> enumClass, boolean nullable, T current, boolean backwards, Predicate<T> predicate) {
		Validate.notNull(enumClass);
		Validate.isTrue(current != null || nullable, "Not nullable, but current is null!");
		T[] values = enumClass.getEnumConstants();
		int currentId = (current == null ? -1 : current.ordinal());
		int nextId = currentId;
		while (true) {
			if (backwards) {
				nextId -= 1;
				if (nextId < (nullable ? -1 : 0)) {
					nextId = (values.length - 1);
				}
			} else {
				nextId += 1;
				if (nextId >= values.length) {
					nextId = (nullable ? -1 : 0);
				}
			}
			if (nextId == currentId) {
				return current;
			}
			T next = (nextId == -1 ? null : values[nextId]);
			if (predicate == null || predicate.test(next)) {
				return next;
			}
		}
	}

	public static <E extends Enum<E>> E parseEnumValue(Class<E> enumClass, String name) {
		if (name == null) return null;
		try {
			return Enum.valueOf(enumClass, name);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * Calculates the average of the given values.
	 * <p>
	 * Note: This can overflow if the sum of the values doesn't fit into a single <code>long</code>.
	 * 
	 * @param values
	 *            the values
	 * @return the average
	 */
	public static double average(long[] values) {
		long total = 0L;
		for (long value : values) {
			total += value;
		}
		return ((double) total / values.length);
	}

	/**
	 * Gets the block's center location.
	 * 
	 * @param block
	 *            the block
	 * @return the block's center location
	 */
	public static Location getBlockCenterLocation(Block block) {
		Validate.notNull(block, "Block is null!");
		return block.getLocation().add(0.5D, 0.5D, 0.5D);
	}

	public enum BlockFaceDirections {
		// order matters for operations like yaw to block face
		CARDINAL(Arrays.asList(BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH, BlockFace.EAST)),
		INTERCARDINAL(Arrays.asList(BlockFace.SOUTH, BlockFace.SOUTH_WEST, BlockFace.WEST, BlockFace.NORTH_WEST, BlockFace.NORTH, BlockFace.NORTH_EAST, BlockFace.EAST, BlockFace.SOUTH_EAST)),
		SECONDARY_INTERCARDINAL(
				Arrays.asList(
						BlockFace.SOUTH, BlockFace.SOUTH_SOUTH_WEST, BlockFace.SOUTH_WEST, BlockFace.WEST_SOUTH_WEST,
						BlockFace.WEST, BlockFace.WEST_NORTH_WEST, BlockFace.NORTH_WEST, BlockFace.NORTH_NORTH_WEST,
						BlockFace.NORTH, BlockFace.NORTH_NORTH_EAST, BlockFace.NORTH_EAST, BlockFace.EAST_NORTH_EAST,
						BlockFace.EAST, BlockFace.EAST_SOUTH_EAST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_SOUTH_EAST));

		private final List<BlockFace> directions;

		private BlockFaceDirections(List<BlockFace> directions) {
			this.directions = Collections.unmodifiableList(directions);
		}

		public List<BlockFace> getDirections() {
			return directions;
		}
	}

	/**
	 * Gets the horizontal {@link BlockFace} corresponding to the given yaw angle.
	 * 
	 * @param yaw
	 *            the yaw angle
	 * @param blockFaceDirections
	 *            the block face directions
	 * @return the block face corresponding to the given yaw angle
	 */
	public static BlockFace yawToFace(float yaw, BlockFaceDirections blockFaceDirections) {
		Validate.notNull(blockFaceDirections, "BlockFaceDirections is null!");
		switch (blockFaceDirections) {
		case CARDINAL:
			return blockFaceDirections.getDirections().get(Math.round(yaw / 90.0f) & 3);
		case INTERCARDINAL:
			return blockFaceDirections.getDirections().get(Math.round(yaw / 45.0f) & 7);
		case SECONDARY_INTERCARDINAL:
			return blockFaceDirections.getDirections().get(Math.round(yaw / 22.5f) & 15);
		default:
			throw new IllegalArgumentException("Unsupported BlockFaceDirections: " + blockFaceDirections);
		}
	}

	private static final List<BlockFace> BLOCK_SIDES = Collections.unmodifiableList(Arrays.asList(BlockFace.UP, BlockFace.DOWN,
			BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST));

	public static List<BlockFace> getBlockSides() {
		return BLOCK_SIDES;
	}

	public static boolean isBlockSide(BlockFace blockFace) {
		return BLOCK_SIDES.contains(blockFace);
	}

	/**
	 * Checks if the given {@link BlockFace} is valid to be used for a wall sign.
	 * 
	 * @param blockFace
	 *            the block face
	 * @return <code>true</code> if the given block face is a valid wall sign face
	 */
	public static boolean isWallSignFace(BlockFace blockFace) {
		return blockFace == BlockFace.NORTH || blockFace == BlockFace.SOUTH
				|| blockFace == BlockFace.EAST || blockFace == BlockFace.WEST;
	}

	public static BlockFace getSignPostFacing(float yaw) {
		return yawToFace(yaw, BlockFaceDirections.SECONDARY_INTERCARDINAL);
	}

	public static boolean isSignPostFacing(BlockFace blockFace) {
		return BlockFaceDirections.SECONDARY_INTERCARDINAL.getDirections().contains(blockFace);
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

	// temporary objects getting re-used during ray tracing:
	private static final Location TEMP_START_LOCATION = new Location(null, 0, 0, 0);
	private static final Vector TEMP_START_POSITION = new Vector();
	private static final Vector DOWN_DIRECTION = new Vector(0.0D, -1.0D, 0.0D);
	private static final double RAY_TRACE_OFFSET = 0.01D;

	/**
	 * Get the distance to the nearest block collision in the range of the given <code>maxDistance</code>.
	 * <p>
	 * This performs a ray trace through the blocks' collision boxes, ignoring fluids and passable blocks.
	 * <p>
	 * The ray tracing gets slightly offset (by <code>0.01</code>) in order to make sure that we don't miss any block
	 * directly at the start location. If this results in a hit above the start location, we ignore it and return
	 * <code>0.0</code>.
	 * 
	 * @param startLocation
	 *            the start location, has to use a valid world, does not get modified
	 * @param maxDistance
	 *            the max distance to check for block collisions, has to be positive
	 * @return the distance to the ground, or <code>maxDistance</code> if there are no block collisions within the
	 *         specified range
	 */
	public static double getCollisionDistanceToGround(Location startLocation, double maxDistance) {
		World world = startLocation.getWorld();
		assert world != null;
		// setup re-used offset start location:
		TEMP_START_LOCATION.setWorld(world);
		TEMP_START_LOCATION.setX(startLocation.getX());
		TEMP_START_LOCATION.setY(startLocation.getY() + RAY_TRACE_OFFSET);
		TEMP_START_LOCATION.setZ(startLocation.getZ());

		// considers block collision boxes, ignoring fluids and passable blocks:
		RayTraceResult rayTraceResult = world.rayTraceBlocks(TEMP_START_LOCATION, DOWN_DIRECTION, maxDistance + RAY_TRACE_OFFSET, FluidCollisionMode.NEVER, true);
		TEMP_START_LOCATION.setWorld(null); // cleanup temporarily used start location

		double distanceToGround;
		if (rayTraceResult == null) {
			// no collision with the range:
			distanceToGround = maxDistance;
		} else {
			TEMP_START_POSITION.setX(TEMP_START_LOCATION.getX());
			TEMP_START_POSITION.setY(TEMP_START_LOCATION.getY());
			TEMP_START_POSITION.setZ(TEMP_START_LOCATION.getZ());
			distanceToGround = TEMP_START_POSITION.distance(rayTraceResult.getHitPosition()) - RAY_TRACE_OFFSET;
			// might be negative if the hit is between the start location and the offset start location, we ignore it
			// then:
			if (distanceToGround < 0.0D) distanceToGround = 0.0D;
		}
		return distanceToGround;
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

	// more performant variant if coordinates are integers:
	public static String getLocationString(String worldName, int x, int y, int z) {
		return worldName + "," + x + "," + y + "," + z;
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

	private static final char COLOR_CHAR_ALTERNATIVE = '&';
	private static final Pattern STRIP_COLOR_ALTERNATIVE_PATTERN = Pattern.compile("(?i)" + String.valueOf(COLOR_CHAR_ALTERNATIVE) + "[0-9A-FK-OR]");

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

	public static String stripColor(String colored) {
		if (colored == null) return null;
		String uncolored = ChatColor.stripColor(colored);
		uncolored = STRIP_COLOR_ALTERNATIVE_PATTERN.matcher(uncolored).replaceAll("");
		return uncolored;
	}

	public static String decolorize(String colored) {
		if (colored == null) return null;
		return Utils.translateColorCodesToAlternative(COLOR_CHAR_ALTERNATIVE, colored);
	}

	public static List<String> decolorize(List<String> colored) {
		if (colored == null) return null;
		List<String> decolored = new ArrayList<>(colored.size());
		for (String string : colored) {
			decolored.add(Utils.translateColorCodesToAlternative(COLOR_CHAR_ALTERNATIVE, string));
		}
		return decolored;
	}

	public static String colorize(String message) {
		if (message == null || message.isEmpty()) return message;
		return ChatColor.translateAlternateColorCodes(COLOR_CHAR_ALTERNATIVE, message);
	}

	public static List<String> colorize(List<String> messages) {
		if (messages == null) return messages;
		List<String> colored = new ArrayList<>(messages.size());
		for (String message : messages) {
			colored.add(Utils.colorize(message));
		}
		return colored;
	}

	public static String replaceArgs(String message, String... args) {
		if (!StringUtils.isEmpty(message) && args != null && args.length >= 2) {
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
		return message;
	}

	public static List<String> replaceArgs(Collection<String> messages, String... args) {
		List<String> replaced = new ArrayList<>(messages.size());
		for (String message : messages) {
			replaced.add(replaceArgs(message, args));
		}
		return replaced;
	}

	public static void sendMessage(CommandSender sender, String message, String... args) {
		// replace message arguments:
		message = replaceArgs(message, args);

		// skip if sender is null or message is empty:
		if (sender == null || StringUtils.isEmpty(message)) return;

		// send message:
		String[] msgs = message.split("\n");
		for (String msg : msgs) {
			sender.sendMessage(msg);
		}
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

	public static List<Entity> getNearbyEntities(Location location, double radius, EntityType... types) {
		List<Entity> entities = new ArrayList<>();
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
		List<Entity> entities = new ArrayList<>();
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
