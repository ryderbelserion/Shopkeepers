package com.nisovin.shopkeepers.commands.arguments;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.FallbackArgument;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.util.Validate;

/**
 * A {@link PlayerFallbackArgument} that returns the targeted shopkeeper without consuming any arguments.
 * <p>
 * If the sender is not a player, the parsing exception of the original argument is thrown (the original argument might
 * get reevaluated is some parsing context has changed).
 */
public class TargetShopkeeperFallback extends FallbackArgument<Shopkeeper> {

	public TargetShopkeeperFallback(CommandArgument<Shopkeeper> argument) {
		this(argument, TargetShopkeeperFilter.ANY);
	}

	public TargetShopkeeperFallback(CommandArgument<Shopkeeper> argument, TargetShopkeeperFilter filter) {
		super(argument, new TargetShopkeeperArgument(Validate.notNull(argument).getName(), filter));
	}
}
