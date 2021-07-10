package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.UUID;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;

/**
 * Base class for arguments that accept an UUID as identifier for some type of objects.
 * <p>
 * By default this argument accepts any UUID. But unlike {@link UUIDArgument} this class uses
 * {@link #getCompletionSuggestions(String)} to provide completions for partial inputs.
 */
public abstract class ObjectUUIDArgument extends ObjectIdArgument<UUID> {

	public static final int DEFAULT_MINIMAL_COMPLETION_INPUT = 3;

	public ObjectUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ObjectUUIDArgument(String name, ArgumentFilter<UUID> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ObjectUUIDArgument(String name, ArgumentFilter<UUID> filter, int minimalCompletionInput) {
		super(name, new UUIDArgument(name + ":uuid"), filter, minimalCompletionInput);
	}

	@Override
	protected String toString(UUID id) {
		return id.toString();
	}
}
