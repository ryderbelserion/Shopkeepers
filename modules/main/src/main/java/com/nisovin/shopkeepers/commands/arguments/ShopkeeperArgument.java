package com.nisovin.shopkeepers.commands.arguments;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFirstOfArgument;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils;

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

	public ShopkeeperArgument(String name, ArgumentFilter<Shopkeeper> filter) {
		this(name, false, filter);
	}

	public ShopkeeperArgument(String name, boolean joinRemainingArgs, ArgumentFilter<Shopkeeper> filter) {
		this(name, joinRemainingArgs, filter, ShopkeeperNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT, ShopkeeperUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperArgument(String name, boolean joinRemainingArgs, ArgumentFilter<Shopkeeper> filter, int minimalNameCompletionInput, int minimalUUIDCompletionInput) {
		super(name);
		this.shopUUIDArgument = new ShopkeeperByUUIDArgument(name + ":uuid", filter, minimalUUIDCompletionInput);
		this.shopIdArgument = new ShopkeeperByIdArgument(name + ":id", filter);
		this.shopNameArgument = new ShopkeeperByNameArgument(name + ":name", joinRemainingArgs, filter, minimalNameCompletionInput) {
			@Override
			public Shopkeeper getObject(CommandInput input, CommandContextView context, String nameInput) throws ArgumentParseException {
				return ShopkeeperArgument.this.getShopkeeper(nameInput);
			}
		};
		this.firstOfArgument = new TypedFirstOfArgument<>(name + ":firstOf", Arrays.asList(shopUUIDArgument, shopIdArgument, shopNameArgument), false, false);
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
	public Shopkeeper getShopkeeper(String nameInput) throws IllegalArgumentException {
		Stream<? extends Shopkeeper> shopkeepers = ShopkeeperArgumentUtils.ShopkeeperNameMatchers.DEFAULT.match(nameInput);
		Optional<? extends Shopkeeper> shopkeeper = shopkeepers.findFirst();
		return shopkeeper.orElse(null);
		// TODO deal with ambiguities
	}

	@Override
	public Shopkeeper parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// Also handles argument exceptions:
		return firstOfArgument.parseValue(input, context, argsReader);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return firstOfArgument.complete(input, context, argsReader);
	}
}
