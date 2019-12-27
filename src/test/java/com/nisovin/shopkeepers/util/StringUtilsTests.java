package com.nisovin.shopkeepers.util;

import org.junit.Assert;
import org.junit.Test;

public class StringUtilsTests {

	@Test
	public void testSplitLines() {
		String input = "This is\n a \r\nmulti-line\\ntext\\n! With\n\\nempty lines and trailing\n";
		String[] result = StringUtils.splitLines(input);
		String[] expected = new String[] {
				"This is", " a ", "multi-line", "text", "! With", "", "empty lines and trailing", ""
		};
		Assert.assertArrayEquals(expected, result);
	}

	@Test
	public void testReplaceFirst() {
		String input = "Text with {key} and {key}!";
		String result = StringUtils.replaceFirst(input, "{key}", "replacement");
		String expected = "Text with replacement and {key}!";
		Assert.assertEquals(expected, result);
	}
}
