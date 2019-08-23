package com.nisovin.shopkeepers.util;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

public class BlockFaceUtils {

	private BlockFaceUtils() {
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
}
