package com.nisovin.shopkeepers.util.java;

import org.junit.Assert;
import org.junit.Test;

public class MathUtilsTests {

	@Test
	public void testFuzzyEquals() {
		Assert.assertTrue(MathUtils.fuzzyEquals(0.0D, 0.0D));
		Assert.assertTrue(MathUtils.fuzzyEquals(1.0D, 1.0D));
		Assert.assertTrue(MathUtils.fuzzyEquals(0.0D, 0.000001D));
		Assert.assertTrue(MathUtils.fuzzyEquals(0.0D, 0.00001D));
		Assert.assertFalse(MathUtils.fuzzyEquals(0.0D, 0.00002D));

		// Special values:
		Assert.assertTrue(MathUtils.fuzzyEquals(-0.0D, 0.0D)); // Unlike Double#equals(Object)
		Assert.assertTrue(
				MathUtils.fuzzyEquals(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)
		);
		Assert.assertTrue(
				MathUtils.fuzzyEquals(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)
		);
		Assert.assertFalse(
				MathUtils.fuzzyEquals(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
		);
		// Similar to Double#equals(Object):
		Assert.assertTrue(MathUtils.fuzzyEquals(Double.NaN, Double.NaN));
		double nan = 0.0D / 0.0D;
		Assert.assertTrue(MathUtils.fuzzyEquals(Double.NaN, nan));
	}
}
