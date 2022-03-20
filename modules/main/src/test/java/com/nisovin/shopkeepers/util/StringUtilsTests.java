package com.nisovin.shopkeepers.util;

import org.junit.Assert;
import org.junit.Test;

import com.nisovin.shopkeepers.util.java.StringUtils;

public class StringUtilsTests {

	@Test
	public void testSplitLines() {
		String newlines = "Unix\nWindows\r\nMac\rForm Feed\fVertical Tab\u000BNext line"
				+ "\u0085Line Separator\u2028Paragraph Separator\u2029 trailing \n\n";
		String[] newlinesSplit = StringUtils.splitLines(newlines, false);
		// This should produce the same result, since there are no literal newlines:
		String[] newlinesAndLiteralSplit = StringUtils.splitLines(newlines, true);
		String[] expectedNewlinesSplit = new String[] {
				"Unix", "Windows", "Mac", "Form Feed", "Vertical Tab", "Next line",
				"Line Separator", "Paragraph Separator", " trailing ", "", ""
		};
		Assert.assertArrayEquals(expectedNewlinesSplit, newlinesSplit);
		Assert.assertArrayEquals(expectedNewlinesSplit, newlinesAndLiteralSplit);
	}

	@Test
	public void testSplitLiteralNewlines() {
		String literal = "Literal\\n trailing \\n\\n";
		String[] literalSplit = StringUtils.splitLines(literal, true);
		String[] expectedLiteralSplit = new String[] {
				"Literal", " trailing ", "", ""
		};
		Assert.assertArrayEquals(expectedLiteralSplit, literalSplit);

		String[] nonLiteralSplit = StringUtils.splitLines(literal, false);
		String[] expectedNonLiteralSplit = new String[] {
				"Literal\\n trailing \\n\\n"
		};
		Assert.assertArrayEquals(expectedNonLiteralSplit, nonLiteralSplit);
	}

	@Test
	public void testStripTrailingNewlines() {
		String string1 = "Multi-line\nString\r\nwith trailing\nnewlines\n";
		String string2 = "Multi-line\nString\r\nwith trailing\nnewlines\r\n";
		String string3 = "Multi-line\nString\r\nwith trailing\nnewlines\n\n\n";
		String string4 = "Multi-line\nString\r\nwith trailing\nnewlines\n\r\r\n";
		String expected = "Multi-line\nString\r\nwith trailing\nnewlines";
		String stripped1 = StringUtils.stripTrailingNewlines(string1);
		String stripped2 = StringUtils.stripTrailingNewlines(string2);
		String stripped3 = StringUtils.stripTrailingNewlines(string3);
		String stripped4 = StringUtils.stripTrailingNewlines(string4);
		Assert.assertEquals(expected, stripped1);
		Assert.assertEquals(expected, stripped2);
		Assert.assertEquals(expected, stripped3);
		Assert.assertEquals(expected, stripped4);
	}

	@Test
	public void testReplaceFirst() {
		String input = "Text with {key} and {key}!";
		String result = StringUtils.replaceFirst(input, "{key}", "replacement");
		String expected = "Text with replacement and {key}!";
		Assert.assertEquals(expected, result);
	}
}
