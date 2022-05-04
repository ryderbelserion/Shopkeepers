package com.nisovin.shopkeepers.ui.state;

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
 * It is up to a specific UI to decide which {@link UIState}s it accepts. Attempting to restore an
 * incompatible UI state will result in a {@link RuntimeException}.
 * <p>
 * Other possible uses of {@link UIState} are the ability to pass inputs to a UI or receive outputs
 * from it. For example, the {@link UIState} may implement {@link UIOutput} to receive results or
 * messages from the UI and transfer them back to the creator of the {@link UIState}.
 */
public interface UIState {

	/**
	 * An empty {@link UIState}.
	 */
	public static final UIState EMPTY = new UIState() {
	};
}
