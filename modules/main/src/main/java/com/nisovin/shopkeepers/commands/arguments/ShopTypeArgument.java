package com.nisovin.shopkeepers.commands.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.api.ShopkeepersPlugin;
import com.nisovin.shopkeepers.api.shopkeeper.ShopType;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.ObjectUtils;
import com.nisovin.shopkeepers.util.java.StringUtils;

public class ShopTypeArgument extends CommandArgument<ShopType<?>> {

	public ShopTypeArgument(String name) {
		super(name);
	}

	@Override
	protected Text getInvalidArgumentErrorMsgText() {
		return Messages.commandShopTypeArgumentInvalid;
	}

	@Override
	public ShopType<?> parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
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
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		if (argsReader.getRemainingSize() != 1) {
			return Collections.emptyList();
		}

		Player senderPlayer = ObjectUtils.castOrNull(input.getSender(), Player.class);
		List<String> suggestions = new ArrayList<>();
		String partialArg = StringUtils.normalize(argsReader.next());
		for (ShopType<?> shopType : ShopkeepersPlugin.getInstance().getShopTypeRegistry().getRegisteredTypes()) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			if (!shopType.isEnabled()) continue;
			if (senderPlayer != null && !shopType.hasPermission(senderPlayer)) continue;

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
