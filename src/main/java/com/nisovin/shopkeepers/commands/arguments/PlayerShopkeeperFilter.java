package com.nisovin.shopkeepers.commands.arguments;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ArgumentFilter;
import com.nisovin.shopkeepers.util.TextUtils;

public class PlayerShopkeeperFilter implements ArgumentFilter<Shopkeeper> {

	@Override
	public boolean test(Shopkeeper shopkeeper) {
		return (shopkeeper instanceof PlayerShopkeeper);
	}

	@Override
	public String getInvalidArgumentErrorMsg(CommandArgument argument, String input, Shopkeeper value) {
		if (input == null) input = "";
		return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentNoPlayerShop,
				"{argumentName}", argument.getName(),
				"{argumentFormat}", argument.getFormat(),
				"{argument}", input);
	}
}
