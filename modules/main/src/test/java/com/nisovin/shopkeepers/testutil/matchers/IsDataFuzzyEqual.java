package com.nisovin.shopkeepers.testutil.matchers;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.nisovin.shopkeepers.util.data.matcher.DataMatcher;

/**
 * A {@link Matcher} that uses {@link DataMatcher#FUZZY_NUMBERS} to test if a given data object matches an expected data
 * object.
 */
public class IsDataFuzzyEqual extends BaseMatcher<Object> {

	public static Matcher<Object> dataFuzzyEqualTo(Object expectedData) {
		return new IsDataFuzzyEqual(expectedData);
	}

	private final Object expectedObject; // Can be null

	private IsDataFuzzyEqual(Object expectedObject) {
		this.expectedObject = expectedObject;
	}

	@Override
	public boolean matches(Object item) {
		return DataMatcher.FUZZY_NUMBERS.matches(expectedObject, item);
	}

	@Override
	public void describeTo(Description description) {
		description.appendValue(expectedObject);
	}
}
