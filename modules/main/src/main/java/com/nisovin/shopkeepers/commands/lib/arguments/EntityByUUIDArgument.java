package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;

/**
 * Determines an existing entity by the given UUID input.
 */
public class EntityByUUIDArgument extends ObjectByIdArgument<UUID, Entity> {

	public EntityByUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public EntityByUUIDArgument(String name, ArgumentFilter<Entity> filter) {
		this(name, filter, EntityUUIDArgument.DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public EntityByUUIDArgument(String name, ArgumentFilter<Entity> filter, int minimumCompletionInput) {
		super(name, filter, new IdArgumentArgs(minimumCompletionInput));
	}

	@Override
	protected ObjectIdArgument<UUID> createIdArgument(String name, IdArgumentArgs args) {
		return new EntityUUIDArgument(name, ArgumentFilter.acceptAny(), args.minimumCompletionInput) {
			@Override
			protected Iterable<UUID> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix) {
				return EntityByUUIDArgument.this.getCompletionSuggestions(input, context, minimumCompletionInput, idPrefix);
			}
		};
	}

	@Override
	protected Entity getObject(CommandInput input, CommandContextView context, UUID uuid) throws ArgumentParseException {
		return Bukkit.getEntity(uuid);
	}

	@Override
	protected Iterable<UUID> getCompletionSuggestions(	CommandInput input, CommandContextView context,
														int minimumCompletionInput, String idPrefix) {
		return EntityUUIDArgument.getDefaultCompletionSuggestions(input, context, minimumCompletionInput, idPrefix, filter);
	}
}
