package com.nisovin.shopkeepers.command.shopkeepers.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Predicate;

import org.bukkit.ChatColor;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperRegistry;
import com.nisovin.shopkeepers.command.lib.ArgumentParseException;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandInput;
import com.nisovin.shopkeepers.command.lib.arguments.StringArgument;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class ShopkeeperArgument extends StringArgument {

	private static final int MAX_SUGGESTIONS = 30;

	private final Predicate<Shopkeeper> filter; // can be null

	public ShopkeeperArgument(String name) {
		this(name, false, null);
	}

	public ShopkeeperArgument(String name, boolean joinRemainingArgs) {
		this(name, joinRemainingArgs, null);
	}

	public ShopkeeperArgument(String name, Predicate<Shopkeeper> filter) {
		this(name, false, filter);
	}

	public ShopkeeperArgument(String name, boolean joinRemainingArgs, Predicate<Shopkeeper> filter) {
		super(name, joinRemainingArgs);
		this.filter = filter;
	}

	@Override
	public String getInvalidArgumentErrorMsg(String argument) {
		if (argument == null) argument = "";
		return Utils.replaceArgs(Settings.msgCommandShopkeeperArgumentInvalid,
				"{argumentName}", this.getName(),
				"{argumentFormat}", this.getFormat(),
				"{argument}", argument);
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		String shopkeeperArg = (String) super.parseValue(input, args);

		ShopkeeperRegistry shopkeeperRegistry = ShopkeepersAPI.getShopkeeperRegistry();
		Shopkeeper shopkeeper = null;
		if (shopkeeperArg != null) {
			// check if the argument is an uuid:
			UUID shopUniqueId = null;
			try {
				shopUniqueId = UUID.fromString(shopkeeperArg);
			} catch (IllegalArgumentException e) {
				// invalid uuid
			}
			if (shopUniqueId != null) {
				shopkeeper = shopkeeperRegistry.getShopkeeperByUniqueId(shopUniqueId);
			}
			shopkeeper = this.applyFilter(shopkeeper);
			if (shopkeeper != null) return shopkeeper;

			// check if the argument is an integer:
			int shopId = -1;
			try {
				shopId = Integer.parseInt(shopkeeperArg);
			} catch (NumberFormatException e) {
				// invalid integer
			}
			if (shopId != -1) {
				shopkeeper = shopkeeperRegistry.getShopkeeperById(shopId);
			}
			shopkeeper = this.applyFilter(shopkeeper);
			if (shopkeeper != null) return shopkeeper;

			// try to get shopkeeper by name:
			shopkeeper = shopkeeperRegistry.getShopkeeperByName(shopkeeperArg);
			shopkeeper = this.applyFilter(shopkeeper);
			if (shopkeeper != null) return shopkeeper;
		}
		assert shopkeeper == null;
		throw this.invalidArgument(shopkeeperArg);
	}

	// returnes null if filtered
	private Shopkeeper applyFilter(Shopkeeper shopkeeper) {
		return ((filter == null || filter.test(shopkeeper)) ? shopkeeper : null);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		if (args.getRemainingSize() == 1 || (joinRemainingArgs && args.getRemainingSize() > 1)) {
			List<String> suggestions = new ArrayList<>();
			String partialArg = (joinRemainingArgs ? this.getJoinedRemainingArgs(args) : args.next()).toLowerCase();

			// check for matching shop names:
			for (Shopkeeper shopkeeper : ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers()) {
				if (filter != null && !filter.test(shopkeeper)) continue; // filtered
				String shopName = ChatColor.stripColor(shopkeeper.getName());
				if (!StringUtils.isEmpty(shopName) && shopName.toLowerCase().startsWith(partialArg)) {
					// TODO only add the part of the name past the matching parts as suggestion (in case of joined
					// remaining args)
					suggestions.add(shopName);
					if (suggestions.size() >= MAX_SUGGESTIONS) break; // suggestions limit reached
				}
			}

			if (!partialArg.isEmpty()) {
				// check for matching ids:
				if (suggestions.size() < MAX_SUGGESTIONS) {
					for (Shopkeeper shopkeeper : ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers()) {
						if (filter != null && !filter.test(shopkeeper)) continue; // filtered
						String shopId = String.valueOf(shopkeeper.getId());
						if (shopId.startsWith(partialArg)) {
							suggestions.add(shopId);
							if (suggestions.size() >= MAX_SUGGESTIONS) break; // suggestions limit reached
						}
					}
				}

				// check for matching unique ids:
				if (suggestions.size() < MAX_SUGGESTIONS) {
					for (Shopkeeper shopkeeper : ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers()) {
						if (filter != null && !filter.test(shopkeeper)) continue; // filtered
						String shopUniqueId = shopkeeper.getUniqueId().toString();
						if (shopUniqueId.startsWith(partialArg)) {
							suggestions.add(shopUniqueId);
							if (suggestions.size() >= MAX_SUGGESTIONS) break; // suggestions limit reached
						}
					}
				}
			}
			return Collections.unmodifiableList(suggestions);
		}
		return Collections.emptyList();
	}
}
