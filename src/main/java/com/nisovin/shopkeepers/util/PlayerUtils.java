package com.nisovin.shopkeepers.util;

import java.util.Locale;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class PlayerUtils {

	private PlayerUtils() {
	}

	public static class NameMatchers {

		private NameMatchers() {
		}

		public static final ObjectMatcher<Player> EXACT = new ObjectMatcher<Player>() {
			@Override
			public Player match(String input) {
				if (input == null || input.isEmpty()) return null;
				return Bukkit.getPlayerExact(input);
			}
		};

		public static final ObjectMatcher<Player> STARTS_WITH = new ObjectMatcher<Player>() {
			@Override
			public Player match(String input) {
				// Note: Similar to Bukkit.getPlayer(String) but also considers display names.
				if (input == null || input.isEmpty()) return null;

				// Check for an exact match:
				Player foundPlayer = Bukkit.getPlayerExact(input);
				if (foundPlayer != null) return foundPlayer;

				String playerNameLower = input.toLowerCase(Locale.ROOT);
				int delta = Integer.MAX_VALUE;
				for (Player player : Bukkit.getOnlinePlayers()) {
					// check name:
					String playerName = player.getName();
					if (playerName.toLowerCase(Locale.ROOT).startsWith(playerNameLower)) {
						int currentDelta = Math.abs(playerName.length() - playerNameLower.length());
						if (currentDelta < delta) {
							foundPlayer = player;
							delta = currentDelta;
						}
						if (currentDelta == 0) break;
					}

					// check display name:
					String displayName = ChatColor.stripColor(player.getDisplayName());
					if (displayName.toLowerCase(Locale.ROOT).startsWith(playerNameLower)) {
						int currentDelta = Math.abs(displayName.length() - playerNameLower.length());
						if (currentDelta < delta) {
							foundPlayer = player;
							delta = currentDelta;
						}
						if (currentDelta == 0) break;
					}
				}
				return foundPlayer;
			}
		};

		public static final ObjectMatcher<Player> CONTAINS = new ObjectMatcher<Player>() {
			@Override
			public Player match(String input) {
				// Note: Similar to Bukkit.matchPlayer(String) but also considers display names.
				if (input == null || input.isEmpty()) return null;

				// Check for an exact match:
				Player foundPlayer = Bukkit.getPlayerExact(input);
				if (foundPlayer != null) return foundPlayer;

				String playerNameLower = input.toLowerCase(Locale.ROOT);
				int delta = Integer.MAX_VALUE;
				for (Player player : Bukkit.getOnlinePlayers()) {
					// check name:
					String playerName = player.getName();
					if (playerName.toLowerCase(Locale.ROOT).contains(playerNameLower)) {
						int currentDelta = Math.abs(playerName.length() - playerNameLower.length());
						if (currentDelta < delta) {
							foundPlayer = player;
							delta = currentDelta;
						}
						if (currentDelta == 0) break;
					}

					// check display name:
					String displayName = player.getDisplayName();
					if (displayName.toLowerCase(Locale.ROOT).contains(playerNameLower)) {
						int currentDelta = Math.abs(displayName.length() - playerNameLower.length());
						if (currentDelta < delta) {
							foundPlayer = player;
							delta = currentDelta;
						}
						if (currentDelta == 0) break;
					}
				}
				return foundPlayer;
			}
		};

		public static final ObjectMatcher<Player> DEFAULT = STARTS_WITH;
	}
}
