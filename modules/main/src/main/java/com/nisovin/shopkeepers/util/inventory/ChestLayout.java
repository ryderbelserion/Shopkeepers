package com.nisovin.shopkeepers.util.inventory;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Chest inventory layout helpers.
 */
public final class ChestLayout {

	public static final int SLOTS_PER_ROW = 9;
	public static final int MAX_ROWS = 6;
	public static final int MAX_SLOTS = SLOTS_PER_ROW * MAX_ROWS;

	// Returns at least one row of slots, and at maximum MAX_SLOTS.
	public static int getRequiredSlots(int requestedSlots) {
		Validate.isTrue(requestedSlots >= 0, "requestedSlots must not be negative");
		int requiredRows = (requestedSlots / SLOTS_PER_ROW);
		if (requiredRows == 0 || requestedSlots % SLOTS_PER_ROW != 0) {
			requiredRows += 1;
		}
		return Math.min(requiredRows * SLOTS_PER_ROW, MAX_SLOTS);
	}

	private ChestLayout() {
	}
}
