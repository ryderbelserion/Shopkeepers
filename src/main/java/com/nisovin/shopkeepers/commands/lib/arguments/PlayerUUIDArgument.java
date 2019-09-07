package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.UUID;

import org.bukkit.Bukkit;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.util.TextUtils;

/**
 * Provides suggestions for the UUIDs of online players.
 * <p>
 * By default this accepts any UUID regardless of whether it corresponds to an online player.
 */
public class PlayerUUIDArgument extends ObjectUUIDArgument {

	public static final ArgumentFilter<UUID> ACCEPT_ONLINE_PLAYERS = new ArgumentFilter<UUID>() {
		@Override
		public boolean test(UUID uuid) {
			// Only accept UUIDs corresponding to an online player
			return (Bukkit.getPlayer(uuid) != null);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<UUID> argument, String input, UUID value) {
			if (input == null) input = "";
			return TextUtils.replaceArgs(Settings.msgCommandPlayerArgumentInvalid,
					"{argumentName}", argument.getName(),
					"{argumentFormat}", argument.getFormat(),
					"{argument}", input);
		}
	};

	public PlayerUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerUUIDArgument(String name, ArgumentFilter<UUID> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public PlayerUUIDArgument(String name, ArgumentFilter<UUID> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	public String getMissingArgumentErrorMsg() {
		return TextUtils.replaceArgs(Settings.msgCommandPlayerArgumentMissing,
				"{argumentName}", this.getName(),
				"{argumentFormat}", this.getFormat());
	}

	// override this to limit which player uuids get used for suggestions
	@Override
	protected Iterable<UUID> getKnownIds() {
		return Bukkit.getOnlinePlayers().stream().map((player) -> {
			return player.getUniqueId();
		})::iterator;
	}
}
