package com.nisovin.shopkeepers.command.shopkeepers.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.command.lib.ArgumentParseException;
import com.nisovin.shopkeepers.command.lib.CommandArgs;
import com.nisovin.shopkeepers.command.lib.CommandArgument;
import com.nisovin.shopkeepers.command.lib.CommandContext;
import com.nisovin.shopkeepers.command.lib.CommandInput;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.Utils;

public class ShopTypeArgument extends CommandArgument {

	public ShopTypeArgument(String name) {
		super(name);
	}

	@Override
	public String getInvalidArgumentErrorMsg(String argument) {
		if (argument == null) argument = "";
		return Utils.replaceArgs(Settings.msgCommandShopTypeArgumentInvalid,
				"{argumentName}", this.getName(),
				"{argumentFormat}", this.getFormat(),
				"{argument}", argument);
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgument();
		}
		String argument = args.next();
		ShopType<?> value = ShopkeepersPlugin.getInstance().getShopTypeRegistry().match(argument);
		if (value == null) {
			throw this.invalidArgument(argument);
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		if (args.getRemainingSize() == 1) {
			List<String> suggestions = new ArrayList<>();
			String partialArg = StringUtils.normalize(args.next());
			for (ShopType<?> shopType : ShopkeepersPlugin.getInstance().getShopTypeRegistry().getRegisteredTypes()) {
				String identifier = shopType.getIdentifier();
				if (identifier.startsWith(partialArg)) {
					suggestions.add(identifier);
				}
			}
			return Collections.unmodifiableList(suggestions);
		}
		return Collections.emptyList();
	}
}
