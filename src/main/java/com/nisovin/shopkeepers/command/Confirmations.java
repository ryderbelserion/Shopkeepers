package com.nisovin.shopkeepers.command;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.Utils;

public class Confirmations {

	private static class ConfirmEntry {
		private final Runnable action;
		private final int taskId;

		public ConfirmEntry(Runnable action, int taskId) {
			this.taskId = taskId;
			this.action = action;
		}

		public int getTaskId() {
			return taskId;
		}

		public Runnable getAction() {
			return action;
		}
	}

	public static final int DEFAULT_CONFIRMATION_TICKS = 25 * 20; // 25 seconds

	private final Plugin plugin;
	// player name -> confirmation data
	private final Map<String, ConfirmEntry> confirming = new HashMap<>();

	public Confirmations(Plugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
	}

	public void onDisable() {
		confirming.clear();
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		this.endConfirmation(player);
	}

	public void awaitConfirmation(Player player, Runnable action) {
		this.awaitConfirmation(player, action, DEFAULT_CONFIRMATION_TICKS);
	}

	public void awaitConfirmation(Player player, Runnable action, int timeoutTicks) {
		Validate.notNull(player, "Player is null!");
		Validate.notNull(action, "Action is null!");
		Validate.isTrue(timeoutTicks > 0, "Timeout has to be positive!");

		int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			this.endConfirmation(player);
			Utils.sendMessage(player, Settings.msgConfirmationExpired);
		}, timeoutTicks).getTaskId();

		ConfirmEntry oldEntry = confirming.put(player.getName(), new ConfirmEntry(action, taskId));
		if (oldEntry != null) {
			// end old confirmation task:
			Bukkit.getScheduler().cancelTask(oldEntry.getTaskId());
		}
	}

	// returns the action that was awaiting confirmation
	public Runnable endConfirmation(Player player) {
		Validate.notNull(player, "Player is null!");
		ConfirmEntry entry = confirming.remove(player.getName());
		if (entry != null) {
			// end confirmation task:
			Bukkit.getScheduler().cancelTask(entry.getTaskId());

			// return action:
			return entry.getAction();
		}
		return null;
	}

	public void handleConfirmation(Player player) {
		Validate.notNull(player, "Player is null!");
		Runnable action = this.endConfirmation(player);
		if (action != null) {
			// execute confirmed action:
			action.run();
		} else {
			Utils.sendMessage(player, Settings.msgNothingToConfirm);
		}
	}
}
