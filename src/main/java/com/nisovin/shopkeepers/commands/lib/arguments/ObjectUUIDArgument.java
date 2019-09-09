package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Locale;
import java.util.UUID;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;

/**
 * Base class for arguments that accept an UUID when there is a limited set of known applicable UUIDs (eg. UUIDs
 * identifying a known set of objects).
 * <p>
 * By default this argument actually accepts any UUID. But unlike {@link UUIDArgument} this class uses
 * {@link #getKnownIds()} to provide completions for partial inputs.
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
		// matching doesn't make much sense for UUIDs
		super(name, new UUIDArgument(name + ":uuid"), filter, false, minimalCompletionInput);
	}

	@Override
	protected UUID matchKnownId(UUID input) {
		return input; // no matching for uuids
	}

	@Override
	protected String toString(UUID id) {
		return id.toString();
	}

	@Override
	protected String normalize(String idString) {
		return idString.toLowerCase(Locale.ROOT);
	}
}
