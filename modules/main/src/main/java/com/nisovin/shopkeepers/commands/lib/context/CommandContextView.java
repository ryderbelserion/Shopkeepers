package com.nisovin.shopkeepers.commands.lib.context;

import java.util.Map;
import java.util.function.Supplier;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

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
		Validate.notNull(context, "context is null");
		this.context = context;
	}

	@Override
	public void put(String key, Object value) {
		throw new UnsupportedOperationException(
				"This CommandContext does not allow modifications!"
		);
	}

	@Override
	public <T> @NonNull T get(String key) {
		return context.get(key);
	}

	@Override
	public <T> @Nullable T getOrNull(String key) {
		return context.getOrNull(key);
	}

	@Override
	public <T> @Nullable T getOrDefault(String key, @Nullable T defaultValue) {
		return context.getOrDefault(key, defaultValue);
	}

	@Override
	public <T> @Nullable T getOrDefault(String key, Supplier<@Nullable T> defaultValueSupplier) {
		return context.getOrDefault(key, defaultValueSupplier);
	}

	@Override
	public boolean has(String key) {
		return context.has(key);
	}

	@Override
	public Map<? extends String, @NonNull ?> getMapView() {
		return context.getMapView();
	}

	@Override
	public CommandContextView getView() {
		return this; // Already an unmodifiable view.
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
