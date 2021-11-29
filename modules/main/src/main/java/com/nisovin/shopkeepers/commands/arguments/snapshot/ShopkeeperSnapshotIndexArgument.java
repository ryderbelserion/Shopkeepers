package com.nisovin.shopkeepers.commands.arguments.snapshot;

import java.util.Arrays;
import java.util.List;

import com.nisovin.shopkeepers.api.shopkeeper.Shopkeeper;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.arguments.TypedFirstOfArgument;

public class ShopkeeperSnapshotIndexArgument extends CommandArgument<Integer> {

	private final ShopkeeperSnapshotIndexByIdArgument snapshotIdArgument;
	private final ShopkeeperSnapshotIndexByNameArgument snapshotNameArgument;
	private final TypedFirstOfArgument<Integer> firstOfArgument;
	private final boolean inflateFormat;

	public ShopkeeperSnapshotIndexArgument(String name, CommandArgument<? extends Shopkeeper> shopkeeperArgument) {
		this(name, shopkeeperArgument, false);
	}

	public ShopkeeperSnapshotIndexArgument(	String name, CommandArgument<? extends Shopkeeper> shopkeeperArgument,
											boolean joinRemainingArgs) {
		this(name, shopkeeperArgument, joinRemainingArgs, false);
	}

	public ShopkeeperSnapshotIndexArgument(	String name, CommandArgument<? extends Shopkeeper> shopkeeperArgument,
											boolean joinRemainingArgs, boolean inflateFormat) {
		this(name, shopkeeperArgument, joinRemainingArgs, inflateFormat,
				ShopkeeperSnapshotIndexByIdArgument.DEFAULT_MINIMUM_COMPLETION_INPUT,
				ShopkeeperSnapshotIndexByNameArgument.DEFAULT_MINIMUM_COMPLETION_INPUT);
	}

	public ShopkeeperSnapshotIndexArgument(	String name, CommandArgument<? extends Shopkeeper> shopkeeperArgument,
											boolean joinRemainingArgs, boolean inflateFormat, int minimumIdCompletionInput,
											int minimalNameCompletionInput) {
		super(name);
		this.inflateFormat = inflateFormat;
		this.snapshotIdArgument = new ShopkeeperSnapshotIndexByIdArgument(name + "-id", shopkeeperArgument, minimumIdCompletionInput);
		this.snapshotNameArgument = new ShopkeeperSnapshotIndexByNameArgument(name + "-name", shopkeeperArgument, joinRemainingArgs, minimalNameCompletionInput);
		this.firstOfArgument = new TypedFirstOfArgument<>(name + ":firstOf", Arrays.asList(snapshotIdArgument, snapshotNameArgument), true, false);
		firstOfArgument.setParent(this);
	}

	@Override
	public String getReducedFormat() {
		if (inflateFormat) {
			return firstOfArgument.getReducedFormat();
		} else {
			return super.getReducedFormat();
		}
	}

	@Override
	public Integer parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		return firstOfArgument.parseValue(input, context, argsReader);
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return firstOfArgument.complete(input, context, argsReader);
	}
}
