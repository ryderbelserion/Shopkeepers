package com.nisovin.shopkeepers.commands.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.nisovin.shopkeepers.Settings;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.StringUtils;
import com.nisovin.shopkeepers.util.TextUtils;

public class ShopTypeArgument extends CommandArgument<ShopType<?>> {

	public ShopTypeArgument(String name) {
		super(name);
	}

	@Override
	public String getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		String[] defaultArgs = this.getDefaultErrorMsgArgs();
		return TextUtils.replaceArgs(Settings.msgCommandShopTypeArgumentInvalid,
				defaultArgs, "{argument}", argumentInput);
	}

	@Override
	public ShopType<?> parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgumentError();
		}
		String argument = args.next();
		ShopType<?> value = ShopkeepersPlugin.getInstance().getShopTypeRegistry().match(argument);
		if (value == null) {
			throw this.invalidArgumentError(argument);
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		if (args.getRemainingSize() == 1) {
			List<String> suggestions = new ArrayList<>();
			String partialArg = StringUtils.normalize(args.next());
			for (ShopType<?> shopType : ShopkeepersPlugin.getInstance().getShopTypeRegistry().getRegisteredTypes()) {
				if (suggestions.size() >= MAX_SUGGESTIONS) break;
				String displayName = shopType.getDisplayName();
				displayName = StringUtils.normalizeKeepCase(displayName);
				String displayNameNorm = displayName.toLowerCase(Locale.ROOT);
				if (displayNameNorm.startsWith(partialArg)) {
					suggestions.add(displayName);
				} else {
					String identifier = shopType.getIdentifier();
					if (identifier.startsWith(partialArg)) {
						suggestions.add(identifier);
					}
				}
			}
			return Collections.unmodifiableList(suggestions);
		}
		return Collections.emptyList();
	}
}
