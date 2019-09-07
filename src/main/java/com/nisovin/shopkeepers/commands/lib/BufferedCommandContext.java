package com.nisovin.shopkeepers.commands.lib;

import java.util.Map.Entry;

import com.nisovin.shopkeepers.util.Validate;

/**
 * Wraps another {@link CommandContext} and stores any context changes in a separate buffer that can be manually cleared
 * or applied to the underlying command context.
 */
public final class BufferedCommandContext extends CommandContext {

	private final CommandContext context;
	// the parent's parsedArgs get used for the buffer

	/**
	 * Creates a new and empty {@link BufferedCommandContext}.
	 * 
	 * @param context
	 *            the wrapped context
	 */
	public BufferedCommandContext(CommandContext context) {
		super();
		Validate.notNull(context);
		this.context = context;
	}

	/**
	 * Clears the buffer.
	 */
	public void clearBuffer() {
		if (parsedArgs != null) {
			parsedArgs.clear();
		}
	}

	/**
	 * Applies the buffer to the underlying {@link CommandContext}.
	 */
	public void applyBuffer() {
		if (parsedArgs != null) {
			for (Entry<String, Object> entry : parsedArgs.entrySet()) {
				context.put(entry.getKey(), entry.getValue());
			}
			parsedArgs.clear();
		}
	}

	@Override
	public <T> T get(String key) {
		T value = super.get(key); // check buffer
		return (value != null) ? value : context.get(key); // else check wrapped context
	}

	@Override
	public boolean has(String key) {
		// check both buffer and wrapped context:
		return super.has(key) || context.has(key);
	}
}
