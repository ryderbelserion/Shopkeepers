package com.nisovin.shopkeepers.debug;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public final class DebugOptions {

	private static final Set<String> allOptions = new LinkedHashSet<>();
	private static final Set<String> allOptionsView = Collections.unmodifiableSet(allOptions);

	// Logs details of the server version dependent capabilities.
	public static final String capabilities = add("capabilities");
	// Logs all events (spams!). Starts slightly delayed. Subsequent calls of the same event get combined into a
	// single logging entry to slightly reduce spam.
	public static final String logAllEvents = add("log-all-events");
	// Prints the registered listeners for the first call of each event.
	public static final String printListeners = add("print-listeners");
	// Enables debugging output related to shopkeeper activation.
	public static final String shopkeeperActivation = add("shopkeeper-activation");
	// Enables debug output related to shopkeeper ticking activities that may be considered non-exceptional, and might
	// occur regularly and therefore cause debug spam otherwise. This includes, for example, the activity of teleporting
	// shopkeepers back into place, or updating a shopkeeper's location when a mobile shopkeeper NPC moved around.
	public static final String regularTickActivities = add("regular-tick-activities");
	// Visualizes the ticking activities of shopkeepers in-game.
	public static final String visualizeShopkeeperTicks = add("visualize-shopkeeper-ticks");
	// Enables additional commands related debugging output.
	public static final String commands = add("commands");
	// Logs information when updating stored shop owner names.
	public static final String ownerNameUpdates = add("owner-name-updates");
	// Logs whenever a shopkeeper performs item migrations (eg. for trading offers).
	public static final String itemMigrations = add("item-migrations");
	// Logs whenever we explicitly convert items to Spigot's data format. Note that this does not log when items get
	// implicitly converted, which may happen under various circumstances.
	public static final String itemConversions = add("item-conversions");

	private static String add(String debugOption) {
		allOptions.add(debugOption);
		return debugOption;
	}

	public static Set<String> getAll() {
		return allOptionsView;
	}

	private DebugOptions() {
	}
}
