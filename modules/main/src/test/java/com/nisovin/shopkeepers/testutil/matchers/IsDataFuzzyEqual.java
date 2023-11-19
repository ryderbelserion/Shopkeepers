package com.nisovin.shopkeepers.testutil.matchers;

import org.checkerframework.checker.nullness.qual.Nullable;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.util.data.matcher.DataMatcher;

/**
 * A {@link Matcher} that uses {@link DataMatcher#FUZZY_NUMBERS} to test if a given data object
 * matches an expected data object.
 */
public class IsDataFuzzyEqual extends BaseMatcher<@Nullable Object> {

	public static Matcher<@Nullable Object> dataFuzzyEqualTo(@Nullable Object expectedData) {
		return new IsDataFuzzyEqual(expectedData);
	}

	private final @Nullable Object expectedObject;

	private IsDataFuzzyEqual(@Nullable Object expectedObject) {
		this.expectedObject = expectedObject;
	}

	@Override
	public boolean matches(@Nullable Object item) {
		return DataMatcher.FUZZY_NUMBERS.matches(expectedObject, item);
	}

	@Override
	public void describeTo(@Nullable Description description) {
		assert description != null;
		description.appendValue(Unsafe.nullableAsNonNull(expectedObject));
	}
}
