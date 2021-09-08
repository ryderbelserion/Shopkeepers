package com.nisovin.shopkeepers.commands.arguments;

import java.util.Locale;
import java.util.UUID;
import java.util.function.Predicate;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectUUIDArgument;

/**
 * Provides suggestions for the UUIDs of existing shopkeepers.
 * <p>
 * By default this accepts any UUID regardless of whether it corresponds to an existing shopkeeper.
 */
public class ShopkeeperUUIDArgument extends ObjectUUIDArgument {

	public static final int DEFAULT_MINIMAL_COMPLETION_INPUT = ObjectUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT;

	// Note: Not providing default argument filters that only accept existing shops, admin shops, or player shops,
	// because this can be achieved more efficiently by using ShopkeeperByUUIDArgument instead.

	public ShopkeeperUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperUUIDArgument(String name, ArgumentFilter<UUID> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperUUIDArgument(String name, ArgumentFilter<UUID> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	// Using the regular 'missing argument' message.
	// Using the uuid argument's 'invalid argument' message if the uuid is invalid.
	// Using the filter's 'invalid argument' message if the uuid is not accepted.

	/**
	 * Gets the default uuid completion suggestions.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param uuidPrefix
	 *            the uuid prefix, may be empty, not <code>null</code>
	 * @param shopkeeperFilter
	 *            only suggestions for shopkeepers accepted by this predicate get included
	 * @return the shopkeeper uuid completion suggestions
	 */
	public static Iterable<UUID> getDefaultCompletionSuggestions(	CommandInput input, CommandContextView context,
																	String uuidPrefix, Predicate<Shopkeeper> shopkeeperFilter) {
		String normalizedUUIDPrefix = uuidPrefix.toLowerCase(Locale.ROOT);
		// TODO Improve by using a TreeMap for the prefix matching?
		return ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().stream()
				.filter(shopkeeperFilter)
				.map(shopkeeper -> shopkeeper.getUniqueId())
				.filter(uuid -> {
					// Assumption: UUID#toString is already lowercase (normalized)
					return uuid.toString().startsWith(normalizedUUIDPrefix);
				})::iterator;
	}

	@Override
	protected Iterable<UUID> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix) {
		return getDefaultCompletionSuggestions(input, context, idPrefix, (shopkeeper) -> true);
	}
}
