package com.nisovin.shopkeepers.util.logging;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A {@link Logger} that logs nothing.
 */
public final class NullLogger extends Logger {

	private static final Logger INSTANCE = new NullLogger();

	public static Logger getInstance() {
		return INSTANCE;
	}

	private NullLogger() {
		super(NullLogger.class.getCanonicalName(), null);
		super.setLevel(Level.OFF);
	}

	@Override
	public void setLevel(Level level) {
		throw new UnsupportedOperationException("This logger cannot be modified!");
	}

	@Override
	public void log(LogRecord logRecord) {
		assert logRecord != null;
		// Does not log anything.
	}
}
