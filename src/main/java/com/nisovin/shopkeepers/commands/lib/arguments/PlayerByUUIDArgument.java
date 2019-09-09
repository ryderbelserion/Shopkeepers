package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;

/**
 * Determines an online player by the given UUID input.
 * <p>
 * If the option <code>fallbackToSender</code> is used and no matching online player was found and the sender is a
 * player himself, the sender will be returned as result.
 */
public class PlayerByUUIDArgument extends CommandArgument<Player> {

	private final PlayerUUIDArgument playerUUIDArgument;
	private final ArgumentFilter<Player> filter; // not null

	public PlayerByUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerByUUIDArgument(String name, ArgumentFilter<Player> filter) {
		this(name, filter, PlayerUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerByUUIDArgument(String name, ArgumentFilter<Player> filter, int minimalCompletionInput) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		// only accepting uuids of online and accepted players:
		ArgumentFilter<UUID> uuidFilter = new ArgumentFilter<UUID>() {
			@Override
			public boolean test(UUID uuid) {
				Player player = Bukkit.getPlayer(uuid);
				return player != null && PlayerByUUIDArgument.this.filter.test(player);
			}

			@Override
			public String getInvalidArgumentErrorMsg(CommandArgument<UUID> argument, String input, UUID value) {
				Player player = Bukkit.getPlayer(value);
				if (player == null) {
					return PlayerUUIDArgument.ACCEPT_ONLINE_PLAYERS.getInvalidArgumentErrorMsg(argument, input, value);
				} else {
					return PlayerByUUIDArgument.this.filter.getInvalidArgumentErrorMsg(PlayerByUUIDArgument.this, input, player);
				}
			}
		};
		this.playerUUIDArgument = new PlayerUUIDArgument(name + ":uuid", uuidFilter, minimalCompletionInput);
		playerUUIDArgument.setParent(this);
	}

	@Override
	public Player parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		// exceptions (and messages) are handled by the player-uuid argument
		UUID uuid = playerUUIDArgument.parseValue(input, args);
		Player player = Bukkit.getPlayer(uuid);
		assert player != null; // already checked by the player-uuid argument
		return player;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return playerUUIDArgument.complete(input, context, args);
	}
}
