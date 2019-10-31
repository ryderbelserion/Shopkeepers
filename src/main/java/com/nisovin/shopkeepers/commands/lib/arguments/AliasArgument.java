package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;
import java.util.function.Function;

import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Wraps another {@link CommandArgument} and uses a different name to represent it in the argument's format.
 */
public class AliasArgument<T> extends CommandArgument<T> {

	/**
	 * Constructs a new {@link AliasArgument} by evaluating the provided function.
	 * <p>
	 * This may be useful for fluently creating an {@link AliasArgument} with a display name that is based on some
	 * property of the wrapped argument (for example its previous argument format).
	 * <p>
	 * The provided function is only evaluated once to determine the fixed display name to use. It does not get
	 * evaluated dynamically whenever the argument's display name is requested.
	 * 
	 * @param <T>
	 *            the argument's parsed type
	 * @param <A>
	 *            the argument type
	 * @param argument
	 *            the wrapped argument
	 * @param displayNameFunction
	 *            the function providing the new display name
	 * @return the new alias argument
	 */
	public static <T, A extends CommandArgument<T>> AliasArgument<T> of(A argument, Function<A, String> displayNameFunction) {
		Validate.notNull(argument, "Argument is null!");
		Validate.notNull(displayNameFunction, "displayNameFunction is null!");
		return new AliasArgument<>(displayNameFunction.apply(argument), argument);
	}

	private final String displayName; // can be empty! (see hidden arguments)
	private final CommandArgument<T> argument;

	public AliasArgument(String displayName, CommandArgument<T> argument) {
		// using the wrapped argument's name for all purposes except display
		super(Validate.notNull(argument, "Argument is null!").getName());
		Validate.notNull(displayName, "Display name is null!");
		this.displayName = displayName;
		this.argument = argument;
	}

	@Override
	public String getReducedFormat() {
		return displayName;
	}

	@Override
	public T parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		return argument.parseValue(input, context, argsReader);// delegate
	}

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return argument.complete(input, context, argsReader); // delegate
	}
}
