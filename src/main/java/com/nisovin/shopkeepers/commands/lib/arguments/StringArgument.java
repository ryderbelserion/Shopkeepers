package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;

public class StringArgument extends CommandArgument {

	protected final boolean joinRemainingArgs;

	public StringArgument(String name) {
		this(name, false);
	}

	public StringArgument(String name, boolean joinRemainingArgs) {
		super(name);
		this.joinRemainingArgs = joinRemainingArgs;
	}

	@Override
	public Object parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		if (!args.hasNext()) {
			throw this.missingArgument();
		}
		if (joinRemainingArgs) {
			return this.getJoinedRemainingArgs(args);
		} else {
			return args.next();
		}
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		return Collections.emptyList();
	}

	public String getJoinedRemainingArgs(CommandArgs args) {
		StringBuilder value = new StringBuilder(args.next());
		while (args.hasNext()) {
			value.append(' ').append(args.next());
		}
		return value.toString();
	}
}
