package com.nisovin.shopkeepers.ui.state;

/**
 * A receiver of UI output messages.
 * <p>
 * UI output can for example be some result that is sent once the UI is closed, or messages that
 * regularly inform about user interactions within the UI.
 *
 * @param <T>
 *            the message type
 */
public interface UIOutput<T> {

	/**
	 * Receives an output message.
	 * 
	 * @param message
	 *            the message
	 */
	public void receive(T message);
}
