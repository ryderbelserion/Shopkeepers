package com.nisovin.shopkeepers.util.bukkit;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.block.BlockFace;
import org.bukkit.util.Vector;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Utilities related to {@link BlockFace}.
 */
public final class BlockFaceUtils {

	/**
	 * Sets of directional {@link BlockFace BlockFaces}.
	 */
	public enum BlockFaceDirections {
		// Order matters for operations like yaw to BlockFace.
		BLOCK_SIDES(
				Arrays.asList(
						BlockFace.SOUTH,
						BlockFace.WEST,
						BlockFace.NORTH,
						BlockFace.EAST,
						BlockFace.UP,
						BlockFace.DOWN
				)),
		CARDINAL(
				Arrays.asList(
						BlockFace.SOUTH,
						BlockFace.WEST,
						BlockFace.NORTH,
						BlockFace.EAST
				)),
		INTERCARDINAL(
				Arrays.asList(
						BlockFace.SOUTH,
						BlockFace.SOUTH_WEST,
						BlockFace.WEST,
						BlockFace.NORTH_WEST,
						BlockFace.NORTH,
						BlockFace.NORTH_EAST,
						BlockFace.EAST,
						BlockFace.SOUTH_EAST
				)),
		SECONDARY_INTERCARDINAL(
				Arrays.asList(
						BlockFace.SOUTH,
						BlockFace.SOUTH_SOUTH_WEST,
						BlockFace.SOUTH_WEST,
						BlockFace.WEST_SOUTH_WEST,
						BlockFace.WEST,
						BlockFace.WEST_NORTH_WEST,
						BlockFace.NORTH_WEST,
						BlockFace.NORTH_NORTH_WEST,
						BlockFace.NORTH,
						BlockFace.NORTH_NORTH_EAST,
						BlockFace.NORTH_EAST,
						BlockFace.EAST_NORTH_EAST,
						BlockFace.EAST,
						BlockFace.EAST_SOUTH_EAST,
						BlockFace.SOUTH_EAST,
						BlockFace.SOUTH_SOUTH_EAST
				));

		private final List<? extends BlockFace> blockFaces;

		private BlockFaceDirections(List<? extends BlockFace> blockFaces) {
			this.blockFaces = Collections.unmodifiableList(blockFaces);
		}

		/**
		 * Gets the {@link BlockFace BlockFaces} contained in this set.
		 * 
		 * @return the block faces
		 */
		public final List<? extends BlockFace> getBlockFaces() {
			return blockFaces;
		}

		/**
		 * Checks if this set contains the given {@link BlockFace}.
		 * 
		 * @param blockFace
		 *            the block face to check for
		 * @return <code>true</code> if this set contains the specified block face
		 */
		public final boolean contains(BlockFace blockFace) {
			return blockFaces.contains(blockFace);
		}

		/**
		 * Gets the {@link BlockFace} within this set that corresponds most closely to the given yaw
		 * angle.
		 * 
		 * @param yaw
		 *            the yaw angle
		 * @return the corresponding block face
		 */
		public BlockFace fromYaw(float yaw) {
			if (this == BLOCK_SIDES) {
				// BLOCK_SIDES is CARDINAL with UP and DOWN, which are not relevant for yaw
				// conversions.
				return CARDINAL.fromYaw(yaw);
			}

			int blockFaceCount = blockFaces.size();
			float anglePerBlockFace = 360.0F / blockFaceCount;
			int blockFaceIndex = Math.round(yaw / anglePerBlockFace) % blockFaceCount;
			if (blockFaceIndex < 0) blockFaceIndex += blockFaceCount;
			return blockFaces.get(blockFaceIndex);
		}
	}

	/**
	 * Gets the six {@link BlockFace BlockFaces} that correspond to the sides of a block.
	 * 
	 * @return the block faces that correspond to the sides of a block
	 */
	public static List<? extends BlockFace> getBlockSides() {
		return BlockFaceDirections.BLOCK_SIDES.getBlockFaces();
	}

	/**
	 * Checks whether the given {@link BlockFace} is a {@link #getBlockSides() block side}.
	 * 
	 * @param blockFace
	 *            the block face
	 * @return <code>true</code> if the given block face is a block side
	 */
	public static boolean isBlockSide(BlockFace blockFace) {
		return BlockFaceDirections.BLOCK_SIDES.contains(blockFace);
	}

	/**
	 * Gets the yaw angle that corresponds to the given horizontal {@link BlockFace}.
	 * 
	 * @param blockFace
	 *            the block face
	 * @return the corresponding yaw
	 */
	public static float getYaw(BlockFace blockFace) {
		Validate.notNull(blockFace, "blockFace is null");
		List<? extends BlockFace> horizontalBlockFaces = BlockFaceDirections.SECONDARY_INTERCARDINAL
				.getBlockFaces();
		int blockFaceIndex = horizontalBlockFaces.indexOf(blockFace);
		Validate.isTrue(blockFaceIndex != -1, "blockFace is not horizontal: " + blockFace);

		float anglePerBlockFace = 360.0F / horizontalBlockFaces.size();
		return blockFaceIndex * anglePerBlockFace;
	}

	/**
	 * Gets the set of valid wall sign {@link BlockFace BlockFaces}.
	 * 
	 * @return the set of wall sign block faces
	 */
	public static BlockFaceDirections getWallSignFacings() {
		return BlockFaceDirections.CARDINAL;
	}

	/**
	 * Gets the wall sign {@link BlockFace} that most closely faces towards the given direction.
	 * 
	 * @param direction
	 *            the direction
	 * @return the corresponding wall sign face
	 */
	public static BlockFace toWallSignFacing(Vector direction) {
		Validate.notNull(direction, "direction is null");
		return getAxisAlignedBlockFace(direction.getX(), 0.0D, direction.getZ());
	}

	/**
	 * Checks if the given {@link BlockFace} is a valid wall sign block face.
	 * 
	 * @param blockFace
	 *            the block face
	 * @return <code>true</code> if the given block face is a valid wall sign block face
	 */
	public static boolean isWallSignFacing(BlockFace blockFace) {
		return getWallSignFacings().contains(blockFace);
	}

	/**
	 * Gets the set of valid sign post {@link BlockFace BlockFaces}.
	 * 
	 * @return the set of sign post block faces
	 */
	public static BlockFaceDirections getSignPostFacings() {
		return BlockFaceDirections.SECONDARY_INTERCARDINAL;
	}

	/**
	 * Checks if the given {@link BlockFace} is a valid sign post block face.
	 * 
	 * @param blockFace
	 *            the block face
	 * @return <code>true</code> if the given block face is a valid sign post block face
	 */
	public static boolean isSignPostFacing(BlockFace blockFace) {
		return getSignPostFacings().contains(blockFace);
	}

	/**
	 * Gets the axis-aligned {@link BlockFace} that most closely faces towards the given direction.
	 * <p>
	 * If {@code modY} is zero, only horizontally facing block faces are considered.
	 * <p>
	 * This method takes into account that the values for EAST/WEST and NORTH/SOUTH were switched in
	 * some past version of Bukkit. So it should also properly work with older Bukkit versions.
	 * 
	 * @param modX
	 *            the x direction
	 * @param modY
	 *            the y direction
	 * @param modZ
	 *            the z direction
	 * @return the block face that most closely faces towards the given direction
	 */
	public static BlockFace getAxisAlignedBlockFace(double modX, double modY, double modZ) {
		double xAbs = Math.abs(modX);
		double yAbs = Math.abs(modY);
		double zAbs = Math.abs(modZ);

		if (xAbs >= zAbs) {
			if (xAbs >= yAbs) {
				if (modX >= 0.0D) {
					// EAST/WEST and NORTH/SOUTH values were switched in some past Bukkit version.
					// With these additional checks it should work across different versions.
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

	private BlockFaceUtils() {
	}
}
