package com.nisovin.shopkeepers.commands.arguments.snapshot;

import java.util.Collections;
import java.util.Locale;
import java.util.Objects;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectByIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectNameArgument;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.StringUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Determines the index of a shopkeeper {@link Shopkeeper#getSnapshots() snapshot} by a given name input.
 * <p>
 * The shopkeeper whose snapshots are considered is derived from the {@link CommandContext} using a given command
 * argument.
 */
public class ShopkeeperSnapshotIndexByNameArgument extends ObjectByIdArgument<String, Integer> {

	public static final int DEFAULT_MINIMUM_COMPLETION_INPUT = 0;

	private final CommandArgument<? extends Shopkeeper> shopkeeperArgument;

	public ShopkeeperSnapshotIndexByNameArgument(String name, CommandArgument<? extends Shopkeeper> shopkeeperArgument) {
		this(name, shopkeeperArgument, false, DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ShopkeeperSnapshotIndexByNameArgument(	String name, CommandArgument<? extends Shopkeeper> shopkeeperArgument,
													boolean joinRemainingArgs, int minimumCompletionInput) {
		super(name, ArgumentFilter.acceptAny(), new IdArgumentArgs(minimumCompletionInput, joinRemainingArgs));
		Validate.notNull(shopkeeperArgument, "shopkeeperArgument is null");
		this.shopkeeperArgument = shopkeeperArgument;
	}

	@Override
	protected ObjectIdArgument<String> createIdArgument(String name, IdArgumentArgs args) {
		return new ObjectNameArgument(name, args.joinRemainingArgs, ArgumentFilter.acceptAny(), args.minimumCompletionInput) {
			@Override
			protected Iterable<String> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix) {
				return ShopkeeperSnapshotIndexByNameArgument.this.getCompletionSuggestions(input, context, minimumCompletionInput, idPrefix);
			}
		};
	}

	private Shopkeeper getShopkeeperScope(CommandInput input, CommandContextView context) {
		Object shopkeeper = context.get(shopkeeperArgument.getName());
		if (shopkeeper instanceof Shopkeeper) return (Shopkeeper) shopkeeper;
		return null;
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		if (argumentInput == null) argumentInput = "";
		Text text = Messages.invalidSnapshotName;
		text.setPlaceholderArguments(this.getDefaultErrorMsgArgs());
		text.setPlaceholderArguments(Collections.singletonMap("name", argumentInput));
		return text;
	}

	@Override
	protected Integer getObject(CommandInput input, CommandContextView context, String id) throws ArgumentParseException {
		assert id != null;
		if (id.isEmpty()) return null;
		Shopkeeper shopkeeper = this.getShopkeeperScope(input, context);
		if (shopkeeper == null) return null;
		int snapshotIndex = shopkeeper.getSnapshotIndex(id);
		return (snapshotIndex != -1) ? snapshotIndex : null;
	}

	@Override
	protected Iterable<String> getCompletionSuggestions(CommandInput input, CommandContextView context,
														int minimumCompletionInput, String idPrefix) {
		// Only provide suggestions if there is a minimum length input:
		if (idPrefix.length() < minimumCompletionInput) {
			return Collections.emptyList();
		}

		Shopkeeper shopkeeper = this.getShopkeeperScope(input, context);
		if (shopkeeper == null) return Collections.emptyList();

		String normalizedNamePrefix = StringUtils.normalize(idPrefix);
		return shopkeeper.getSnapshots().stream().map(snapshot -> {
			String normalizedWithCase = StringUtils.normalizeKeepCase(snapshot.getName());
			String normalized = normalizedWithCase.toLowerCase(Locale.ROOT);
			if (normalized.startsWith(normalizedNamePrefix)) {
				return normalizedWithCase;
			} else {
				return null;
			}
		}).filter(Objects::nonNull)::iterator;
	}
}
