package com.nisovin.shopkeepers.util.bukkit;

import static org.junit.Assert.assertEquals;

import org.bukkit.block.BlockFace;
import org.junit.Test;

import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils.BlockFaceDirections;

public class BlockFaceUtilsTests {

	private static final float EPSILON = 0.00001F;

	@Test
	public void testBlockFaceToYaw() {
		assertEquals(0.0F, BlockFaceUtils.getYaw(BlockFace.SOUTH), EPSILON);
		assertEquals(22.5F, BlockFaceUtils.getYaw(BlockFace.SOUTH_SOUTH_WEST), EPSILON);
		assertEquals(45.0F, BlockFaceUtils.getYaw(BlockFace.SOUTH_WEST), EPSILON);
		assertEquals(67.5F, BlockFaceUtils.getYaw(BlockFace.WEST_SOUTH_WEST), EPSILON);
		assertEquals(90.0F, BlockFaceUtils.getYaw(BlockFace.WEST), EPSILON);
		assertEquals(180.0F, BlockFaceUtils.getYaw(BlockFace.NORTH), EPSILON);
		assertEquals(270.0F, BlockFaceUtils.getYaw(BlockFace.EAST), EPSILON);
	}

	@Test
	public void testYawToBlockFace() {
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.CARDINAL.fromYaw(0.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.CARDINAL.fromYaw(-0.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.CARDINAL.fromYaw(40.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.CARDINAL.fromYaw(-40.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.CARDINAL.fromYaw(-400.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.CARDINAL.fromYaw(760.0F));
		assertEquals(BlockFace.WEST, BlockFaceDirections.CARDINAL.fromYaw(90.0F));
		assertEquals(BlockFace.NORTH, BlockFaceDirections.CARDINAL.fromYaw(180.0F));
		assertEquals(BlockFace.NORTH, BlockFaceDirections.CARDINAL.fromYaw(-180.0F));
		assertEquals(BlockFace.EAST, BlockFaceDirections.CARDINAL.fromYaw(270.0F));

		assertEquals(BlockFace.SOUTH, BlockFaceDirections.INTERCARDINAL.fromYaw(0.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.INTERCARDINAL.fromYaw(-0.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.INTERCARDINAL.fromYaw(20.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.INTERCARDINAL.fromYaw(-20.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.INTERCARDINAL.fromYaw(-380.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.INTERCARDINAL.fromYaw(740.0F));
		assertEquals(BlockFace.SOUTH_WEST, BlockFaceDirections.INTERCARDINAL.fromYaw(45.0F));
		assertEquals(BlockFace.WEST, BlockFaceDirections.INTERCARDINAL.fromYaw(90.0F));
		assertEquals(BlockFace.NORTH, BlockFaceDirections.INTERCARDINAL.fromYaw(180.0F));
		assertEquals(BlockFace.NORTH, BlockFaceDirections.INTERCARDINAL.fromYaw(-180.0F));
		assertEquals(BlockFace.EAST, BlockFaceDirections.INTERCARDINAL.fromYaw(270.0F));

		assertEquals(BlockFace.SOUTH, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(0.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(-0.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(10.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(-10.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(-370.0F));
		assertEquals(BlockFace.SOUTH, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(730.0F));
		assertEquals(
				BlockFace.SOUTH_SOUTH_WEST,
				BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(22.5F)
		);
		assertEquals(
				BlockFace.SOUTH_WEST,
				BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(45.0F)
		);
		assertEquals(
				BlockFace.WEST_SOUTH_WEST,
				BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(67.5F)
		);
		assertEquals(BlockFace.WEST, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(90.0F));
		assertEquals(BlockFace.NORTH, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(180.0F));
		assertEquals(BlockFace.NORTH, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(-180.0F));
		assertEquals(BlockFace.EAST, BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(270.0F));
	}
}
