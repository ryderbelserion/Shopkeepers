package com.nisovin.shopkeepers.input;

import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * A request for input from a specific player.
 * <p>
 * The {@link #onInput(Object)} method is invoked when the requested input has been provided.
 * <p>
 * If the request requires a reference to the player or any other context, it has to capture this
 * itself within its implementation.
 *
 * @param <T>
 *            the result type of the requested input
 */
@FunctionalInterface
public interface InputRequest<@NonNull T> {

	/**
	 * This is invoked with the player's input when the request is fulfilled.
	 * <p>
	 * Requests are always fulfilled on the server's main thread, even if the underlying player
	 * input has been provided on a different thread (for example, player chat inputs are received
	 * on a separate thread).
	 * 
	 * @param input
	 *            the input result, not <code>null</code>
	 */
	public void onInput(T input);

	/**
	 * This is invoked when the request is aborted.
	 * <p>
	 * Possible reasons for why a request is aborted are for example:
	 * <ul>
	 * <li>The request has been replaced by another request.
	 * <li>The player has quit the server.
	 * <li>The plugin is shutting down.
	 * <li>The request is manually aborted for some other reason.
	 * </ul>
	 */
	public default void onAborted() {
	}
}
