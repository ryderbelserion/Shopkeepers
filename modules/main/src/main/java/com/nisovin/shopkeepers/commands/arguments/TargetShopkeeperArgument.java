package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;
import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeeperFilter;
import com.nisovin.shopkeepers.commands.util.ShopkeeperArgumentUtils.TargetShopkeepersResult;

/**
 * A {@link CommandArgument} that returns the targeted shopkeeper without consuming any arguments.
 * <p>
 * If the sender is not a player, a 'requires a player' error message is thrown. If no shopkeeper is targeted or the
 * targeted shopkeeper is not accepted, the filter's corresponding error message is used.
 */
public class TargetShopkeeperArgument extends CommandArgument<Shopkeeper> {

	private final TargetShopkeeperFilter filter; // not null

	public TargetShopkeeperArgument(String name) {
		this(name, TargetShopkeeperFilter.ANY);
	}

	public TargetShopkeeperArgument(String name, TargetShopkeeperFilter filter) {
		super(name);
		this.filter = (filter == null) ? TargetShopkeeperFilter.ANY : filter;
	}

	@Override
	public boolean isOptional() {
		return true; // Does not require user input
	}

	@Override
	public Shopkeeper parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		CommandSender sender = input.getSender();
		if (!(sender instanceof Player)) {
			throw this.requiresPlayerError();
		}

		Player player = (Player) sender;
		TargetShopkeepersResult result = ShopkeeperArgumentUtils.getTargetedShopkeepers(player, filter);
		if (!result.isSuccess()) {
			throw new ArgumentParseException(this, result.getErrorMessage());
		} else {
			assert !result.getShopkeepers().isEmpty();
			// TODO Print an error if result is ambiguous?
			return result.getShopkeepers().get(0);
		}
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return Collections.emptyList();
	}
}
