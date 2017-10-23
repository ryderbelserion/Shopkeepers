package com.nisovin.shopkeepers.util;

import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.plugin.IllegalPluginAccessException;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitWorker;

/**
 * Scheduler-related utilities.
 */
public class SchedulerUtils {

	private SchedulerUtils() {
	}

	public static int getActiveAsyncTasks(Plugin plugin) {
		int workers = 0;
		for (BukkitWorker worker : Bukkit.getScheduler().getActiveWorkers()) {
			if (worker.getOwner().equals(plugin)) {
				workers++;
			}
		}
		return workers;
	}

	private static void validatePluginTask(Plugin plugin, Runnable task) {
		Validate.notNull(plugin, "Plugin is null!");
		Validate.notNull(task, "Task is null!");
	}

	/**
	 * Checks if the current thread is the server's main thread.
	 * 
	 * @return <code>true</code> if currently running on the main thread
	 */
	public static boolean isMainThread() {
		return Bukkit.isPrimaryThread();
	}

	/**
	 * Schedules the given task to be run on the primary thread if required.
	 * 
	 * <p>
	 * If the current thread is already the primary thread, the task will be run immediately. Otherwise it attempts to
	 * schedule the task to run on the server's primary thread. However, the task won't be scheduled if the current
	 * thread is interrupted (for shutdown measures), or if the plugin is <code>null</code> or disabled.
	 * </p>
	 * 
	 * @param plugin
	 *            the plugin to use for scheduling
	 * @param task
	 *            the task
	 * @return <code>true</code> if the task was run or successfully scheduled to be run, <code>false</code> otherwise
	 */
	public static boolean runOnMainThreadOrOmit(Plugin plugin, Runnable task) {
		if (isMainThread()) {
			task.run();
		} else {
			if (plugin == null) return false;
			if (Thread.currentThread().isInterrupted()) return false;
			if (!runTaskOrOmit(plugin, task)) return false;
		}
		return true;
	}

	public static boolean runTaskOrOmit(Plugin plugin, Runnable task) {
		return runTaskLaterOrOmit(plugin, task, 0L);
	}

	public static boolean runTaskLaterOrOmit(Plugin plugin, Runnable task, long delay) {
		validatePluginTask(plugin, task);
		// tasks can only be registered while enabled:
		if (plugin.isEnabled()) {
			try {
				Bukkit.getScheduler().runTaskLater(plugin, task, delay);
				return true;
			} catch (IllegalPluginAccessException e) {
				// couldn't register task: the plugin got disabled just now
			}
		}
		return false;
	}

	public static boolean runAsyncTaskOrOmit(Plugin plugin, Runnable task) {
		return runAsyncTaskLaterOrOmit(plugin, task, 0L);
	}

	public static boolean runAsyncTaskLaterOrOmit(Plugin plugin, Runnable task, long delay) {
		validatePluginTask(plugin, task);
		// tasks can only be registered while enabled:
		if (plugin.isEnabled()) {
			try {
				Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay);
				return true;
			} catch (IllegalPluginAccessException e) {
				// couldn't register task: the plugin got disabled just now
			}
		}
		return false;
	}
}
