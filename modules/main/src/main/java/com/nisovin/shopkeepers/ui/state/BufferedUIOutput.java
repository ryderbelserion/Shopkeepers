package com.nisovin.shopkeepers.ui.state;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link UIOutput} implementation that buffers the last received message.
 *
 * @param <T>
 *            the message type
 */
public class BufferedUIOutput<T> implements UIOutput<T> {

	private @Nullable T lastMessage = null;

	public BufferedUIOutput() {
	}

	@Override
	public void receive(T message) {
		this.lastMessage = message;
	}

	/**
	 * Gets the last received output message.
	 * 
	 * @return the last received output message, or <code>null</code> if none was received yet
	 */
	public @Nullable T getLastMessage() {
		return lastMessage;
	}
}
