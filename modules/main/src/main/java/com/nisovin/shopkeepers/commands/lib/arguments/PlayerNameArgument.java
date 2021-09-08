package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Locale;
import java.util.function.Predicate;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * Provides suggestions for names of online players.
 * <p>
 * By default this accepts any name regardless of whether it corresponds to an online player.
 */
public class PlayerNameArgument extends ObjectNameArgument {

	public static final int DEFAULT_MINIMAL_COMPLETION_INPUT = ObjectNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT;

	// Note: Not providing a default argument filter that only accepts names of online players, because this can be
	// achieved more efficiently by using PlayerByNameArgument instead.

	public PlayerNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public PlayerNameArgument(String name, ArgumentFilter<String> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	// Joining remaining args doesn't make much sense for player names (and we normalize whitespace in display names).
	public PlayerNameArgument(String name, ArgumentFilter<String> filter, int minimalCompletionInput) {
		super(name, false, filter, minimalCompletionInput);
	}

	@Override
	public Text getMissingArgumentErrorMsg() {
		Text text = Messages.commandPlayerArgumentMissing;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		return text;
	}

	// Using the filter's 'invalid argument' message if the name is not accepted.

	/**
	 * Gets the default name completion suggestions.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param namePrefix
	 *            the name prefix, may be empty, not <code>null</code>
	 * @param playerFilter
	 *            only suggestions for players accepted by this predicate get included
	 * @param includeDisplayNames
	 *            <code>true</code> to include display name suggestions
	 * @return the player name completion suggestions
	 */
	public static Iterable<String> getDefaultCompletionSuggestions(	CommandInput input, CommandContextView context,
																	String namePrefix, Predicate<Player> playerFilter,
																	boolean includeDisplayNames) {
		// Assumption: Name prefix does not contain color codes (users are not expected to specify color codes).
		// Normalizes whitespace and converts to lowercase:
		String normalizedNamePrefix = StringUtils.normalize(namePrefix);
		return Bukkit.getOnlinePlayers().stream()
				.filter(playerFilter)
				.map(player -> {
					// Note: Not suggesting both the name and display name for the same player.
					// Assumption: Player names don't contain whitespace or color codes
					String name = player.getName();
					if (StringUtils.normalize(name).startsWith(normalizedNamePrefix)) {
						return name;
					} else if (includeDisplayNames) {
						String displayName = TextUtils.stripColor(player.getDisplayName());
						String normalizedWithCase = StringUtils.normalizeKeepCase(displayName);
						String normalized = normalizedWithCase.toLowerCase(Locale.ROOT);
						if (normalized.startsWith(normalizedNamePrefix)) {
							return normalizedWithCase;
						}
					}
					return null; // no match
				}).filter(name -> name != null)::iterator;
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix) {
		return getDefaultCompletionSuggestions(input, context, idPrefix, (player) -> true, true);
	}
}
