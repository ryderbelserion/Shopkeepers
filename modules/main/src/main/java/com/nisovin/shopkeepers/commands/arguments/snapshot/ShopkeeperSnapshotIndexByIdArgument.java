package com.nisovin.shopkeepers.commands.arguments.snapshot;

import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.api.shopkeeper.ShopkeeperSnapshot;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.argument.filter.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectByIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.ObjectIdArgument;
import com.nisovin.shopkeepers.commands.lib.arguments.PositiveIntegerArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContext;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;
import com.nisovin.shopkeepers.lang.Messages;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.ConversionUtils;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Determines the index of a shopkeeper {@link Shopkeeper#getSnapshots() snapshot} by a given id.
 * <p>
 * The shopkeeper whose snapshots are considered is derived from the {@link CommandContext} using a
 * given command argument.
 * <p>
 * Invalid ids (i.e. ids that are out of bounds) are rejected.
 */
public class ShopkeeperSnapshotIndexByIdArgument extends ObjectByIdArgument<Integer, Integer> {

	public static final int DEFAULT_MINIMUM_COMPLETION_INPUT = 1;

	private final CommandArgument<? extends Shopkeeper> shopkeeperArgument;

	public ShopkeeperSnapshotIndexByIdArgument(
			String name,
			CommandArgument<? extends Shopkeeper> shopkeeperArgument
	) {
		this(name, shopkeeperArgument, DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ShopkeeperSnapshotIndexByIdArgument(
			String name,
			CommandArgument<? extends Shopkeeper> shopkeeperArgument,
			int minimumCompletionInput
	) {
		super(name, ArgumentFilter.acceptAny(), new IdArgumentArgs(minimumCompletionInput));
		Validate.notNull(shopkeeperArgument, "shopkeeperArgument is null");
		this.shopkeeperArgument = shopkeeperArgument;
	}

	@Override
	protected ObjectIdArgument<Integer> createIdArgument(
			@UnknownInitialization ShopkeeperSnapshotIndexByIdArgument this,
			String name,
			IdArgumentArgs args
	) {
		return new ObjectIdArgument<Integer>(
				name,
				new PositiveIntegerArgument(name + ":id"),
				ArgumentFilter.acceptAny(),
				args.minimumCompletionInput
		) {
			@Override
			protected Iterable<? extends Integer> getCompletionSuggestions(
					CommandInput input,
					CommandContextView context,
					String idPrefix
			) {
				return ShopkeeperSnapshotIndexByIdArgument.this.getCompletionSuggestions(
						input,
						context,
						minimumCompletionInput,
						idPrefix
				);
			}

			@Override
			protected String toString(Integer id) {
				return id.toString();
			}
		};
	}

	private @Nullable Shopkeeper getShopkeeperScope(
			CommandInput input,
			CommandContextView context
	) {
		Object shopkeeper = context.getOrNull(shopkeeperArgument.getName());
		if (shopkeeper instanceof Shopkeeper) return (Shopkeeper) shopkeeper;
		return null;
	}

	@Override
	protected Text getInvalidArgumentErrorMsgText() {
		return Messages.invalidSnapshotId;
	}

	@Override
	protected @Nullable Integer getObject(
			CommandInput input,
			CommandContextView context,
			Integer id
	) throws ArgumentParseException {
		assert id != null && id > 0;
		Shopkeeper shopkeeper = this.getShopkeeperScope(input, context);
		if (shopkeeper == null) return null;
		if (id > shopkeeper.getSnapshots().size()) return null;
		// Convert the valid snapshot id to a valid snapshot index:
		return id - 1;
	}

	@Override
	protected Iterable<? extends Integer> getCompletionSuggestions(
			CommandInput input,
			CommandContextView context,
			int minimumCompletionInput,
			String idPrefix
	) {
		// Only provide suggestions if there is a minimum length input:
		if (idPrefix.length() < minimumCompletionInput) {
			return Collections.emptyList();
		}

		// If idPrefix is not empty but not a valid number, we can skip checking for completions:
		if (!idPrefix.isEmpty() && ConversionUtils.parseInt(idPrefix) == null) {
			return Collections.emptyList();
		}

		Shopkeeper shopkeeper = this.getShopkeeperScope(input, context);
		if (shopkeeper == null) {
			return Collections.emptyList();
		}

		List<? extends ShopkeeperSnapshot> snapshots = shopkeeper.getSnapshots();
		return IntStream.rangeClosed(1, snapshots.size())
				.filter(id -> String.valueOf(id).startsWith(idPrefix))::iterator;
	}
}
