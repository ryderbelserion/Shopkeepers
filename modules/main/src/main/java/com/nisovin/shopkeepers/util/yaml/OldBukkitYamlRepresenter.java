package com.nisovin.shopkeepers.util.yaml;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.file.YamlRepresenter;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Represent;

/**
 * Extends Bukkit's {@link YamlRepresenter}, but ensures that {@link ConfigurationSection}s can still be represented.
 * <p>
 * There are plans in Spigot to change how {@link YamlConfiguration} serializes {@link ConfigurationSection}s in order
 * to properly support comments inside of configurations. These changes move the representation of configuration
 * sections from Bukkit's {@link YamlRepresenter} into {@link YamlConfiguration}. However, since we reuse Bukkit's
 * {@link YamlRepresenter} for our own Yaml serialization purposes, for which we don't need the comment support provided
 * by {@link YamlConfiguration}, we need the Yaml representer to still be able to represent configuration sections in
 * the future. This class therefore extends Bukkit's {@link YamlRepresenter} and ensures that the previous
 * {@link Represent} for {@link ConfigurationSection}s is still registered.
 */
class OldBukkitYamlRepresenter extends YamlRepresenter {

	OldBukkitYamlRepresenter() {
		super();
		this.multiRepresenters.put(ConfigurationSection.class, new RepresentConfigurationSection());
	}

	private class RepresentConfigurationSection extends RepresentMap {
		@Override
		public Node representData(Object data) {
			return super.representData(((ConfigurationSection) data).getValues(false));
		}
	}
}
