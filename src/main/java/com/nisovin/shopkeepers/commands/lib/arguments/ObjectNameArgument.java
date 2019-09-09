package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;

/**
 * Base class for arguments that accept a name when there is a limited set of known applicable names (eg. names
 * identifying a known set of objects).
 * <p>
 * By default this argument actually accepts any String. But unlike {@link StringArgument} this class uses
 * {@link #getKnownIds()} to provide completions for partial inputs.
 * <p>
 * If the option <code>matchKnownNames</code> is used and a known name matches the given input (according to the used
 * matching function), that name will be returned instead of the input.
 */
public abstract class ObjectNameArgument extends ObjectIdArgument<String> {

	public static final int DEFAULT_MINIMAL_COMPLETION_INPUT = 0;

	public ObjectNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ObjectNameArgument(String name, ArgumentFilter<String> filter) {
		this(name, filter, true);
	}

	public ObjectNameArgument(String name, ArgumentFilter<String> filter, boolean matchKnownNames) {
		this(name, false, filter, matchKnownNames, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ObjectNameArgument(String name, boolean joinRemainingArgs, ArgumentFilter<String> filter, boolean matchKnownNames, int minimalCompletionInput) {
		super(name, new StringArgument(name + ":string", joinRemainingArgs), filter, matchKnownNames, minimalCompletionInput);
	}

	@Override
	protected String toString(String id) {
		return id; // already a String
	}

	@Override
	protected String normalize(String idString) {
		return idString.toLowerCase(); // uses default Locale by default
	}
}
