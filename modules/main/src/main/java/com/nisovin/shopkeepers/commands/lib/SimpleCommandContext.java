package com.nisovin.shopkeepers.commands.lib;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A simple {@link CommandContext}.
 */
public class SimpleCommandContext implements CommandContext {

	protected final Map<String, Object> values = new LinkedHashMap<>();
	private final CommandContextView view = new CommandContextView(this);

	/**
	 * Creates a new and empty {@link SimpleCommandContext}.
	 */
	public SimpleCommandContext() {
	}

	@Override
	public void put(String key, Object value) {
		Validate.notEmpty(key, "key is null or empty");
		Validate.notNull(value, "value is null");
		values.put(key, value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T get(String key) {
		return (T) values.get(key);
	}

	@Override
	public <T> T getOrDefault(String key, T defaultValue) {
		T value = this.get(key);
		return (value != null) ? value : defaultValue;
	}

	@Override
	public <T> T getOrDefault(String key, Supplier<T> defaultValueSupplier) {
		T value = this.get(key);
		if (value != null) return value;
		if (defaultValueSupplier == null) return null;
		return defaultValueSupplier.get();
	}

	@Override
	public boolean has(String key) {
		return values.containsKey(key);
	}

	@Override
	public Map<String, Object> getMapView() {
		return Collections.unmodifiableMap(values);
	}

	@Override
	public CommandContextView getView() {
		return view;
	}

	@Override
	public CommandContext copy() {
		SimpleCommandContext copy = new SimpleCommandContext();
		copy.values.putAll(this.getMapView());
		return copy;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SimpleCommandContext [values=");
		builder.append(values);
		builder.append("]");
		return builder.toString();
	}
}
