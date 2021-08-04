package com.nisovin.shopkeepers.util.bukkit;

import static org.junit.Assert.assertEquals;

import org.bukkit.block.BlockFace;
import org.junit.Test;

import com.nisovin.shopkeepers.util.bukkit.BlockFaceUtils.BlockFaceDirections;

public class BlockFaceUtilsTests {

	private static final float EPSILON = 0.00001F;

	@Test
	public void testBlockFaceToYaw() {
		assertEquals(BlockFaceUtils.getYaw(BlockFace.SOUTH), 0.0F, EPSILON);
		assertEquals(BlockFaceUtils.getYaw(BlockFace.SOUTH_SOUTH_WEST), 22.5F, EPSILON);
		assertEquals(BlockFaceUtils.getYaw(BlockFace.SOUTH_WEST), 45.0F, EPSILON);
		assertEquals(BlockFaceUtils.getYaw(BlockFace.WEST_SOUTH_WEST), 67.5F, EPSILON);
		assertEquals(BlockFaceUtils.getYaw(BlockFace.WEST), 90.0F, EPSILON);
		assertEquals(BlockFaceUtils.getYaw(BlockFace.NORTH), 180.0F, EPSILON);
		assertEquals(BlockFaceUtils.getYaw(BlockFace.EAST), 270.0F, EPSILON);
	}

	@Test
	public void testYawToBlockFace() {
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(0.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(-0.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(40.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(-40.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(-400.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(760.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(90.0F), BlockFace.WEST);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(180.0F), BlockFace.NORTH);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(-180.0F), BlockFace.NORTH);
		assertEquals(BlockFaceDirections.CARDINAL.fromYaw(270.0F), BlockFace.EAST);

		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(0.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(-0.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(20.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(-20.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(-380.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(740.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(45.0F), BlockFace.SOUTH_WEST);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(90.0F), BlockFace.WEST);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(180.0F), BlockFace.NORTH);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(-180.0F), BlockFace.NORTH);
		assertEquals(BlockFaceDirections.INTERCARDINAL.fromYaw(270.0F), BlockFace.EAST);

		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(0.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(-0.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(10.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(-10.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(-370.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(730.0F), BlockFace.SOUTH);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(22.5F), BlockFace.SOUTH_SOUTH_WEST);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(45.0F), BlockFace.SOUTH_WEST);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(67.5F), BlockFace.WEST_SOUTH_WEST);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(90.0F), BlockFace.WEST);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(180.0F), BlockFace.NORTH);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(-180.0F), BlockFace.NORTH);
		assertEquals(BlockFaceDirections.SECONDARY_INTERCARDINAL.fromYaw(270.0F), BlockFace.EAST);
	}
}
