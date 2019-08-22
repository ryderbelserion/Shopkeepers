package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

/**
 * Valid formats:
 * <ul>
 * <li>playerName (might not have to be exact, depending on matching function)
 * <li>playerUUID
 * </ul>
 */
public class PlayerArgument extends CommandArgument {

	private final boolean fallbackToSender;

	public PlayerArgument(String name) {
		this(name, false);
	}

	public PlayerArgument(String name, boolean fallbackToSender) {
		super(name);
		this.fallbackToSender = fallbackToSender;
	}

	@Override
	public String getMissingArgumentErrorMsg() {
		return TextUtils.replaceArgs(Settings.msgCommandPlayerArgumentMissing,
				"{argumentName}", this.getName(),
				"{argumentFormat}", this.getFormat());
	}

	@Override
	public String getInvalidArgumentErrorMsg(String argument) {
		if (argument == null) argument = "";
		return TextUtils.replaceArgs(Settings.msgCommandPlayerArgumentInvalid,
				"{argumentName}", this.getName(),
				"{argumentFormat}", this.getFormat(),
				"{argument}", argument);
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		Player player = null;
		String playerArg = null;
		Object argsState = args.getState();
		if (args.hasNext()) {
			playerArg = args.next();
			// check if the argument matches a player name:
			player = this.matchPlayer(playerArg);
			if (player == null) {
				// check if argument matches a player uuid:
				UUID uuid = ConversionUtils.parseUUID(playerArg);
				if (uuid != null) {
					player = Bukkit.getPlayer(uuid);
				}
			}
		}

		if (player == null && fallbackToSender) {
			// check if sender is a player:
			CommandSender sender = input.getSender();
			if (sender instanceof Player) {
				player = (Player) sender;
				// reset arguments:
				args.setState(argsState);
			}
		}

		if (player == null) {
			// throw argument parse exception:
			if (playerArg != null) {
				throw this.invalidArgument(playerArg);
			} else {
				throw this.missingArgument();
			}
		}

		return player;
	}

	/**
	 * Gets a {@link Player} which matches the given player name input.
	 * <p>
	 * By default this uses {@link Bukkit#getPlayer(String)}. This can be overridden if a different behavior is
	 * required.
	 * 
	 * @param playerNameInput
	 *            the raw player name input
	 * @return the player, can be <code>null</code>
	 */
	@SuppressWarnings("deprecation")
	public Player matchPlayer(String playerNameInput) {
		return Bukkit.getPlayer(playerNameInput);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		if (args.getRemainingSize() == 1) {
			List<String> suggestions = new ArrayList<>();
			String partialPlayerName = args.next().toLowerCase();
			for (Player player : Bukkit.getOnlinePlayers()) {
				String playerName = player.getName();
				if (playerName.toLowerCase().startsWith(partialPlayerName)) {
					suggestions.add(playerName);
				} else {
					String displayName = ChatColor.stripColor(player.getDisplayName());
					if (displayName.toLowerCase().startsWith(partialPlayerName)) {
						suggestions.add(displayName);
					}
				}
			}
			return Collections.unmodifiableList(suggestions);
		}
		return Collections.emptyList();
	}
}
