package com.nisovin.shopkeepers.commands.arguments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentRejectedException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.IntegerArgument;
import com.nisovin.shopkeepers.util.ConversionUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public class ShopkeeperByIdArgument extends CommandArgument<Shopkeeper> {

	private final IntegerArgument idArgument;
	private final ArgumentFilter<Shopkeeper> filter; // not null

	public ShopkeeperByIdArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperByIdArgument(String name, ArgumentFilter<Shopkeeper> filter) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		this.idArgument = new IntegerArgument(name + ":id");
		idArgument.setParent(this);
	}

	@Override
	public String getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		String[] defaultArgs = this.getDefaultErrorMsgArgs();
		return TextUtils.replaceArgs(Settings.msgCommandShopkeeperArgumentInvalid,
				defaultArgs, "{argument}", argumentInput);
	}

	@Override
	public Shopkeeper parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// throws an exception if the argument is missing or not a valid integer
		Integer shopId = idArgument.parseValue(input, context, argsReader);
		Shopkeeper shopkeeper = ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperById(shopId);
		if (shopkeeper == null) {
			throw this.invalidArgumentError(argsReader.current());
		} else if (!filter.test(shopkeeper)) {
			throw new ArgumentRejectedException(this, filter.getInvalidArgumentErrorMsg(this, argsReader.current(), shopkeeper));
		} else {
			return shopkeeper;
		}
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		if (argsReader.getRemainingSize() != 1) {
			return Collections.emptyList();
		}

		List<String> suggestions = new ArrayList<>();
		String partialArg = argsReader.next();
		if (ConversionUtils.parseInt(partialArg) != null) {
			// assert !partialArg.isEmpty() && partialArg is valid integer
			// check for matching ids:
			// TODO prefer short ids (eg. input "2", suggest "20", "21", "22",.. instead of "200", "201", "202",..)
			Collection<? extends Shopkeeper> allShopkeepers = ShopkeepersAPI.getShopkeeperRegistry().getAllShopkeepers();
			for (Shopkeeper shopkeeper : allShopkeepers) {
				if (suggestions.size() >= MAX_SUGGESTIONS) break;
				if (!filter.test(shopkeeper)) continue; // filtered
				String shopId = String.valueOf(shopkeeper.getId());
				if (shopId.startsWith(partialArg)) {
					suggestions.add(shopId);
				}
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
