package com.nisovin.shopkeepers.util.data.container;

import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.bukkit.ConfigUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link DataContainer} that wraps a {@link ConfigurationSection}.
 */
public class ConfigBasedDataContainer extends AbstractDataContainer {

	private final ConfigurationSection config;

	/**
	 * Creates a new {@link ConfigBasedDataContainer}.
	 * 
	 * @param config
	 *            the configuration section that stores the data, not <code>null</code>
	 */
	public ConfigBasedDataContainer(ConfigurationSection config) {
		Validate.notNull(config, "config is null");
		this.config = config;
	}

	/**
	 * Gets the underlying {@link ConfigurationSection} that stores the data of this data container.
	 * 
	 * @return the config section, not <code>null</code>
	 */
	public ConfigurationSection getConfig() {
		return config;
	}

	@Override
	public @Nullable Object getOrDefault(String key, @Nullable Object defaultValue) {
		Validate.notEmpty(key, "key is empty");
		Object value = config.get(key, null);
		return (value != null) ? value : defaultValue;
	}

	@Override
	protected void internalSet(String key, Object value) {
		config.set(key, value);
	}

	@Override
	public void remove(String key) {
		config.set(key, null);
	}

	@Override
	public void clear() {
		// TODO This is wasteful.
		ConfigUtils.clearConfigSection(config);
	}

	@Override
	public int size() {
		// TODO This is wasteful.
		return this.getKeys().size();
	}

	@Override
	public Set<? extends String> getKeys() {
		// TODO This is wasteful.
		return Unsafe.cast(config.getKeys(false));
	}

	@Override
	public Map<? extends String, @NonNull ?> getValues() {
		// TODO This is wasteful.
		return this.getValuesCopy();
	}

	@Override
	public Map<String, Object> getValuesCopy() {
		return ConfigUtils.getValues(config);
	}

	@Override
	public @Nullable Object serialize() {
		return config;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ConfigBasedDataContainer [config=");
		builder.append(this.getValues());
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return this.getValues().hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof DataContainer)) return false;
		DataContainer otherContainer = (DataContainer) obj;
		return this.getValues().equals(otherContainer.getValues());
	}
}
