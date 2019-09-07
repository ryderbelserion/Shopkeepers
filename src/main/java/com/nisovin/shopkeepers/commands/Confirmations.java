package com.nisovin.shopkeepers.commands;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.util.TextUtils;
import com.nisovin.shopkeepers.util.Validate;

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
	// null name is used for console confirmations
	private final Map<String, ConfirmEntry> confirming = new HashMap<>();

	public Confirmations(Plugin plugin) {
		this.plugin = plugin;
	}

	public void onEnable() {
	}

	public void onDisable() {
		confirming.clear();
	}

	private String getSenderKey(CommandSender sender) {
		if (sender instanceof Player) {
			return sender.getName(); // player's name
		} else {
			// any other command sender, such as console:
			return null;
		}
	}

	public void onPlayerQuit(Player player) {
		assert player != null;
		this.endConfirmation(player);
	}

	public void awaitConfirmation(CommandSender sender, Runnable action) {
		this.awaitConfirmation(sender, action, DEFAULT_CONFIRMATION_TICKS);
	}

	public void awaitConfirmation(CommandSender sender, Runnable action, int timeoutTicks) {
		Validate.notNull(sender, "Sender is null!");
		Validate.notNull(action, "Action is null!");
		Validate.isTrue(timeoutTicks > 0, "Timeout has to be positive!");

		int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			this.endConfirmation(sender);
			TextUtils.sendMessage(sender, Settings.msgConfirmationExpired);
		}, timeoutTicks).getTaskId();

		ConfirmEntry oldEntry = confirming.put(this.getSenderKey(sender), new ConfirmEntry(action, taskId));
		if (oldEntry != null) {
			// end old confirmation task:
			Bukkit.getScheduler().cancelTask(oldEntry.getTaskId());
		}
	}

	// returns the action that was awaiting confirmation
	public Runnable endConfirmation(CommandSender sender) {
		Validate.notNull(sender, "Sender is null!");
		ConfirmEntry entry = confirming.remove(this.getSenderKey(sender));
		if (entry != null) {
			// end confirmation task:
			Bukkit.getScheduler().cancelTask(entry.getTaskId());

			// return action:
			return entry.getAction();
		}
		return null;
	}

	public void handleConfirmation(CommandSender sender) {
		Validate.notNull(sender, "Sender is null!");
		Runnable action = this.endConfirmation(sender);
		if (action != null) {
			// execute confirmed action:
			action.run();
		} else {
			TextUtils.sendMessage(sender, Settings.msgNothingToConfirm);
		}
	}
}
