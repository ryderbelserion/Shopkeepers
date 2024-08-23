package com.nisovin.shopkeepers.tradenotifications;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.util.java.Validate;

public class NotificationUserPreferences implements Listener {

	private static class UserPreferences {

		public boolean notifyOnTrades = true;
		public boolean receivedDisableTradeNotificationsHint = false;
	}

	private final Plugin plugin;
	private final Map<UUID, UserPreferences> userPreferences = new HashMap<>();

	public NotificationUserPreferences(Plugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public void onDisable() {
		HandlerList.unregisterAll(this);
		userPreferences.clear();
	}

	private UserPreferences getOrCreateUserPreferences(Player player) {
		Validate.notNull(player, "player is null");
		UserPreferences preferences = userPreferences.computeIfAbsent(
				player.getUniqueId(),
				playerId -> new UserPreferences()
		);
		assert preferences != null;
		return preferences;
	}

	public boolean hasReceivedDisableTradeNotificationsHint(Player player) {
		return this.getOrCreateUserPreferences(player).receivedDisableTradeNotificationsHint;
	}

	public void setReceivedDisableTradeNotificationsHint(Player player, boolean received) {
		this.getOrCreateUserPreferences(player).receivedDisableTradeNotificationsHint = received;
	}

	public boolean isNotifyOnTrades(Player player) {
		return this.getOrCreateUserPreferences(player).notifyOnTrades;
	}

	public void setNotifyOnTrades(Player player, boolean notify) {
		this.getOrCreateUserPreferences(player).notifyOnTrades = notify;
	}

	private void clearUserPreferences(Player player) {
		Validate.notNull(player, "player is null");
		userPreferences.remove(player.getUniqueId());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	void onPlayerQuit(PlayerQuitEvent event) {
		this.clearUserPreferences(event.getPlayer());
	}
}
