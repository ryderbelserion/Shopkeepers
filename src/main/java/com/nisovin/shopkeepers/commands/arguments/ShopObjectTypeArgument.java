package com.nisovin.shopkeepers.commands.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopobjects.ShopObjectType;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.StringUtils;

public class ShopObjectTypeArgument extends CommandArgument<ShopObjectType<?>> {

	public ShopObjectTypeArgument(String name) {
		super(name);
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Messages.commandShopObjectTypeArgumentInvalid;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		return text;
	}

	@Override
	public ShopObjectType<?> parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		String argument = argsReader.next();
		ShopObjectType<?> value = ShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().match(argument);
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
		for (ShopObjectType<?> shopObjectType : ShopkeepersPlugin.getInstance().getShopObjectTypeRegistry().getRegisteredTypes()) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			String displayName = shopObjectType.getDisplayName();
			displayName = StringUtils.normalizeKeepCase(displayName);
			String displayNameNorm = displayName.toLowerCase(Locale.ROOT);
			if (displayNameNorm.startsWith(partialArg)) {
				suggestions.add(displayName);
			} else {
				String identifier = shopObjectType.getIdentifier();
				if (identifier.startsWith(partialArg)) {
					suggestions.add(identifier);
				}
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
