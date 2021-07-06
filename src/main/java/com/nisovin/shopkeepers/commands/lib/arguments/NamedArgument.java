package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.commands.lib.FallbackArgumentException;
import com.nisovin.shopkeepers.text.Text;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * A {@link CommandArgument} that requires the command user to explicitly specify this argument by name by using the
 * input format <code>"{argumentName}={argument}"</code>.
 * <p>
 * This can be useful when there would otherwise be ambiguities between different arguments.
 * <p>
 * Currently this might only function correctly for command arguments that consume exactly one input argument.
 */
public class NamedArgument<T> extends FallbackArgument<T> {

	private static final String NAME_DELIMITER = "=";

	private final CommandArgument<T> argument;

	public NamedArgument(CommandArgument<T> argument) {
		super(Validate.notNull(argument, "Argument is null!").getName());
		this.argument = argument;
		argument.setParent(this);
	}

	@Override
	public Text getRequiresPlayerErrorMsg() {
		return argument.getRequiresPlayerErrorMsg();
	}

	@Override
	public Text getMissingArgumentErrorMsg() {
		return argument.getMissingArgumentErrorMsg();
	}

	@Override
	public Text getInvalidArgumentErrorMsg(String argumentInput) {
		return argument.getInvalidArgumentErrorMsg(argumentInput);
	}

	private String getArgumentPrefix() {
		return argument.getDisplayName() + NAME_DELIMITER;
	}

	@Override
	public String getReducedFormat() {
		return this.getArgumentPrefix() + "...";
	}

	@FunctionalInterface
	private static interface Parser<R> {
		public R parse(CommandInput input, ArgumentsReader argsReader) throws ArgumentParseException;
	}

	private <R> R parseNamedArgument(CommandInput input, ArgumentsReader argsReader, Parser<R> parser) throws ArgumentParseException {
		return this.parseNamedArgument(input, argsReader, this.getArgumentPrefix(), parser);
	}

	private <R> R parseNamedArgument(CommandInput input, ArgumentsReader argsReader, String argPrefix, Parser<R> parser) throws ArgumentParseException {
		String arg = argsReader.peekIfPresent();
		if (arg == null) {
			throw this.missingArgumentError();
		}

		if (!arg.startsWith(argPrefix)) {
			throw this.invalidArgumentError(arg);
		}

		// Strip the naming prefix from the input argument:
		String namelessArg = arg.substring(argPrefix.length());
		List<String> adjustedArgs = new ArrayList<>(input.getArguments());
		for (int i = 0; i < adjustedArgs.size(); ++i) {
			if (adjustedArgs.get(i).equals(arg)) {
				adjustedArgs.set(i, namelessArg);
				break;
			}
		}
		CommandInput adjustedInput = new CommandInput(input.getSender(), input.getCommand(), input.getCommandAlias(), adjustedArgs);
		ArgumentsReader adjustedArgsReader = new ArgumentsReader(adjustedInput);
		adjustedArgsReader.setCursor(argsReader.getCursor());

		try {
			R value = parser.parse(adjustedInput, adjustedArgsReader);
			argsReader.setCursor(adjustedArgsReader.getCursor()); // Mirror args reader changes
			return value;
		} catch (FallbackArgumentException e) {
			// Wrap into our own fallback exception, so that we get informed:
			throw new FallbackArgumentException(this, e);
		}
	}

	@Override
	public T parse(CommandInput input, CommandContext context, ArgumentsReader argsReader) throws ArgumentParseException {
		return this.parseNamedArgument(input, argsReader, (adjustedInput, adjustedArgsReader) -> {
			return argument.parse(adjustedInput, context, adjustedArgsReader);
		});
	}

	@Override
	public T parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		return this.parseNamedArgument(input, argsReader, (adjustedInput, adjustedArgsReader) -> {
			return argument.parseValue(adjustedInput, context, adjustedArgsReader);
		});
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		String arg = argsReader.peekIfPresent();
		if (arg == null) {
			return Collections.emptyList();
		}

		String argPrefix = this.getArgumentPrefix();
		// Prefix might be partially given:
		String prefix = (argPrefix.startsWith(arg) ? arg : argPrefix);
		try {
			return this.parseNamedArgument(input, argsReader, prefix, (adjustedInput, adjustedArgsReader) -> {
				return argument.complete(adjustedInput, context, adjustedArgsReader);
			}).stream().map((s) -> argPrefix + s).collect(Collectors.toList());
		} catch (ArgumentParseException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public T parseFallback(CommandInput input, CommandContext context, ArgumentsReader argsReader, FallbackArgumentException fallbackException, boolean parsingFailed) throws ArgumentParseException {
		return this.parseNamedArgument(input, argsReader, (adjustedInput, adjustedArgsReader) -> {
			FallbackArgumentException originalFallback = (FallbackArgumentException) fallbackException.getOriginalException();
			return ((FallbackArgument<T>) argument).parseFallback(adjustedInput, context, adjustedArgsReader, originalFallback, parsingFailed);
		});
	}
}
