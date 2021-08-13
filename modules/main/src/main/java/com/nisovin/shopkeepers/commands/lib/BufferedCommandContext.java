package com.nisovin.shopkeepers.commands.lib;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Wraps another {@link CommandContext} and stores any context changes in a separate buffer that can be manually cleared
 * or applied to the underlying parent command context.
 */
public final class BufferedCommandContext extends SimpleCommandContext {

	private final CommandContext context;
	// The super context's values get used for the buffer.

	/**
	 * Creates a new and empty {@link BufferedCommandContext}.
	 * 
	 * @param context
	 *            the parent command context
	 */
	public BufferedCommandContext(CommandContext context) {
		super();
		Validate.notNull(context, "context is null");
		this.context = context;
	}

	/**
	 * Gets the parent {@link CommandContext}.
	 * 
	 * @return the parent command context
	 */
	public CommandContext getParentContext() {
		return context;
	}

	/**
	 * Clears the buffer.
	 */
	public void clearBuffer() {
		if (super.values != null) {
			super.values.clear();
		}
	}

	/**
	 * Applies the buffer to the parent {@link CommandContext} and then clears it.
	 */
	public void applyBuffer() {
		if (super.values != null) {
			for (Entry<String, Object> entry : super.values.entrySet()) {
				context.put(entry.getKey(), entry.getValue());
			}
			this.clearBuffer();
		}
	}

	@Override
	public <T> T get(String key) {
		T value = super.get(key); // Check buffer
		return (value != null) ? value : context.get(key); // Else check parent context
	}

	@Override
	public boolean has(String key) {
		// Check both buffer and parent context:
		return super.has(key) || context.has(key);
	}

	// Note: This is relatively costly for the buffered command context, compared to the regular command context!
	@Override
	public Map<String, Object> getMapView() {
		if (super.values == null || super.values.isEmpty()) {
			return context.getMapView();
		} else {
			// combine maps:
			Map<String, Object> combinedMap = new LinkedHashMap<>(context.getMapView());
			combinedMap.putAll(super.values); // Replaces existing entries for duplicate keys
			return Collections.unmodifiableMap(combinedMap);
		}
	}

	@Override
	public CommandContextView getView() {
		return super.getView();
	}

	@Override
	public CommandContext copy() {
		// Note: #copy() creates a copy of the combined command context contents by using #getMapView().
		return super.copy();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BufferedCommandContext [context=");
		builder.append(context);
		builder.append(", buffer=");
		builder.append(super.values);
		builder.append("]");
		return builder.toString();
	}
}
