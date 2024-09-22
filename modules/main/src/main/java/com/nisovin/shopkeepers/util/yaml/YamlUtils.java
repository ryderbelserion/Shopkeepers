package com.nisovin.shopkeepers.util.yaml;

import org.bukkit.configuration.file.YamlConstructor;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.representer.Representer;

import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public final class YamlUtils {

	// Compact (single line) Yaml formatting:
	private static final ThreadLocal<Yaml> YAML_COMPACT = ThreadLocal.withInitial(() -> {
		DumperOptions yamlDumperOptions = new DumperOptions();
		yamlDumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
		yamlDumperOptions.setDefaultScalarStyle(ScalarStyle.PLAIN);
		yamlDumperOptions.setSplitLines(false);
		yamlDumperOptions.setWidth(Integer.MAX_VALUE);
		Representer yamlRepresenter = new CompactYamlRepresenter();
		yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
		yamlRepresenter.setDefaultScalarStyle(ScalarStyle.PLAIN);
		LoaderOptions yamlLoaderOptions = new LoaderOptions();
		// Similar settings as in Bukkit:
		yamlLoaderOptions.setMaxAliasesForCollections(Integer.MAX_VALUE);
		yamlLoaderOptions.setCodePointLimit(Integer.MAX_VALUE);
		// Increase the default nesting limit (50), because this limit can easily be reached for
		// nested bundles, which can be nested up to 16 levels deep, each adding 3 levels of nesting
		// inside the serialized Yaml. See Spigot-7906.
		yamlLoaderOptions.setNestingDepthLimit(100);
		BaseConstructor yamlConstructor = new YamlConstructor(yamlLoaderOptions);
		return new Yaml(yamlConstructor, yamlRepresenter, yamlDumperOptions);
	});

	private static final String YAML_NEWLINE = "\n"; // YAML uses Unix line breaks by default

	public static String toCompactYaml(@Nullable Object object) {
		String yamlString = toYaml(YAML_COMPACT.get(), object);
		// SnakeYaml always appends a newline at the end:
		yamlString = StringUtils.stripTrailingNewlines(yamlString);
		return yamlString;
	}

	// Returns an empty String if the object is null.
	private static String toYaml(Yaml yaml, @Nullable Object object) {
		assert yaml != null;
		if (object == null) return "";
		String yamlString = yaml.dump(object);
		assert yamlString != null;
		return yamlString;
	}

	@SuppressWarnings("unchecked")
	public static <T> @Nullable T fromYaml(String yamlString) {
		Validate.notNull(yamlString, "yamlString is null");
		Yaml yaml = YAML_COMPACT.get();
		Object object = yaml.load(yamlString); // Can be null (e.g. for an empty String)
		return (T) object;
	}

	public static String yamlNewline() {
		return YAML_NEWLINE;
	}

	private YamlUtils() {
	}
}
