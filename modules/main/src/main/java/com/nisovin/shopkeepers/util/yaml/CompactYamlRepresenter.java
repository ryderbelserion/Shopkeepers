package com.nisovin.shopkeepers.util.yaml;

import org.bukkit.configuration.file.YamlRepresenter;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.ScalarStyle;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * There are situations in which SnakeYaml uses the literal block style by default and thereby produces output with line
 * breaks. This includes for example:
 * <ul>
 * <li>Strings that contain non-printable characters and are therefore considered to be binary data.
 * <li>Strings and characters that contain newline characters when the used scalar style is {@link ScalarStyle#PLAIN}.
 * <li>Byte arrays, which are also considered binary data.
 * <li>Custom representers may also decide to forcefully use block styles.
 * </ul>
 * <p>
 * For our compact Yaml representation we want to produce Yaml output without any line breaks in all situations. This
 * {@link Representer} therefore enforces the use of the scalar style {@link ScalarStyle#DOUBLE_QUOTED} for any scalars
 * that contain newline characters.
 */
public class CompactYamlRepresenter extends YamlRepresenter { // Extends Bukkit's YamlRepresenter

	public CompactYamlRepresenter() {
	}

	// We expect that all Representers use this method to create scalar nodes. The alternative would be to replace all
	// registered Representers with wrappers that analyze the created nodes and replace them if necessary.
	@Override
	protected Node representScalar(Tag tag, String value, DumperOptions.ScalarStyle style) {
		assert value != null;
		if (style == null) {
			style = this.defaultScalarStyle;
		}
		assert style != null;

		// Only the double-quoted style is guaranteed to not output line breaks, but escape them instead:
		if (style != ScalarStyle.DOUBLE_QUOTED && StringUtils.containsNewline(value)) {
			// The value contains newline characters but is not double-quoted. Enforce the use of the double-quoted
			// style:
			style = ScalarStyle.DOUBLE_QUOTED;
		} // Else: Stick to the default representation:
		return super.representScalar(tag, value, style);
	}
}
