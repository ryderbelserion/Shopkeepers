package com.nisovin.shopkeepers.ui;

import com.nisovin.shopkeepers.api.ui.UIType;

/**
 * A captured UI state that provides hints on how to restore a previous UI session.
 * <p>
 * A UI state is meant to only store state that is specific to a particular UI session. Any UI state
 * that depends on external data (e.g. the trades and items stored by a shopkeeper) is not captured
 * but needs to be freshly retrieved at the time the UI state is restored. Consequently, if any
 * external data has changed, a captured UI state may not be able to perfectly restore the previous
 * UI session. A captured UI state is restored in a best-effort manner, but should not be strictly
 * relied upon.
 * <p>
 * UI states are often specific to a particular {@link UIType}. Attempting to restore an
 * incompatible UI state will result in a {@link RuntimeException}.
 */
public interface UIState {

	/**
	 * An empty {@link UIState}.
	 * <p>
	 * This state is always accepted by all UI types.
	 */
	public static final UIState EMPTY = new UIState() {
	};
}
