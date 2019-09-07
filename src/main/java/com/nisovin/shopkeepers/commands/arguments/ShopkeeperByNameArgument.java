package com.nisovin.shopkeepers.commands.arguments;

import java.util.List;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.PlayerNameArgument;

/**
 * Determines a shopkeeper by the given name input.
 */
public class ShopkeeperByNameArgument extends CommandArgument<Shopkeeper> {

	private final ShopkeeperNameArgument shopkeeperNameArgument;
	private final ArgumentFilter<Shopkeeper> filter; // not null
	// avoid duplicate name lookups and name matching by keeping track of the matched shopkeeper:
	private boolean shopkeeperMatched = false;
	private Shopkeeper matchedShopkeeper = null;

	public ShopkeeperByNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperByNameArgument(String name, ArgumentFilter<Shopkeeper> filter) {
		this(name, false, filter, PlayerNameArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperByNameArgument(String name, boolean joinRemainingArgs, ArgumentFilter<Shopkeeper> filter, int minimalCompletionInput) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		// only accepting names of existing and accepted shopkeeper:
		ArgumentFilter<String> nameFilter = new ArgumentFilter<String>() {
			@Override
			public boolean test(String name) {
				Shopkeeper shopkeeper = getShopkeeperByName(name);
				return shopkeeper != null && ShopkeeperByNameArgument.this.filter.test(shopkeeper);
			}

			@Override
			public String getInvalidArgumentErrorMsg(CommandArgument<String> argument, String input, String value) {
				Shopkeeper shopkeeper = getShopkeeperByName(name);
				if (shopkeeper == null) {
					return ShopkeeperNameArgument.ACCEPT_EXISTING_SHOPS.getInvalidArgumentErrorMsg(argument, input, value);
				} else {
					return ShopkeeperByNameArgument.this.filter.getInvalidArgumentErrorMsg(ShopkeeperByNameArgument.this, input, shopkeeper);
				}
			}
		};
		// always match known names: we keep track of the matched shopkeeper to avoid duplicate name lookups later (eg.
		// for the filters)
		this.shopkeeperNameArgument = new ShopkeeperNameArgument(name, joinRemainingArgs, nameFilter, true, minimalCompletionInput) {
			@Override
			protected String matchKnownId(String input) {
				Shopkeeper shopkeeper = ShopkeeperByNameArgument.this.matchShopkeeper(input);
				// keep track of whether and which Shopkeeper got matched:
				shopkeeperMatched = true;
				matchedShopkeeper = shopkeeper;
				return (shopkeeper == null) ? input : shopkeeper.getName();
			}
		};
	}

	private Shopkeeper getShopkeeperByName(String name) {
		// use the cached shopkeeper if available:
		if (shopkeeperMatched) {
			return matchedShopkeeper; // can be null if no shopkeeper matched the name
		} else {
			// lookup by name:
			return ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByName(name);
		}
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
		try {
			// exceptions (and messages) are handled by the shopkeeper-name argument
			String name = shopkeeperNameArgument.parseValue(input, args);
			// we found a matching and accepted shopkeeper, otherwise the name argument and its filters would have
			// thrown an exception
			assert shopkeeperMatched && matchedShopkeeper != null && matchedShopkeeper.getName().equals(name) && filter.test(matchedShopkeeper);
			return matchedShopkeeper;
		} finally {
			// reset cached shopkeeper:
			shopkeeperMatched = false;
			matchedShopkeeper = null;
		}
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return shopkeeperNameArgument.complete(input, context, args);
	}
}
