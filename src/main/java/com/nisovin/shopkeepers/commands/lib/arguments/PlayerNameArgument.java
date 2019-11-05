package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.PlayerUtils;
import com.nisovin.shopkeepers.util.TextUtils;

/**
 * By default this accepts any String regardless of whether it corresponds to an online player, but provides suggestions
 * for the names of online players.
 * <p>
 * If the option <code>matchKnownNames</code> is used and an online player matches the input (according to the used
 * matching function), the player's actual name will be returned instead of the used input.
 * <p>
 * If the option <code>fallbackToSender</code> is used and no online player matches the input and the sender is a
 * player, the sender's name will be returned as result.
 */
public class PlayerNameArgument extends ObjectNameArgument {

	public static final ArgumentFilter<String> ACCEPT_ONLINE_PLAYERS = new ArgumentFilter<String>() {
		@Override
		public boolean test(String name) {
			// Only accept names corresponding to an online player
			// Needs to be overridden if a different name matching shall be used
			return (Bukkit.getPlayerExact(name) != null);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<String> argument, String argumentInput, String value) {
			if (argumentInput == null) argumentInput = "";
			String[] defaultArgs = argument.getDefaultErrorMsgArgs();
			return TextUtils.replaceArgs(Settings.msgCommandPlayerArgumentInvalid,
					defaultArgs, "{argument}", argumentInput);
		}
	};

	public PlayerNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerNameArgument(String name, ArgumentFilter<String> filter) {
		this(name, filter, true);
	}

	public PlayerNameArgument(String name, ArgumentFilter<String> filter, boolean matchKnownNames) {
		this(name, filter, matchKnownNames, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	// joining remaining args doesn't make much sense for player names
	public PlayerNameArgument(String name, ArgumentFilter<String> filter, boolean matchKnownNames, int minimalCompletionInput) {
		super(name, false, filter, matchKnownNames, minimalCompletionInput);
	}

	@Override
	public String getMissingArgumentErrorMsg() {
		String[] defaultArgs = this.getDefaultErrorMsgArgs();
		return TextUtils.replaceArgs(Settings.msgCommandPlayerArgumentMissing, defaultArgs);
	}

	// using the filter's 'invalid player' message if the name is not accepted

	// override this to limit which player names get used for suggestions
	// Note: this is not actually used right now
	@Override
	protected Iterable<String> getKnownIds() {
		return Bukkit.getOnlinePlayers().stream().map((player) -> {
			return player.getName();
		})::iterator;
	}

	// This can be overridden if a different matching behavior is required.
	@Override
	protected String matchKnownId(String input) {
		Player player = PlayerUtils.NameMatchers.DEFAULT.match(input);
		return (player == null) ? input : player.getName();
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		// Note: Custom completion code for now to handle display names correctly (not suggesting both the name and
		// display name for the same player).
		if (argsReader.getRemainingSize() != 1) {
			// there are no remaining arguments to complete, or we are not using up the final argument
			return Collections.emptyList();
		}

		String nameArg = argsReader.next(); // can be empty
		if (nameArg.length() < minimalCompletionInput) {
			// only provide suggestions if there is a minimal length input
			return Collections.emptyList();
		}
		String partialName = this.normalize(nameArg);
		List<String> suggestions = new ArrayList<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			String playerName = player.getName();
			if (!filter.test(playerName)) continue; // skip
			if (this.normalize(playerName).startsWith(partialName)) {
				suggestions.add(playerName);
			} else {
				String displayName = ChatColor.stripColor(player.getDisplayName());
				if (this.normalize(displayName).startsWith(partialName)) {
					suggestions.add(displayName);
				}
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
