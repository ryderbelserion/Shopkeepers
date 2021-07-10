package com.nisovin.shopkeepers.commands.lib.util;

import java.util.Iterator;
import java.util.function.ObjIntConsumer;

import org.bukkit.command.CommandSender;

public class CommandArgumentUtils {

	private CommandArgumentUtils() {
	}

	private static final int DEFAULT_AMBIGUOUS_INPUT_MAX_ENTRIES = 5;

	// Note: Iterable is only iterated once.
	// true if there are multiple matches.
	public static <T> boolean handleAmbiguousInput(	CommandSender sender, String input, Iterable<T> matches,
													Runnable sendHeader, ObjIntConsumer<T> sendEntry, Runnable sendMore) {
		return handleAmbiguousInput(sender, input, matches, DEFAULT_AMBIGUOUS_INPUT_MAX_ENTRIES, sendHeader, sendEntry, sendMore);
	}

	// Note: Iterable is only iterated once.
	// true if there are multiple matches.
	public static <T> boolean handleAmbiguousInput(	CommandSender sender, String input, Iterable<T> matches, int maxEntries,
													Runnable sendHeader, ObjIntConsumer<T> sendEntry, Runnable sendMore) {
		assert sender != null && input != null && matches != null && sendHeader != null && sendEntry != null && sendMore != null;
		Iterator<T> matchesIter = matches.iterator();
		if (!matchesIter.hasNext()) return false; // Empty -> Not ambiguous.
		T match = matchesIter.next();
		if (!matchesIter.hasNext()) return false; // Only one element -> Not ambiguous.

		// Header:
		sendHeader.run();

		int index = 1;
		while (true) {
			if (index > maxEntries) { // Limit the number of listed match entries
				sendMore.run();
				break;
			}

			sendEntry.accept(match, index);

			if (matchesIter.hasNext()) {
				match = matchesIter.next();
				index++;
			} else {
				break;
			}
		}
		return true;
	}
}
