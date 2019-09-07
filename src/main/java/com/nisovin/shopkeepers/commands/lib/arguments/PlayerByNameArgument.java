package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.PlayerUtils;

/**
 * Determines an online player by the given name input.
 * <p>
 * If the option <code>fallbackToSender</code> is used and no online player matches the input (according to the used
 * matching function) and the sender is a player himself, the sender will be returned as result.
 */
public class PlayerByNameArgument extends CommandArgument<Player> {

	private final PlayerNameArgument playerNameArgument;
	private final ArgumentFilter<Player> filter; // not null
	// avoid duplicate name lookups and name matching by keeping track of the matched player:
	private boolean playerMatched = false;
	private Player matchedPlayer = null;

	public PlayerByNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerByNameArgument(String name, ArgumentFilter<Player> filter) {
		this(name, filter, PlayerNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerByNameArgument(String name, ArgumentFilter<Player> filter, int minimalCompletionInput) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		// only accepting names of online and accepted players:
		ArgumentFilter<String> nameFilter = new ArgumentFilter<String>() {
			@Override
			public boolean test(String name) {
				Player player = getPlayerByName(name);
				return player != null && PlayerByNameArgument.this.filter.test(player);
			}

			@Override
			public String getInvalidArgumentErrorMsg(CommandArgument<String> argument, String input, String value) {
				Player player = getPlayerByName(name);
				if (player == null) {
					return PlayerNameArgument.ACCEPT_ONLINE_PLAYERS.getInvalidArgumentErrorMsg(argument, input, value);
				} else {
					return PlayerByNameArgument.this.filter.getInvalidArgumentErrorMsg(PlayerByNameArgument.this, input, player);
				}
			}
		};
		// always match known names: we keep track of the matched player to avoid duplicate name lookups later (eg. for
		// the filters)
		this.playerNameArgument = new PlayerNameArgument(name, nameFilter, true, minimalCompletionInput) {
			@Override
			protected String matchKnownId(String input) {
				Player player = PlayerByNameArgument.this.matchPlayer(input);
				// keep track of whether and which Player got matched:
				playerMatched = true;
				matchedPlayer = player;
				return (player == null) ? input : player.getName();
			}
		};
	}

	private Player getPlayerByName(String name) {
		// use the cached player if available:
		if (playerMatched) {
			return matchedPlayer; // can be null if no player matched the name
		} else {
			// lookup by name:
			return Bukkit.getPlayerExact(name);
		}
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
	public Player parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		try {
			// exceptions (and messages) are handled by the player-name argument
			String name = playerNameArgument.parseValue(input, args);
			// we found a matching and accepted player, otherwise the name argument and its filters would have thrown an
			// exception
			assert playerMatched && matchedPlayer != null && matchedPlayer.getName().equals(name) && filter.test(matchedPlayer);
			return matchedPlayer;
		} finally {
			// reset cached shopkeeper:
			playerMatched = false;
			matchedPlayer = null;
		}
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return playerNameArgument.complete(input, context, args);
	}
}
