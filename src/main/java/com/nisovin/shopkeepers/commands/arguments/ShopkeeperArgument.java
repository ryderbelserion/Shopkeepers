package com.nisovin.shopkeepers.commands.arguments;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.FirstOfArgument;
import com.nisovin.shopkeepers.util.Pair;

public class ShopkeeperArgument extends CommandArgument<Shopkeeper> {

	private final ShopkeeperByUUIDArgument shopUUIDArgument;
	private final ShopkeeperByIdArgument shopIdArgument;
	private final ShopkeeperByNameArgument shopNameArgument;
	private final FirstOfArgument firstOfArgument;

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
		this.shopUUIDArgument = new ShopkeeperByUUIDArgument(name, filter, minimalUUIDCompletionInput);
		this.shopIdArgument = new ShopkeeperByIdArgument(name, filter);
		this.shopNameArgument = new ShopkeeperByNameArgument(name, joinRemainingArgs, filter, minimalNameCompletionInput) {
			@Override
			public Shopkeeper matchShopkeeper(String nameInput) {
				return ShopkeeperArgument.this.matchShopkeeper(nameInput);
			}
		};
		this.firstOfArgument = new FirstOfArgument(name, Arrays.asList(shopUUIDArgument, shopIdArgument, shopNameArgument), false, false);
	}

	/**
	 * Gets a shopkeeper which matches the given name input.
	 * <p>
	 * This can be overridden if a different behavior is required.
	 * 
	 * @param nameInput
	 *            the raw name input
	 * @return the matched shopkeeper, or <code>null</code>
	 */
	public Shopkeeper matchShopkeeper(String nameInput) {
		return ShopkeeperNameArgument.ShopkeeperNameMatchers.DEFAULT.match(nameInput);
	}

	@Override
	public Shopkeeper parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		// also handles argument exceptions:
		Pair<CommandArgument<?>, Object> result = firstOfArgument.parseValue(input, args);
		return (result == null) ? null : (Shopkeeper) result.getSecond();
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return firstOfArgument.complete(input, context, args);
	}
}
