package com.nisovin.shopkeepers.util.yaml;

import org.bukkit.configuration.file.YamlConstructor;
import org.bukkit.configuration.file.YamlRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.BaseConstructor;
import org.yaml.snakeyaml.representer.Representer;

import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

public class YamlUtils {

	// Mimics Bukkit's Yaml configuration:
	private static final ThreadLocal<Yaml> YAML = ThreadLocal.withInitial(() -> {
		DumperOptions yamlOptions = new DumperOptions();
		yamlOptions.setIndent(2);
		yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		Representer yamlRepresenter = new YamlRepresenter();
		yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
		BaseConstructor yamlConstructor = new YamlConstructor();
		Yaml yaml = new Yaml(yamlConstructor, yamlRepresenter, yamlOptions);
		return yaml;
	});

	// Compact (single line) Yaml formatting:
	private static final ThreadLocal<Yaml> YAML_COMPACT = ThreadLocal.withInitial(() -> {
		DumperOptions yamlOptions = new DumperOptions();
		yamlOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
		yamlOptions.setDefaultScalarStyle(ScalarStyle.PLAIN);
		yamlOptions.setSplitLines(false);
		yamlOptions.setWidth(Integer.MAX_VALUE);
		Representer yamlRepresenter = new CompactYamlRepresenter();
		yamlRepresenter.setDefaultFlowStyle(DumperOptions.FlowStyle.FLOW);
		yamlRepresenter.setDefaultScalarStyle(ScalarStyle.PLAIN);
		BaseConstructor yamlConstructor = new YamlConstructor();
		Yaml yaml = new Yaml(yamlConstructor, yamlRepresenter, yamlOptions);
		return yaml;
	});

	private static final String YAML_NEWLINE = "\n"; // YAML uses Unix line breaks by default

	public static String toYaml(Object object) {
		return toYaml(YAML.get(), object);
	}

	public static String toCompactYaml(Object object) {
		String yamlString = toYaml(YAML_COMPACT.get(), object);
		// SnakeYaml always appends a newline at the end:
		yamlString = StringUtils.stripTrailingNewlines(yamlString);
		return yamlString;
	}

	// Returns an empty String if the object is null.
	private static String toYaml(Yaml yaml, Object object) {
		assert yaml != null;
		if (object == null) return "";
		String yamlString = yaml.dump(object);
		return yamlString;
	}

	@SuppressWarnings("unchecked")
	public static <T> T fromYaml(String yamlString) {
		Validate.notNull(yamlString, "yamlString is null");
		Yaml yaml = YAML.get();
		Object object = yaml.load(yamlString); // Can be null (e.g. for an empty String)
		return (T) object;
	}

	public static String yamlNewline() {
		return YAML_NEWLINE;
	}

	private YamlUtils() {
	}
}
