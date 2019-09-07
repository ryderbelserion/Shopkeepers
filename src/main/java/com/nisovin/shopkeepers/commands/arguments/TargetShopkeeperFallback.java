package com.nisovin.shopkeepers.commands.arguments;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;
import com.nisovin.shopkeepers.commands.lib.arguments.FallbackArgument;
import com.nisovin.shopkeepers.util.ShopkeeperUtils;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.util.ShopkeeperUtils.TargetShopkeepersResult;

/**
 * A {@link FallbackArgument} that returns the targeted shopkeeper.
 * <p>
 * If there are unparsed remaining arguments, the original parsing exception is thrown.
 * <p>
 * If the sender is no player, a 'missing argument' exception is thrown (since it is assumed that this is used as
 * fallback for a shopkeeper argument).
 * <p>
 * If no shopkeeper is targeted, or the targeted shopkeeper is not accepted, an appropriate error message is used.
 */
public class TargetShopkeeperFallback extends FallbackArgument<Shopkeeper> {

	private final TargetShopkeeperFilter filter; // not null

	public TargetShopkeeperFallback(CommandArgument<Shopkeeper> argument, TargetShopkeeperFilter filter) {
		super(argument);
		this.filter = (filter == null) ? TargetShopkeeperFilter.ANY : filter;
	}

	@Override
	public boolean hasNoArgFallback() {
		return true;
	}

	@Override
	public Shopkeeper parseFallbackValue(CommandInput input, CommandArgs args, FallbackArgumentException fallbackException) throws ArgumentParseException {
		String arg = args.nextIfPresent();
		if (arg != null) {
			throw fallbackException.getRootException();
		} else {
			CommandSender sender = input.getSender();
			if (!(sender instanceof Player)) {
				throw this.missingArgument();
			} else {
				Player player = (Player) sender;
				TargetShopkeepersResult result = ShopkeeperUtils.getTargetedShopkeepers(player, filter);
				if (!result.isSuccess()) {
					throw new ArgumentParseException(result.getErrorMessage());
				} else {
					assert !result.getShopkeepers().isEmpty();
					// TODO print an error if result is ambiguous?
					return result.getShopkeepers().get(0);
				}
			}
		}
	}
}
