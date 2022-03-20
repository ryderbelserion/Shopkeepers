package com.nisovin.shopkeepers.util.logging;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * A {@link Handler} that keeps track of the last observed {@link LogRecord}, i.e. with a level
 * equal or above the {@link #getLevel() level of this handler}.
 */
public class LogDetectionHandler extends Handler {

	private @Nullable LogRecord lastLogRecord = null;

	/**
	 * Creates a new {@link LogDetectionHandler}.
	 */
	public LogDetectionHandler() {
	}

	/**
	 * Checks whether a {@link #getLastLogRecord() LogRecord} has been detected.
	 * 
	 * @return <code>true</code> if a {@link LogRecord} has been detected
	 */
	public boolean hasLogRecord() {
		return (lastLogRecord != null);
	}

	/**
	 * Gets the last observed {@link LogRecord}.
	 * 
	 * @return the last observed {@link LogRecord}, or <code>null</code>
	 */
	public @Nullable LogRecord getLastLogRecord() {
		return lastLogRecord;
	}

	/**
	 * Releases any currently {@link #getLastLogRecord() tracked LogRecord}.
	 */
	public void reset() {
		lastLogRecord = null;
	}

	// Note: Log levels are already checked before this is called.
	@Override
	public void publish(@Nullable LogRecord record) {
		assert record != null;
		lastLogRecord = record;
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
		this.reset();
	}
}
