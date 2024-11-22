package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.bukkit.EntityUtils;

/**
 * Argument for entity UUIDs.
 * <p>
 * By default this accepts any UUID regardless of whether it corresponds to an existing entity.
 */
public class EntityUUIDArgument extends ObjectUUIDArgument {

	public static final int DEFAULT_MINIMUM_COMPLETION_INPUT = ObjectUUIDArgument.DEFAULT_MINIMUM_COMPLETION_INPUT;

	// Note: Not providing a default argument filter that only accepts uuids of existing entities,
	// because this can be
	// achieved more efficiently by using EntityByUUIDArgument instead.

	public EntityUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public EntityUUIDArgument(String name, ArgumentFilter<? super UUID> filter) {
		this(name, filter, DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public EntityUUIDArgument(
			String name,
			ArgumentFilter<? super UUID> filter,
			int minimumCompletionInput
	) {
		super(name, filter, minimumCompletionInput);
	}

	// Using the uuid argument's 'missing argument' message if the uuid is missing.
	// Using the uuid argument's 'invalid argument' message if the uuid is invalid.
	// Using the filter's 'invalid argument' message if the uuid is not accepted.

	/**
	 * Gets the default uuid completion suggestions.
	 * <p>
	 * This always suggests the uuid of the targeted entity, regardless of the
	 * {@code minimumCompletionInput} argument.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param minimumCompletionInput
	 *            the minimum input length before completion suggestions are provided
	 * @param uuidPrefix
	 *            the uuid prefix, may be empty, not <code>null</code>
	 * @param filter
	 *            only suggestions for entities accepted by this filter are included, not
	 *            <code>null</code>
	 * @return the entity uuid completion suggestions
	 */
	public static Iterable<? extends UUID> getDefaultCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			int minimumCompletionInput,
			String uuidPrefix,
			ArgumentFilter<? super Entity> filter
	) {
		// Suggestion for the unique id of the targeted entity:
		CommandSender sender = input.getSender();
		if (sender instanceof Player) {
			Player player = (Player) sender;
			Entity targetEntity = EntityUtils.getTargetedEntity(
					player,
					entity -> filter.test(input, context, entity)
			);
			if (targetEntity != null) {
				String normalizedUUIDPrefix = uuidPrefix.toLowerCase(Locale.ROOT);
				if (targetEntity.getUniqueId().toString().startsWith(normalizedUUIDPrefix)) {
					return Collections.singleton(targetEntity.getUniqueId());
				}
			}
		}
		return Collections.emptyList();
	}

	@Override
	protected Iterable<? extends UUID> getCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			String idPrefix
	) {
		return getDefaultCompletionSuggestions(
				input,
				context,
				minimumCompletionInput,
				idPrefix,
				ArgumentFilter.acceptAny()
		);
	}
}
