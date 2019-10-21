package com.nisovin.shopkeepers.commands.arguments;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.util.TextUtils;

public final class ShopkeeperFilter {

	private ShopkeeperFilter() {
	}

	public static final ArgumentFilter<Shopkeeper> ANY = ArgumentFilter.acceptAny();

	public static final ArgumentFilter<Shopkeeper> ADMIN = new ArgumentFilter<Shopkeeper>() {
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
	};

	public static final ArgumentFilter<Shopkeeper> PLAYER = new ArgumentFilter<Shopkeeper>() {
		@Override
		public boolean test(Shopkeeper shopkeeper) {
			return (shopkeeper instanceof PlayerShopkeeper);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<Shopkeeper> argument, String argumentInput, Shopkeeper value) {
			if (argumentInput == null) argumentInput = "";
			String[] defaultArgs = argument.getDefaultErrorMsgArgs();
			return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentNoPlayerShop,
					defaultArgs, "{argument}", argumentInput);
		}
	};
}
