package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectNameArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.util.bukkit.TextUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

/**
 * By default, this accepts any String regardless of whether it corresponds to a known shopkeeper,
 * but provides suggestions for the names of known shopkeepers.
 */
public class ShopkeeperNameArgument extends ObjectNameArgument {

	public static final int DEFAULT_MINIMUM_COMPLETION_INPUT = ObjectNameArgument.DEFAULT_MINIMUM_COMPLETION_INPUT;

	// Note: Not providing default argument filters that only accept existing shops, admin shops, or
	// player shops, because this can be achieved more efficiently by using ShopkeeperByNameArgument
	// instead.

	public ShopkeeperNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperNameArgument(String name, ArgumentFilter<? super String> filter) {
		this(name, false, filter, DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ShopkeeperNameArgument(
			String name,
			boolean joinRemainingArgs,
			ArgumentFilter<? super String> filter,
			int minimumCompletionInput
	) {
		super(name, joinRemainingArgs, filter, minimumCompletionInput);
	}

	// Using the regular 'missing argument' message.
	// Using the filter's 'invalid argument' message if the name is not accepted.

	/**
	 * Gets the default name completion suggestions.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param minimumCompletionInput
	 *            the minimum input length before completion suggestions are provided
	 * @param namePrefix
	 *            the name prefix, may be empty, not <code>null</code>
	 * @param shopkeeperFilter
	 *            only suggestions for shopkeepers accepted by this filter are included
	 * @return the shopkeeper name completion suggestions
	 */
	public static Iterable<? extends String> getDefaultCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			int minimumCompletionInput,
			String namePrefix,
			ArgumentFilter<? super Shopkeeper> shopkeeperFilter
	) {
		// Only provide suggestions if there is a minimum length input:
		if (namePrefix.length() < minimumCompletionInput) {
			return Collections.emptyList();
		}

		// Strips color, normalizes whitespace, converts to lowercase:
		String normalizedNamePrefix = StringUtils.normalize(TextUtils.stripColor(namePrefix));
		// TODO Improve by using a TreeMap for the prefix matching?
		ShopkeeperRegistry shopkeeperRegistry = ShopkeepersAPI.getShopkeeperRegistry();
		// TODO CheckerFramework complains when using a wildcard Stream here.
		Stream<Shopkeeper> shopkeepers = Unsafe.castNonNull(shopkeeperRegistry.getAllShopkeepers().stream());
		Iterable<String> suggestions = shopkeepers
				.filter(shopkeeper -> shopkeeperFilter.test(input, context, shopkeeper))
				.<@Nullable String>map(shopkeeper -> {
					String name = TextUtils.stripColor(shopkeeper.getName());
					if (name.isEmpty()) return null;
					String normalizedWithCase = StringUtils.normalizeKeepCase(name);
					String normalized = normalizedWithCase.toLowerCase(Locale.ROOT);
					if (normalized.startsWith(normalizedNamePrefix)) {
						return normalizedWithCase;
					}
					return null; // No match
				}).filter(Objects::nonNull)
				.map(Unsafe::assertNonNull)::iterator;
		return suggestions;
	}

	@Override
	protected Iterable<? extends String> getCompletionSuggestions(
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
