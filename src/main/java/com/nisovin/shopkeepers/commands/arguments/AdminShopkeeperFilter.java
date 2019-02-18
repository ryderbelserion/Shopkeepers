package com.nisovin.shopkeepers.commands.arguments;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ArgumentFilter;
import com.nisovin.shopkeepers.util.Utils;

public class AdminShopkeeperFilter implements ArgumentFilter<Shopkeeper> {

	@Override
	public boolean test(Shopkeeper shopkeeper) {
		return (shopkeeper instanceof AdminShopkeeper);
	}

	@Override
	public String getInvalidArgumentErrorMsg(CommandArgument argument, String input, Shopkeeper value) {
		if (input == null) input = "";
		return Utils.replaceArgs(Settings.msgCommandShopkeeperArgumentNoAdminShop,
				"{argumentName}", argument.getName(),
				"{argumentFormat}", argument.getFormat(),
				"{argument}", input);
	}
}
