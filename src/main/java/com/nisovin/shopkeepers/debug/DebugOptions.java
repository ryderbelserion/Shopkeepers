package com.nisovin.shopkeepers.debug;

public final class DebugOptions {

	// Logs details of the server version dependent capabilities.
	public static final String capabilities = "capabilities";
	// Logs all events (spams!). Starts slightly delayed. Subsequent calls of the same event get combined into a
	// single logging entry to slightly reduce spam.
	public static final String logAllEvents = "log-all-events";
	// Prints the registered listeners for the first call of each event.
	public static final String printListeners = "print-listeners";
	// Enables debugging output related to shopkeeper activation.
	public static final String shopkeeperActivation = "shopkeeper-activation";
	// Enables additional commands related debugging output.
	public static final String commands = "commands";
	// Logs information when updating stored shop owner names.
	public static final String ownerNameUpdates = "owner-name-updates";
	// Logs whenever a shopkeeper performs item migrations (eg. for trading offers).
	public static final String itemMigrations = "item-migrations";
	// Logs whenever we explicitly convert items to Spigot's data format. Note that this does not log when items get
	// implicitly converted, which may happen under various circumstances.
	public static final String itemConversions = "item-conversions";

	private DebugOptions() {
	}
}
