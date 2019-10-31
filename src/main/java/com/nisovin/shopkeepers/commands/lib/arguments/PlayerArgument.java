package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Arrays;
import java.util.List;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Pair;
import com.nisovin.shopkeepers.util.PlayerUtils;

/**
 * Accepts a player specified by either name (might not have to be exact, depending on the used matching function) or
 * UUID.
 */
public class PlayerArgument extends CommandArgument<Player> {

	private final PlayerByNameArgument playerNameArgument;
	private final PlayerByUUIDArgument playerUUIDArgument;
	private final FirstOfArgument firstOfArgument;

	public PlayerArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerArgument(String name, ArgumentFilter<Player> filter) {
		this(name, filter, PlayerNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT, PlayerUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerArgument(String name, ArgumentFilter<Player> filter, int minimalNameCompletionInput, int minimalUUIDCompletionInput) {
		super(name);
		this.playerNameArgument = new PlayerByNameArgument(name + ":name", filter, minimalNameCompletionInput) {
			@Override
			public Player matchPlayer(String nameInput) {
				return PlayerArgument.this.matchPlayer(nameInput);
			}
		};
		this.playerUUIDArgument = new PlayerByUUIDArgument(name + ":uuid", filter, minimalUUIDCompletionInput);
		this.firstOfArgument = new FirstOfArgument(name + ":firstOf", Arrays.asList(playerNameArgument, playerUUIDArgument), false, false);
		firstOfArgument.setParent(this);
	}

	@Override
	public Player parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// also handles argument exceptions:
		Pair<CommandArgument<?>, Object> result = firstOfArgument.parseValue(input, context, argsReader);
		return (result == null) ? null : (Player) result.getSecond();
	}

	/**
	 * Gets a {@link Player} which matches the given name input.
	 * <p>
	 * This can be overridden if a different behavior is required.
	 * 
	 * @param nameInput
	 *            the raw name input
	 * @return the matched player, or <code>null</code>
	 */
	public Player matchPlayer(String nameInput) {
		return PlayerUtils.NameMatchers.DEFAULT.match(nameInput);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return firstOfArgument.complete(input, context, argsReader);
	}
}
