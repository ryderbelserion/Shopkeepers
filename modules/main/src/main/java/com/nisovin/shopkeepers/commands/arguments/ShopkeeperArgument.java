package com.nisovin.shopkeepers.commands.arguments;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFirstOfArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.ShopkeeperNameMatchers;
import com.nisovin.shopkeepers.util.java.CollectionUtils;

public class ShopkeeperArgument extends CommandArgument<Shopkeeper> {

	private final ShopkeeperByUUIDArgument shopUUIDArgument;
	private final ShopkeeperByIdArgument shopIdArgument;
	private final ShopkeeperByNameArgument shopNameArgument;
	private final TypedFirstOfArgument<Shopkeeper> firstOfArgument;

	public ShopkeeperArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperArgument(String name, boolean joinRemainingArgs) {
		this(name, joinRemainingArgs, ArgumentFilter.acceptAny());
	}

	public ShopkeeperArgument(String name, ArgumentFilter<? super Shopkeeper> filter) {
		this(name, false, filter);
	}

	public ShopkeeperArgument(
			String name,
			boolean joinRemainingArgs,
			ArgumentFilter<? super Shopkeeper> filter
	) {
		this(
				name,
				joinRemainingArgs,
				filter,
				ShopkeeperNameArgument.DEFAULT_MINIMUM_COMPLETION_INPUT,
				ShopkeeperUUIDArgument.DEFAULT_MINIMUM_COMPLETION_INPUT
		);
	}

	public ShopkeeperArgument(
			String name,
			boolean joinRemainingArgs,
			ArgumentFilter<? super Shopkeeper> filter,
			int minimumNameCompletionInput,
			int minimumUUIDCompletionInput
	) {
		super(name);
		this.shopUUIDArgument = new ShopkeeperByUUIDArgument(
				name + ":uuid",
				filter,
				minimumUUIDCompletionInput
		);
		this.shopIdArgument = new ShopkeeperByIdArgument(name + ":id", filter);
		this.shopNameArgument = new ShopkeeperByNameArgument(
				name + ":name",
				joinRemainingArgs,
				filter,
				minimumNameCompletionInput
		) {
			@Override
			protected @Nullable Shopkeeper getObject(
					CommandInput input,
					CommandContextView context,
					String nameInput
			) throws ArgumentParseException {
				return ShopkeeperArgument.this.getShopkeeper(nameInput);
			}
		};
		this.firstOfArgument = new TypedFirstOfArgument<>(
				name + ":firstOf",
				Arrays.asList(shopUUIDArgument, shopIdArgument, shopNameArgument),
				false,
				false
		);
		firstOfArgument.setParent(this);
	}

	/**
	 * Gets a shopkeeper which matches the given name input.
	 * <p>
	 * This can be overridden if a different behavior is required.
	 * 
	 * @param nameInput
	 *            the raw name input
	 * @return the matched shopkeeper, or <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the id is ambiguous
	 */
	public @Nullable Shopkeeper getShopkeeper(String nameInput) throws IllegalArgumentException {
		Stream<? extends Shopkeeper> shopkeepers = ShopkeeperNameMatchers.DEFAULT.match(nameInput);
		return CollectionUtils.getFirstOrNull(shopkeepers);
		// TODO deal with ambiguities
	}

	@Override
	public Shopkeeper parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		// Also handles argument exceptions:
		return firstOfArgument.parseValue(input, context, argsReader);
	}

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return firstOfArgument.complete(input, context, argsReader);
	}
}
