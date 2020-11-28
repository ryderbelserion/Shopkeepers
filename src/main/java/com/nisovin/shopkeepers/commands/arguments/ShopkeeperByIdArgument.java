package com.nisovin.shopkeepers.commands.arguments;

import java.util.Collections;

import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectByIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;

/**
 * Determines a shopkeeper by the given id input.
 */
public class ShopkeeperByIdArgument extends ObjectByIdArgument<Integer, Shopkeeper> {

	public ShopkeeperByIdArgument(String name) {
		this(name, ArgumentFilter.acceptAny());
	}

	public ShopkeeperByIdArgument(String name, ArgumentFilter<Shopkeeper> filter) {
		this(name, filter, ShopkeeperIdArgument.DEFAULT_MINIMAL_COMPLETION_INPUT);
	}

	public ShopkeeperByIdArgument(String name, ArgumentFilter<Shopkeeper> filter, int minimalCompletionInput) {
		super(name, filter, minimalCompletionInput);
	}

	@Override
	protected ObjectIdArgument<Integer> createIdArgument(String name, int minimalCompletionInput) {
		return new ShopkeeperIdArgument(name, ArgumentFilter.acceptAny(), minimalCompletionInput) {
			@Override
			protected Iterable<Integer> getCompletionSuggestions(String idPrefix) {
				return ShopkeeperByIdArgument.this.getCompletionSuggestions(idPrefix);
			}
		};
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Messages.commandShopkeeperArgumentInvalid;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("argument", argumentInput));
		return text;
	}

	@Override
	protected Shopkeeper getObject(Integer id) throws ArgumentParseException {
		return ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperById(id);
	}

	@Override
	protected Iterable<Integer> getCompletionSuggestions(String idPrefix) {
		return ShopkeeperIdArgument.getDefaultCompletionSuggestions(idPrefix, filter);
	}
}
