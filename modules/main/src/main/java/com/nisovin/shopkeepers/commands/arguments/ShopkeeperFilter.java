package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

public final class ShopkeeperFilter {

	public static final ArgumentFilter<Shopkeeper> ANY = ArgumentFilter.acceptAny();

	public static final ArgumentFilter<@Nullable Shopkeeper> ADMIN = new ArgumentFilter<@Nullable Shopkeeper>() {
		@Override
		public boolean test(@Nullable Shopkeeper shopkeeper) {
			return (shopkeeper instanceof AdminShopkeeper);
		}

		@Override
		public Text getInvalidArgumentErrorMsg(
				CommandArgument<?> argument,
				String argumentInput,
				@Nullable Shopkeeper value
		) {
			Validate.notNull(argumentInput, "argumentInput is null");
			Text text = Messages.commandShopkeeperArgumentNoAdminShop;
			text.setPlaceholderArguments(argument.getDefaultErrorMsgArgs());
			text.setPlaceholderArguments("argument", argumentInput);
			return text;
		}
	};

	public static final ArgumentFilter<@Nullable Shopkeeper> PLAYER = new ArgumentFilter<@Nullable Shopkeeper>() {
		@Override
		public boolean test(@Nullable Shopkeeper shopkeeper) {
			return (shopkeeper instanceof PlayerShopkeeper);
		}

		@Override
		public Text getInvalidArgumentErrorMsg(
				CommandArgument<?> argument,
				String argumentInput,
				@Nullable Shopkeeper value
		) {
			Validate.notNull(argumentInput, "argumentInput is null");
			Text text = Messages.commandShopkeeperArgumentNoPlayerShop;
			text.setPlaceholderArguments(argument.getDefaultErrorMsgArgs());
			text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
			return text;
		}
	};

	private ShopkeeperFilter() {
	}
}
