package com.nisovin.shopkeepers.commands.arguments;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.util.TextUtils;

public class PlayerShopkeeperFilter implements ArgumentFilter<Shopkeeper> {

	public static final PlayerShopkeeperFilter INSTANCE = new PlayerShopkeeperFilter();

	@Override
	public boolean test(Shopkeeper shopkeeper) {
		return (shopkeeper instanceof PlayerShopkeeper);
	}

	@Override
	public String getInvalidArgumentErrorMsg(CommandArgument<Shopkeeper> argument, String input, Shopkeeper value) {
		if (input == null) input = "";
		return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentNoPlayerShop,
				"{argumentName}", argument.getRootArgument().getName(),
				"{argumentFormat}", argument.getRootArgument().getFormat(),
				"{argument}", input);
	}
}
