package com.nisovin.shopkeepers.commands.lib;

import java.util.Map;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.Validate;

/**
 * An unmodifiable view on another {@link CommandContext}.
 */
public class CommandContextView implements CommandContext {

	private final CommandContext context;

	/**
	 * Creates a new {@link CommandContextView} that provides a read-only view on the given
	 * {@link CommandContext}.
	 * 
	 * @param context
	 *            the underlying command context
	 */
	public CommandContextView(CommandContext context) {
		Validate.notNull(context, "Context is null!");
		this.context = context;
	}

	@Override
	public void put(String key, Object value) {
		throw new UnsupportedOperationException("This CommandContext does not allow modifications!");
	}

	@Override
	public <T> T get(String key) {
		return context.get(key);
	}

	@Override
	public <T> T getOrDefault(String key, T defaultValue) {
		return context.getOrDefault(key, defaultValue);
	}

	@Override
	public <T> T getOrDefault(String key, Supplier<T> defaultValueSupplier) {
		return context.getOrDefault(key, defaultValueSupplier);
	}

	@Override
	public boolean has(String key) {
		return context.has(key);
	}

	@Override
	public Map<String, Object> getMapView() {
		return context.getMapView();
	}

	@Override
	public CommandContextView getView() {
		return this; // already an unmodifiable view
	}

	@Override
	public CommandContext copy() {
		return context.copy();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("CommandContextView [context=");
		builder.append(context);
		builder.append("]");
		return builder.toString();
	}
}
