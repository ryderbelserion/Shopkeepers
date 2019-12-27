package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;
import java.util.function.Predicate;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.IntegerArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.util.ConversionUtils;

/**
 * Provides suggestions for the ids of existing shopkeepers.
 * <p>
 * By default this accepts any id regardless of whether it corresponds to an existing shopkeeper.
 */
public class ShopkeeperIdArgument extends ObjectIdArgument<Integer> {

	// we don't show suggestions for empty input (would simply list a bunch of random ids)
	public static final int DEFAULT_MINIMAL_COMPLETION_INPUT = 1;

	// Note: Not providing default argument filters that only accept existing shops, admin shops, or player shops,
	// because this can be achieved more efficiently by using ShopkeeperByIdArgument instead.

	public ShopkeeperIdArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperIdArgument(String name, ArgumentFilter<Integer> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperIdArgument(String name, ArgumentFilter<Integer> filter, int minimalCompletionInput) {
		super(name, new IntegerArgument(name + ":id"), filter, minimalCompletionInput);
	}

	// using the regular 'missing argument' message
	// using the integer argument's 'invalid argument' message if the id is invalid
	// using the filter's 'invalid argument' message if the id is not accepted

	@Override
	protected String toString(Integer id) {
		return id.toString();
	}

	/**
	 * Gets the default id completion suggestions.
	 * 
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @param shopkeeperFilter
	 *            only suggestions for shopkeepers accepted by this predicate get included
	 * @return the shopkeeper id completion suggestions
	 */
	public static Iterable<Integer> getDefaultCompletionSuggestions(String idPrefix, Predicate<Shopkeeper> shopkeeperFilter) {
		// if idPrefix is not a valid number, we can skip checking all shopkeepers:
		// note: empty check is required to not abort in case there is empty partial input but the used
		// minimalCompletionInput parameter is 0
		if (!idPrefix.isEmpty() && ConversionUtils.parseInt(idPrefix) == null) {
			return Collections.emptyList();
		}
		// note: no normalization required
		// TODO prefer short ids (eg. input "2", suggest "20", "21", "22",.. instead of "200", "201", "202",..)
		return ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().stream()
				.filter(shopkeeperFilter)
				.map(shopkeeper -> shopkeeper.getId())
				.filter(id -> id.toString().startsWith(idPrefix))::iterator;
	}

	@Override
	protected Iterable<Integer> getCompletionSuggestions(String idPrefix) {
		return getDefaultCompletionSuggestions(idPrefix, (shopkeeper) -> true);
	}
}
