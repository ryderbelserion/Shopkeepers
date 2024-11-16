package com.nisovin.shopkeepers.commands.arguments;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Stream;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectUUIDArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;

/**
 * Provides suggestions for the UUIDs of existing shopkeepers.
 * <p>
 * By default this accepts any UUID regardless of whether it corresponds to an existing shopkeeper.
 */
public class ShopkeeperUUIDArgument extends ObjectUUIDArgument {

	public static final int DEFAULT_MINIMUM_COMPLETION_INPUT = ObjectUUIDArgument.DEFAULT_MINIMUM_COMPLETION_INPUT;

	// Note: Not providing default argument filters that only accept existing shops, admin shops, or
	// player shops, because this can be achieved more efficiently by using ShopkeeperByUUIDArgument
	// instead.

	public ShopkeeperUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperUUIDArgument(String name, ArgumentFilter<? super UUID> filter) {
		this(name, filter, DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ShopkeeperUUIDArgument(
			String name,
			ArgumentFilter<? super UUID> filter,
			int minimumCompletionInput
	) {
		super(name, filter, minimumCompletionInput);
	}

	// Using the regular 'missing argument' message.
	// Using the uuid argument's 'invalid argument' message if the uuid is invalid.
	// Using the filter's 'invalid argument' message if the uuid is not accepted.

	/**
	 * Gets the default uuid completion suggestions.
	 * <p>
	 * This always suggests the uuid of the targeted shopkeeper(s), regardless of the
	 * {@code minimumCompletionInput} argument.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param minimumCompletionInput
	 *            the minimum input length before completion suggestions are provided
	 * @param uuidPrefix
	 *            the uuid prefix, may be empty, not <code>null</code>
	 * @param filter
	 *            only suggestions for shopkeepers accepted by this filter are included, not
	 *            <code>null</code>
	 * @return the shopkeeper uuid completion suggestions
	 */
	public static Iterable<? extends UUID> getDefaultCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			int minimumCompletionInput,
			String uuidPrefix,
			ArgumentFilter<? super Shopkeeper> filter
	) {
		String normalizedUUIDPrefix = uuidPrefix.toLowerCase(Locale.ROOT);
		// Suggestion for the unique id(s) of the targeted shopkeeper(s):
		CommandSender sender = input.getSender();
		List<? extends Shopkeeper> targetedShopkeepers = ShopkeeperArgumentUtils.getTargetedShopkeepers(
				sender,
				TargetShopkeeperFilter.ANY
		);

		// Only provide other suggestions if there is a minimum length input:
		Stream<Shopkeeper> shopkeepersStream;
		if (uuidPrefix.length() >= minimumCompletionInput) {
			// TODO Improve by using a TreeMap for the prefix matching?
			shopkeepersStream = Stream.concat(
					targetedShopkeepers.stream(),
					ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().stream()
							.filter(shopkeeper -> !targetedShopkeepers.contains(shopkeeper))
			);
		} else {
			// TODO CheckerFramework complains when using a wildcard Stream here.
			shopkeepersStream = Unsafe.castNonNull(targetedShopkeepers.stream());
		}

		return shopkeepersStream
				.filter(shopkeeper -> filter.test(input, context, shopkeeper))
				.map(Shopkeeper::getUniqueId)
				.filter(uuid -> {
					// Assumption: UUID#toString is already lowercase (normalized)
					return uuid.toString().startsWith(normalizedUUIDPrefix);
				})::iterator;
	}

	@Override
	protected Iterable<? extends UUID> getCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			String idPrefix
	) {
		return getDefaultCompletionSuggestions(
				input,
				context,
				minimumCompletionInput,
				idPrefix,
				ArgumentFilter.acceptAny()
		);
	}
}
