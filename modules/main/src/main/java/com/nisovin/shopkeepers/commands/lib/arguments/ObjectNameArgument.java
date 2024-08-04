package com.nisovin.shopkeepers.commands.lib.arguments;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;

/**
 * Base class for arguments that accept a name as identifier for some type of objects.
 * <p>
 * By default this argument accepts any String. But unlike {@link StringArgument} this class uses
 * {@link #getCompletionSuggestions(CommandInput, CommandContextView, String)} to provide
 * completions for partial inputs.
 */
public abstract class ObjectNameArgument extends ObjectIdArgument<String> {

	public static final int DEFAULT_MINIMUM_COMPLETION_INPUT = 0;

	public ObjectNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ObjectNameArgument(String name, ArgumentFilter<? super String> filter) {
		this(name, false, filter, DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ObjectNameArgument(
			String name,
			boolean joinRemainingArgs,
			ArgumentFilter<? super String> filter,
			int minimumCompletionInput
	) {
		super(
				name,
				new StringArgument(name + ":string", joinRemainingArgs),
				filter,
				minimumCompletionInput
		);
	}

	@Override
	protected String toString(String id) {
		return id; // Already a String
	}
}
