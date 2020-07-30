package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.PlayerUtils;

/**
 * Accepts a player specified by either name (might not have to be exact, depending on the used matching function) or
 * UUID.
 */
public class PlayerArgument extends CommandArgument<Player> {

	protected final ArgumentFilter<Player> filter; // not null
	private final PlayerByNameArgument playerNameArgument;
	private final PlayerByUUIDArgument playerUUIDArgument;
	private final TypedFirstOfArgument<Player> firstOfArgument;

	public PlayerArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerArgument(String name, ArgumentFilter<Player> filter) {
		this(name, filter, PlayerNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT, PlayerUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerArgument(String name, ArgumentFilter<Player> filter, int minimalNameCompletionInput, int minimalUUIDCompletionInput) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		this.playerNameArgument = new PlayerByNameArgument(name + ":name", filter, minimalNameCompletionInput) {
			@Override
			public Player getObject(String nameInput) throws ArgumentParseException {
				return PlayerArgument.this.getPlayerByName(nameInput);
			}

			@Override
			protected Iterable<String> getCompletionSuggestions(String idPrefix) {
				return PlayerArgument.this.getNameCompletionSuggestions(idPrefix);
			}
		};
		this.playerUUIDArgument = new PlayerByUUIDArgument(name + ":uuid", filter, minimalUUIDCompletionInput) {
			@Override
			protected Iterable<UUID> getCompletionSuggestions(String idPrefix) {
				return PlayerArgument.this.getUUIDCompletionSuggestions(idPrefix);
			}
		};
		this.firstOfArgument = new TypedFirstOfArgument<>(name + ":firstOf", Arrays.asList(playerNameArgument, playerUUIDArgument), false, false);
		firstOfArgument.setParent(this);
	}

	@Override
	public Player parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// Also handles argument exceptions:
		return firstOfArgument.parseValue(input, context, argsReader);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return firstOfArgument.complete(input, context, argsReader);
	}

	/**
	 * Gets a {@link Player} which matches the given name input.
	 * <p>
	 * This can be overridden if a different behavior is required. You may also want to override
	 * {@link #getNameCompletionSuggestions(String)} and {@link #getUUIDCompletionSuggestions(String)} then.
	 * 
	 * @param nameInput
	 *            the name input
	 * @return the matched player, or <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the id is ambiguous
	 */
	public Player getPlayerByName(String nameInput) throws IllegalArgumentException {
		// Name input may be both player name or display name:
		Stream<Player> players = PlayerUtils.PlayerNameMatcher.EXACT.match(nameInput);
		Optional<Player> player = players.findFirst();
		return player.orElse(null);
		// TODO deal with ambiguities
	}

	/**
	 * Gets the name completion suggestions for the given name prefix.
	 * <p>
	 * This should take this argument's player filter into account.
	 * 
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected Iterable<String> getNameCompletionSuggestions(String idPrefix) {
		// Note: Whether or not to include display name suggestions usually depends on whether or not the the used
		// matching function considers display names
		return PlayerNameArgument.getDefaultCompletionSuggestions(idPrefix, filter, true);
	}

	/**
	 * Gets the uuid completion suggestions for the given name prefix.
	 * <p>
	 * This should take this argument's player filter into account.
	 * 
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected Iterable<UUID> getUUIDCompletionSuggestions(String idPrefix) {
		return PlayerUUIDArgument.getDefaultCompletionSuggestions(idPrefix, filter);
	}
}
