package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.bukkit.command.CommandSender;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.IntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.PredicateUtils;

/**
 * Provides suggestions for the ids of existing shopkeepers.
 * <p>
 * By default this accepts any id regardless of whether it corresponds to an existing shopkeeper.
 */
public class ShopkeeperIdArgument extends ObjectIdArgument<Integer> {

	// We don't show suggestions for empty input (would simply list a bunch of random ids).
	public static final int DEFAULT_MINIMUM_COMPLETION_INPUT = 1;

	// Note: Not providing default argument filters that only accept existing shops, admin shops, or player shops,
	// because this can be achieved more efficiently by using ShopkeeperByIdArgument instead.

	public ShopkeeperIdArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperIdArgument(String name, ArgumentFilter<Integer> filter) {
		this(name, filter, DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ShopkeeperIdArgument(String name, ArgumentFilter<Integer> filter, int minimumCompletionInput) {
		super(name, new IntegerArgument(name + ":id"), filter, minimumCompletionInput);
	}

	// Using the regular 'missing argument' message.
	// Using the integer argument's 'invalid argument' message if the id is invalid.
	// Using the filter's 'invalid argument' message if the id is not accepted.

	@Override
	protected String toString(Integer id) {
		return id.toString();
	}

	/**
	 * Gets the default id completion suggestions.
	 * <p>
	 * This always suggests the id of the targeted shopkeeper(s), regardless of the {@code minimumCompletionInput}
	 * argument.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param minimumCompletionInput
	 *            the minimum input length before completion suggestions are provided
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @param filter
	 *            only suggestions for shopkeepers accepted by this predicate are included, not <code>null</code>
	 * @return the shopkeeper id completion suggestions
	 */
	public static Iterable<Integer> getDefaultCompletionSuggestions(CommandInput input, CommandContextView context,
																	int minimumCompletionInput, String idPrefix,
																	Predicate<Shopkeeper> filter) {
		// If idPrefix is not empty but not a valid number, we can skip checking for completions:
		if (!idPrefix.isEmpty() && ConversionUtils.parseInt(idPrefix) == null) {
			return Collections.emptyList();
		}

		// Suggestion for the id(s) of the targeted shopkeeper(s):
		CommandSender sender = input.getSender();
		List<? extends Shopkeeper> targetedShopkeepers = ShopkeeperArgumentUtils.getTargetedShopkeepers(sender, TargetShopkeeperFilter.ANY);
		Stream<? extends Shopkeeper> shopkeepersStream = targetedShopkeepers.stream();

		// Only provide other suggestions if there is a minimum length input:
		if (idPrefix.length() >= minimumCompletionInput) {
			shopkeepersStream = Stream.concat(shopkeepersStream, ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().stream()
					.filter(shopkeeper -> !targetedShopkeepers.contains(shopkeeper)));
		}

		// Note: No normalization required.
		// TODO Prefer short ids (e.g. input "2", suggest "20", "21", "22",.. instead of "200", "201", "202",..)
		return shopkeepersStream
				.filter(filter)
				.map(Shopkeeper::getId)
				.filter(id -> id.toString().startsWith(idPrefix))::iterator;
	}

	@Override
	protected Iterable<Integer> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix) {
		return getDefaultCompletionSuggestions(input, context, minimumCompletionInput, idPrefix, PredicateUtils.alwaysTrue());
	}
}
