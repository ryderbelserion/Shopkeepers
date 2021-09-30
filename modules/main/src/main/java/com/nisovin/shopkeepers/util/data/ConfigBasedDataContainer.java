package com.nisovin.shopkeepers.util.data;

import java.util.Map;
import java.util.Set;

import org.bukkit.configuration.ConfigurationSection;

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
	public Object getOrDefault(String key, Object defaultValue) {
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
	public Set<String> getKeys() {
		// TODO This is wasteful.
		return config.getKeys(false);
	}

	@Override
	public Map<String, Object> getValues() {
		// TODO This is wasteful.
		return config.getValues(false);
	}

	@Override
	public Map<String, Object> getValuesCopy() {
		return config.getValues(false);
	}

	@Override
	public Object serialize() {
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
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof DataContainer)) return false;
		DataContainer otherContainer = (DataContainer) obj;
		return this.getValues().equals(otherContainer.getValues());
	}
}
