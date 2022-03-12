package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Optional;
import java.util.stream.Stream;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.util.PlayerArgumentUtils;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;

/**
 * Determines an online player by the given name input.
 */
public class PlayerByNameArgument extends ObjectByIdArgument<String, Player> {

	public PlayerByNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerByNameArgument(String name, ArgumentFilter<Player> filter) {
		this(name, filter, PlayerNameArgument.DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public PlayerByNameArgument(String name, ArgumentFilter<Player> filter, int minimumCompletionInput) {
		super(name, filter, new IdArgumentArgs(minimumCompletionInput));
	}

	@Override
	protected ObjectIdArgument<String> createIdArgument(String name, IdArgumentArgs args) {
		return new PlayerNameArgument(name, ArgumentFilter.acceptAny(), args.minimumCompletionInput) {
			@Override
			protected Iterable<String> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix) {
				return PlayerByNameArgument.this.getCompletionSuggestions(input, context, minimumCompletionInput, idPrefix);
			}
		};
	}

	@Override
	protected Text getInvalidArgumentErrorMsgText() {
		return Messages.commandPlayerArgumentInvalid;
	}

	@Override
	public Player getObject(CommandInput input, CommandContextView context, String nameInput) throws ArgumentParseException {
		// Name input may be both player name or display name:
		Stream<Player> players = PlayerArgumentUtils.PlayerNameMatcher.EXACT.match(nameInput);
		Optional<Player> player = players.findFirst();
		return player.orElse(null);
		// TODO deal with ambiguities
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(CommandInput input, CommandContextView context,
														int minimumCompletionInput, String idPrefix) {
		// Note: Whether to include display name suggestions usually depends on whether the used matching function
		// considers display names.
		return PlayerNameArgument.getDefaultCompletionSuggestions(input, context, minimumCompletionInput, idPrefix, filter, true);
	}
}
