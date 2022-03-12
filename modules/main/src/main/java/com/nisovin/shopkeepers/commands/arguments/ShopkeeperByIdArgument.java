package com.nisovin.shopkeepers.commands.arguments;


import com.nisovin.shopkeepers.api.ShopkeepersAPI;
import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectByIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
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
		this(name, filter, ShopkeeperIdArgument.DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ShopkeeperByIdArgument(String name, ArgumentFilter<Shopkeeper> filter, int minimumCompletionInput) {
		super(name, filter, new IdArgumentArgs(minimumCompletionInput));
	}

	@Override
	protected ObjectIdArgument<Integer> createIdArgument(String name, IdArgumentArgs args) {
		return new ShopkeeperIdArgument(name, ArgumentFilter.acceptAny(), args.minimumCompletionInput) {
			@Override
			protected Iterable<Integer> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix) {
				return ShopkeeperByIdArgument.this.getCompletionSuggestions(input, context, minimumCompletionInput, idPrefix);
			}
		};
	}

	@Override
	protected Text getInvalidArgumentErrorMsgText() {
		return Messages.commandShopkeeperArgumentInvalid;
	}

	@Override
	protected Shopkeeper getObject(CommandInput input, CommandContextView context, Integer id) throws ArgumentParseException {
		return ShopkeepersAPI.getShopkeeperRegistry().getShopkeeperById(id);
	}

	@Override
	protected Iterable<Integer> getCompletionSuggestions(	CommandInput input, CommandContextView context,
															int minimumCompletionInput, String idPrefix) {
		return ShopkeeperIdArgument.getDefaultCompletionSuggestions(input, context, minimumCompletionInput, idPrefix, filter);
	}
}
