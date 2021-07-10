package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.Optional;
import java.util.stream.Stream;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
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
		this(name, filter, PlayerNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerByNameArgument(String name, ArgumentFilter<Player> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	protected ObjectIdArgument<String> createIdArgument(String name, int minimalCompletionInput) {
		return new PlayerNameArgument(name, ArgumentFilter.acceptAny(), minimalCompletionInput) {
			@Override
			protected Iterable<String> getCompletionSuggestions(String idPrefix) {
				return PlayerByNameArgument.this.getCompletionSuggestions(idPrefix);
			}
		};
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Messages.commandPlayerArgumentInvalid;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		return text;
	}

	@Override
	public Player getObject(String nameInput) throws ArgumentParseException {
		// Name input may be both player name or display name:
		Stream<Player> players = PlayerArgumentUtils.PlayerNameMatcher.EXACT.match(nameInput);
		Optional<Player> player = players.findFirst();
		return player.orElse(null);
		// TODO deal with ambiguities
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(String idPrefix) {
		// Note: Whether or not to include display name suggestions usually depends on whether or not the the used
		// matching function considers display names
		return PlayerNameArgument.getDefaultCompletionSuggestions(idPrefix, filter, true);
	}
}
