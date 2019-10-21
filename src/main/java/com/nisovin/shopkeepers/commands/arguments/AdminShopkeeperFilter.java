package com.nisovin.shopkeepers.commands.arguments;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.util.TextUtils;

public class AdminShopkeeperFilter implements ArgumentFilter<Shopkeeper> {

	public static final AdminShopkeeperFilter INSTANCE = new AdminShopkeeperFilter();

	@Override
	public boolean test(Shopkeeper shopkeeper) {
		return (shopkeeper instanceof AdminShopkeeper);
	}

	@Override
	public String getInvalidArgumentErrorMsg(CommandArgument<Shopkeeper> argument, String argumentInput, Shopkeeper value) {
		if (argumentInput == null) argumentInput = "";
		String[] defaultArgs = argument.getDefaultErrorMsgArgs();
		return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentNoAdminShop,
				defaultArgs, "{argument}", argumentInput);
	}
}
