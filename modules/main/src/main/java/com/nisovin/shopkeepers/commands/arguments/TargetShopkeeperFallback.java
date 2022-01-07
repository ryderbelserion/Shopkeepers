package com.nisovin.shopkeepers.commands.arguments;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.fallback.FallbackArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFallbackArgument;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link FallbackArgument} that returns the targeted shopkeeper without consuming any arguments.
 * <p>
 * If the sender is not a player, the parsing exception of the original argument is thrown (the original argument might
 * get reevaluated is some parsing context has changed).
 */
public class TargetShopkeeperFallback extends TypedFallbackArgument<Shopkeeper> {

	public TargetShopkeeperFallback(CommandArgument<Shopkeeper> argument) {
		this(argument, TargetShopkeeperFilter.ANY);
	}

	public TargetShopkeeperFallback(CommandArgument<Shopkeeper> argument, TargetShopkeeperFilter filter) {
		super(argument, new TargetShopkeeperArgument(Validate.notNull(argument, "argument is null").getName(), filter));
	}
}
