package com.nisovin.shopkeepers.commands.lib.context;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A simple {@link CommandContext}.
 */
public class SimpleCommandContext implements CommandContext {

	protected final Map<String, Object> values = new LinkedHashMap<>();
	private final CommandContextView view = new CommandContextView(Unsafe.initialized(this));

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
	public <T> @NonNull T get(String key) {
		@Nullable T value = this.getOrNull(key);
		Validate.State.notNull(value, () -> "Missing value for key '" + key + "'.");
		return Unsafe.assertNonNull(value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> @Nullable T getOrNull(String key) {
		return (T) values.get(key);
	}

	@Override
	public <T> @Nullable T getOrDefault(String key, @Nullable T defaultValue) {
		@Nullable T value = this.getOrNull(key);
		return (value != null) ? value : defaultValue;
	}

	@Override
	public <T> @Nullable T getOrDefault(String key, Supplier<@Nullable T> defaultValueSupplier) {
		@Nullable T value = this.getOrNull(key);
		if (value != null) return value;
		Validate.notNull(defaultValueSupplier, "defaultValueSupplier is null");
		assert defaultValueSupplier != null;
		return defaultValueSupplier.get();
	}

	@Override
	public boolean has(String key) {
		return values.containsKey(key);
	}

	@Override
	public Map<? extends String, @NonNull ?> getMapView() {
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
