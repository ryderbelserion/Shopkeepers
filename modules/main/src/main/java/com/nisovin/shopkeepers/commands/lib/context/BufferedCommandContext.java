package com.nisovin.shopkeepers.commands.lib.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Wraps another {@link CommandContext} and stores any context changes in a separate buffer that can
 * be manually cleared or applied to the underlying parent command context.
 */
public final class BufferedCommandContext extends SimpleCommandContext {

	private final CommandContext context;
	// The super context's values are used for the buffer.

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
		values.clear();
	}

	/**
	 * Applies the buffer to the parent {@link CommandContext} and then clears it.
	 */
	public void applyBuffer() {
		values.forEach((key, value) -> {
			context.put(key, value);
		});
		this.clearBuffer();
	}

	@Override
	public <T> @Nullable T getOrNull(String key) {
		@Nullable T value = super.getOrNull(key); // Check buffer
		return (value != null) ? value : context.getOrNull(key); // Else check parent context
	}

	@Override
	public boolean has(String key) {
		// Check both buffer and parent context:
		return super.has(key) || context.has(key);
	}

	// Note: This is relatively costly for the buffered command context, compared to the regular
	// command context!
	@Override
	public Map<? extends @NonNull String, @NonNull ?> getMapView() {
		if (values.isEmpty()) {
			return context.getMapView();
		} else {
			// Combine maps:
			Map<@NonNull String, @NonNull Object> combined = new LinkedHashMap<>(context.getMapView());
			combined.putAll(super.values); // Replaces existing entries for duplicate keys
			return Collections.unmodifiableMap(combined);
		}
	}

	@Override
	public CommandContextView getView() {
		return super.getView();
	}

	@Override
	public CommandContext copy() {
		// Note: #copy() creates a copy of the combined command context contents by using
		// #getMapView().
		return super.copy();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("BufferedCommandContext [context=");
		builder.append(context);
		builder.append(", buffer=");
		builder.append(values);
		builder.append("]");
		return builder.toString();
	}
}
