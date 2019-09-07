package com.nisovin.shopkeepers.commands.arguments;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.admin.AdminShopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.player.PlayerShopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectNameArgument;
import com.nisovin.shopkeepers.util.ObjectMatcher;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;

/**
 * By default this accepts any String regardless of whether it corresponds to a known shopkeeper, but provides
 * suggestions for the names of known shopkeepers.
 * <p>
 * If the option <code>matchKnownNames</code> is used and a known shopkeeper matches the input (according to the used
 * matching function), the shopkeeper's actual name will be returned instead of the used input.
 */
public class ShopkeeperNameArgument extends ObjectNameArgument {

	public static class ShopkeeperNameMatchers {

		private ShopkeeperNameMatchers() {
		}

		public static final ObjectMatcher<Shopkeeper> DEFAULT = new ObjectMatcher<Shopkeeper>() {
			@Override
			public Shopkeeper match(String input) {
				if (input == null || input.isEmpty()) return null;
				return ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByName(input);
			}
		};
	}

	public static final ArgumentFilter<String> ACCEPT_EXISTING_SHOPS = new ArgumentFilter<String>() {
		@Override
		public boolean test(String name) {
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByName(name);
			return (shopkeeper != null);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<String> argument, String input, String value) {
			if (input == null) input = "";
			return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentInvalid,
					"{argumentName}", argument.getName(),
					"{argumentFormat}", argument.getFormat(),
					"{argument}", input);
		}
	};

	public static final ArgumentFilter<String> ACCEPT_ADMIN_SHOPS = new ArgumentFilter<String>() {
		@Override
		public boolean test(String name) {
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByName(name);
			return (shopkeeper instanceof AdminShopkeeper);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<String> argument, String input, String value) {
			if (input == null) input = "";
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByName(value);
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

	public static final ArgumentFilter<String> ACCEPT_PLAYER_SHOPS = new ArgumentFilter<String>() {
		@Override
		public boolean test(String name) {
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByName(name);
			return (shopkeeper instanceof PlayerShopkeeper);
		}

		@Override
		public String getInvalidArgumentErrorMsg(CommandArgument<String> argument, String input, String value) {
			if (input == null) input = "";
			Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperByName(value);
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

	public ShopkeeperNameArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperNameArgument(String name, ArgumentFilter<String> filter) {
		this(name, filter, true);
	}

	public ShopkeeperNameArgument(String name, ArgumentFilter<String> filter, boolean matchKnownNames) {
		this(name, false, filter, matchKnownNames, DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperNameArgument(String name, boolean joinRemainingArgs, ArgumentFilter<String> filter, boolean matchKnownNames, int minimalCompletionInput) {
		super(name, joinRemainingArgs, filter, matchKnownNames, minimalCompletionInput);
	}

	// using regular 'missing argument' message
	// using the filter's 'invalid player' message if the name is not accepted

	// override this to limit which names get used for suggestions
	@Override
	protected Iterable<String> getKnownIds() {
		return ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers().stream().map((shopkeeper) -> {
			return shopkeeper.getName();
		}).filter(name -> !name.isEmpty())::iterator;
	}

	// This can be overridden if a different matching behavior is required.
	@Override
	protected String matchKnownId(String input) {
		Shopkeeper shopkeeper = ShopkeeperNameMatchers.DEFAULT.match(input);
		return (shopkeeper == null) ? input : shopkeeper.getName();
	}

	@Override
	protected String toString(String id) {
		// prepare shopkeeper name to be used as argument suggestion:
		id = TextUtils.stripColor(id);
		id = StringUtils.normalizeKeepCase(id);
		return id;
	}

	@Override
	protected String normalize(String idString) {
		// normalize shopkeeper name for comparison in commands:
		idString = this.toString(idString);
		return idString.toLowerCase(); // uses default Locale
	}
}
