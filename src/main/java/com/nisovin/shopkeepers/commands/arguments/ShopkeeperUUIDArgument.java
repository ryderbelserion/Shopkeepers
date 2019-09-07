package com.nisovin.shopkeepers.commands.arguments;

import java.util.UUID;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectUUIDArgument;
import com.nisovin.shopkeepers.util.TextUtils;

/**
 * Provides suggestions for the UUIDs of existing shopkeepers.
 * <p>
 * By default this accepts any UUID regardless of whether it corresponds to an existing shopkeeper.
 */
public class ShopkeeperUUIDArgument extends ObjectUUIDArgument {

	public static final ArgumentFilter<UUID> ACCEPT_EXISTING_SHOPS = new ArgumentFilter<UUID>() {
		@Override
		public boolean test(UUID uuid) {
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(uuid);
			return (shopkeeper != null);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<UUID> argument, String input, UUID value) {
			if (input == null) input = "";
			return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentInvalid,
					"{argumentName}", argument.getName(),
					"{argumentFormat}", argument.getFormat(),
					"{argument}", input);
		}
	};

	public static final ArgumentFilter<UUID> ACCEPT_ADMIN_SHOPS = new ArgumentFilter<UUID>() {
		@Override
		public boolean test(UUID uuid) {
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(uuid);
			return (shopkeeper instanceof AdminShopkeeper);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<UUID> argument, String input, UUID value) {
			if (input == null) input = "";
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(value);
			if (shopkeeper == null) {
				return ACCEPT_EXISTING_SHOPS.getInvalidArgumentErrorMsg(argument, input, value);
			} else {
				return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentNoAdminShop,
						"{argumentName}", argument.getName(),
						"{argumentFormat}", argument.getFormat(),
						"{argument}", input);
			}
		}
	};

	public static final ArgumentFilter<UUID> ACCEPT_PLAYER_SHOPS = new ArgumentFilter<UUID>() {
		@Override
		public boolean test(UUID uuid) {
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(uuid);
			return (shopkeeper instanceof PlayerShopkeeper);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<UUID> argument, String input, UUID value) {
			if (input == null) input = "";
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByUniqueId(value);
			if (shopkeeper == null) {
				return ACCEPT_EXISTING_SHOPS.getInvalidArgumentErrorMsg(argument, input, value);
			} else {
				return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentNoPlayerShop,
						"{argumentName}", argument.getName(),
						"{argumentFormat}", argument.getFormat(),
						"{argument}", input);
			}
		}
	};

	public ShopkeeperUUIDArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperUUIDArgument(String name, ArgumentFilter<UUID> filter) {
		this(name, filter, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperUUIDArgument(String name, ArgumentFilter<UUID> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	// using regular 'missing argument' message
	// using the regular 'invalid argument' message if the uuid is invalid
	// using the filter's 'invalid shopkeeper' message if the uuid is not accepted

	@Override
	protected Iterable<UUID> getKnownIds() {
		return ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().stream().map((shopkeeper) -> {
			return shopkeeper.getUniqueId();
		})::iterator;
	}
}
