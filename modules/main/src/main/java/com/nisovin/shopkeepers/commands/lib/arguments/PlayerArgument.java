package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.util.PlayerArgumentUtils.PlayerNameMatcher;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Accepts a player specified by either name (might not have to be exact, depending on the used
 * matching function) or UUID.
 */
public class PlayerArgument extends CommandArgument<@NonNull Player> {

	protected final ArgumentFilter<? super @NonNull Player> filter; // Not null
	private final PlayerByNameArgument playerNameArgument;
	private final PlayerByUUIDArgument playerUUIDArgument;
	private final TypedFirstOfArgument<@NonNull Player> firstOfArgument;

	public PlayerArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerArgument(String name, ArgumentFilter<? super @NonNull Player> filter) {
		this(
				name,
				filter,
				PlayerNameArgument.DEFAULT_MINIMUM_COMPLETION_INPUT,
				PlayerUUIDArgument.DEFAULT_MINIMUM_COMPLETION_INPUT
		);
	}

	public PlayerArgument(
			String name,
			ArgumentFilter<? super @NonNull Player> filter,
			int minimalNameCompletionInput,
			int minimumUUIDCompletionInput
	) {
		super(name);
		Validate.notNull(filter, "filter is null");
		this.filter = filter;
		this.playerNameArgument = new PlayerByNameArgument(
				name + ":name",
				filter,
				minimalNameCompletionInput
		) {
			@Override
			public @Nullable Player getObject(
					CommandInput input,
					CommandContextView context,
					String nameInput
			) throws ArgumentParseException {
				return Unsafe.initialized(PlayerArgument.this).getPlayerByName(nameInput);
			}

			@Override
			protected Iterable<? extends @NonNull String> getCompletionSuggestions(
					CommandInput input,
					CommandContextView context,
					int minimumCompletionInput,
					String idPrefix
			) {
				return Unsafe.initialized(PlayerArgument.this).getNameCompletionSuggestions(
						input,
						context,
						minimumCompletionInput,
						idPrefix
				);
			}
		};
		this.playerUUIDArgument = new PlayerByUUIDArgument(
				name + ":uuid",
				filter,
				minimumUUIDCompletionInput
		) {
			@Override
			protected Iterable<? extends @NonNull UUID> getCompletionSuggestions(
					CommandInput input,
					CommandContextView context,
					int minimumCompletionInput,
					String idPrefix
			) {
				return Unsafe.initialized(PlayerArgument.this).getUUIDCompletionSuggestions(
						input,
						context,
						minimumCompletionInput,
						idPrefix
				);
			}
		};
		this.firstOfArgument = new TypedFirstOfArgument<>(
				name + ":firstOf",
				Arrays.asList(playerNameArgument, playerUUIDArgument),
				false,
				false
		);
		firstOfArgument.setParent(this);
	}

	@Override
	public Player parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		// Also handles argument exceptions:
		return firstOfArgument.parseValue(input, context, argsReader);
	}

	@Override
	public List<? extends @NonNull String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return firstOfArgument.complete(input, context, argsReader);
	}

	/**
	 * Gets a {@link Player} which matches the given name input.
	 * <p>
	 * This can be overridden if a different behavior is required. You may also want to override
	 * {@link #getNameCompletionSuggestions(CommandInput, CommandContextView, int, String)} and
	 * {@link #getUUIDCompletionSuggestions(CommandInput, CommandContextView, int, String)} then.
	 * 
	 * @param nameInput
	 *            the name input
	 * @return the matched player, or <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the id is ambiguous
	 */
	public @Nullable Player getPlayerByName(String nameInput) throws IllegalArgumentException {
		// Name input may be both player name or display name:
		Stream<@NonNull Player> players = PlayerNameMatcher.EXACT.match(nameInput);
		Optional<@NonNull Player> player = players.findFirst();
		return player.orElse(null);
		// TODO deal with ambiguities
	}

	/**
	 * Gets the name completion suggestions for the given name prefix.
	 * <p>
	 * This should take this argument's player filter into account.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param minimumCompletionInput
	 *            the minimum input length before completion suggestions are provided
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected Iterable<? extends @NonNull String> getNameCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			int minimumCompletionInput,
			String idPrefix
	) {
		// Note: Whether to include display name suggestions usually depends on whether the used
		// matching function considers display names.
		return PlayerNameArgument.getDefaultCompletionSuggestions(input, context,
				minimumCompletionInput, idPrefix, filter, true);
	}

	/**
	 * Gets the uuid completion suggestions for the given name prefix.
	 * <p>
	 * This should take this argument's player filter into account.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param minimumCompletionInput
	 *            the minimum input length before completion suggestions are provided
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected Iterable<? extends @NonNull UUID> getUUIDCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			int minimumCompletionInput,
			String idPrefix
	) {
		return PlayerUUIDArgument.getDefaultCompletionSuggestions(
				input,
				context,
				minimumCompletionInput,
				idPrefix,
				filter
		);
	}
}
