package com.nisovin.shopkeepers.commands.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.nisovin.shopkeepers.Messages;
import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.StringUtils;

public class ShopTypeArgument extends CommandArgument<ShopType<?>> {

	public ShopTypeArgument(String name) {
		super(name);
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Messages.commandShopTypeArgumentInvalid;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		return text;
	}

	@Override
	public ShopType<?> parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		String argument = argsReader.next();
		ShopType<?> value = ShopkeepersPlugin.getInstance().getShopTypeRegistry().match(argument);
		if (value == null) {
			throw this.invalidArgumentError(argument);
		}
		return value;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		if (argsReader.getRemainingSize() != 1) {
			return Collections.emptyList();
		}

		List<String> suggestions = new ArrayList<>();
		String partialArg = StringUtils.normalize(argsReader.next());
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
}
