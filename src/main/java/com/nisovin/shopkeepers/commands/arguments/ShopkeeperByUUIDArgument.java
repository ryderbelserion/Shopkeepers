package com.nisovin.shopkeepers.commands.arguments;

import java.util.List;
import java.util.UUID;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;

/**
 * Determines a shopkeeper by the given UUID input.
 */
public class ShopkeeperByUUIDArgument extends CommandArgument<Shopkeeper> {

	private final ShopkeeperUUIDArgument shopkeeperUUIDArgument;
	private final ArgumentFilter<Shopkeeper> filter; // not null

	public ShopkeeperByUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperByUUIDArgument(String name, ArgumentFilter<Shopkeeper> filter) {
		this(name, filter, ShopkeeperUUIDArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperByUUIDArgument(String name, ArgumentFilter<Shopkeeper> filter, int minimalCompletionInput) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		// only accepting uuids of existing and accepted players:
		ArgumentFilter<UUID> uuidFilter = new ArgumentFilter<UUID>() {
			@Override
			public boolean test(UUID uuid) {
				Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(uuid);
				return shopkeeper != null && ShopkeeperByUUIDArgument.this.filter.test(shopkeeper);
			}

			@Override
			public String getInvalidArgumentErrorMsg(CommandArgument<UUID> argument, String argumentInput, UUID value) {
				Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(value);
				if (shopkeeper == null) {
					return ShopkeeperUUIDArgument.ACCEPT_EXISTING_SHOPS.getInvalidArgumentErrorMsg(argument, argumentInput, value);
				} else {
					return ShopkeeperByUUIDArgument.this.filter.getInvalidArgumentErrorMsg(ShopkeeperByUUIDArgument.this, argumentInput, shopkeeper);
				}
			}
		};
		this.shopkeeperUUIDArgument = new ShopkeeperUUIDArgument(name + ":uuid", uuidFilter, minimalCompletionInput);
		shopkeeperUUIDArgument.setParent(this);
	}

	@Override
	public Shopkeeper parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// exceptions (and messages) are handled by the shopkeeper-uuid argument
		UUID uuid = shopkeeperUUIDArgument.parseValue(input, context, argsReader);
		Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(uuid);
		assert shopkeeper != null; // already checked by the shopkeeper-uuid argument
		return shopkeeper;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return shopkeeperUUIDArgument.complete(input, context, argsReader);
	}
}
