package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.argument.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.argument.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.context.CommandContextView;

public class StringArgument extends CommandArgument<String> {

	protected final boolean joinRemainingArgs;

	public StringArgument(String name) {
		this(name, false);
	}

	public StringArgument(String name, boolean joinRemainingArgs) {
		super(name);
		this.joinRemainingArgs = joinRemainingArgs;
	}

	public final boolean isJoiningRemainingArgs() {
		return joinRemainingArgs;
	}

	@Override
	public String parseValue(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) throws ArgumentParseException {
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		if (joinRemainingArgs) {
			return getJoinedRemainingArgs(argsReader);
		} else {
			return argsReader.next();
		}
	}

	@Override
	public List<? extends String> complete(
			CommandInput input,
			CommandContextView context,
			ArgumentsReader argsReader
	) {
		return Collections.emptyList();
	}

	public static String getJoinedRemainingArgs(ArgumentsReader argsReader) {
		if (!argsReader.hasNext()) return "";
		StringBuilder value = new StringBuilder(argsReader.next());
		while (argsReader.hasNext()) {
			value.append(' ').append(argsReader.next());
		}
		return value.toString();
	}
}
