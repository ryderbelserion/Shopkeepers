package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentRejectedException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Base class for arguments that accept some form of object identifier (eg. UUID or name).
 * <p>
 * By default this argument accepts any identifier that can be parsed by the wrapped identifier argument.
 * <p>
 * Sub-classes need to override {@link #getCompletionSuggestions(CommandInput, CommandContextView, String)} and
 * {@link #toString(Object)} to provide completion suggestions for partial inputs matching known ids.
 *
 * @param <I>
 *            the identifier type
 */
public abstract class ObjectIdArgument<I> extends CommandArgument<I> {

	protected static final Pattern ARGUMENTS_SEPARATOR_PATTERN = Pattern.compile(Command.ARGUMENTS_SEPARATOR, Pattern.LITERAL);

	protected final CommandArgument<I> idArgument;
	protected final ArgumentFilter<I> filter; // Not null
	// Completions are only provided after at least that many matching input characters:
	protected final int minimalCompletionInput; // 0 to deactivate (provide completions even for empty prefix)

	public ObjectIdArgument(String name, CommandArgument<I> idArgument, ArgumentFilter<I> filter, int minimalCompletionInput) {
		super(name);
		Validate.notNull(idArgument, "idArgument is null");
		Validate.isTrue(minimalCompletionInput >= 0, "minimalCompletionInput cannot be negative");
		this.idArgument = idArgument;
		idArgument.setParent(this);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		this.minimalCompletionInput = minimalCompletionInput;
	}

	@Override
	public I parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// Prefer this class's missing-argument exception over the id-argument's exception:
		if (!argsReader.hasNext()) {
			throw this.missingArgumentError();
		}
		int startIndex = argsReader.getCursor();
		// Throws exceptions with appropriate messages if the id cannot be parsed:
		I id = idArgument.parseValue(input, context, argsReader);

		// Check if id is accepted:
		if (!filter.test(id)) {
			int endIndex = argsReader.getCursor();
			List<String> parsedArgs = argsReader.getArgs().subList(startIndex + 1, endIndex + 1);
			String parsedArgsString = String.join(Command.ARGUMENTS_SEPARATOR, parsedArgs);
			throw new ArgumentRejectedException(this, filter.getInvalidArgumentErrorMsg(this, parsedArgsString, id));
		}
		return id;
	}

	/**
	 * Gets the completion suggestions for the given id prefix.
	 * <p>
	 * The given command input and context can be used to limit the scope of the considered ids.
	 * <p>
	 * Before these suggestions get actually used, they may first have to also pass the id filter used by this
	 * {@link ObjectIdArgument}.
	 * 
	 * @param input
	 *            the command input, not <code>null</code>
	 * @param context
	 *            the command context, not <code>null</code>
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected abstract Iterable<I> getCompletionSuggestions(CommandInput input, CommandContextView context, String idPrefix);

	// This gets applied to convert id completion suggestions to Strings.
	protected abstract String toString(I id);

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		if (argsReader.getRemainingSize() == 0) {
			// There are no remaining arguments to complete.
			return Collections.emptyList();
		}

		// Try to parse id:
		int startIndex = argsReader.getCursor() + 1; // inclusive
		try {
			idArgument.parseValue(input, context, argsReader);
			if (argsReader.getRemainingSize() > 0) {
				// Successfully parsed and there are still arguments left, so this is not consuming the last argument:
				return Collections.emptyList();
			}
		} catch (ArgumentParseException e) {
		}

		// Determine id prefix and args count:
		List<String> args = argsReader.getArgs();
		int endIndex = args.size(); // exclusive
		// The partial id input may consist of multiple joined input arguments:
		int argsCount = (endIndex - startIndex);
		assert argsCount > 0; // Otherwise we would have no remaining arguments in the first place.

		String idPrefix;
		if (argsCount == 1) { // Single argument
			idPrefix = args.get(startIndex);
		} else { // Joined arguments:
			List<String> parsedArgs = args.subList(startIndex, endIndex);
			idPrefix = String.join(Command.ARGUMENTS_SEPARATOR, parsedArgs);
		}

		// Get completion suggestions:
		return this.complete(input, context, idPrefix, argsCount);
	}

	// argsCount: The number of arguments the id prefix consist of (>= 1).
	protected List<String> complete(CommandInput input, CommandContextView context, String idPrefix, int argsCount) {
		// Some types of object id arguments may want to provide suggestions even if there are no remaining args (empty
		// partial input), while others might want to limit their suggestions to the case that there is at least a
		// minimum sized input (eg. if there are lots of candidate ids):
		if (idPrefix.length() < minimalCompletionInput) {
			// Only provide suggestions if there is a minimal length input.
			return Collections.emptyList();
		}

		List<String> suggestions = new ArrayList<>();
		for (I id : this.getCompletionSuggestions(input, context, idPrefix)) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			if (!filter.test(id)) continue; // Skip rejected ids

			String idString = this.toString(id);
			if (idString == null || idString.isEmpty()) continue; // Skip invalid id string

			// If the id prefix consists of multiple joined input arguments, we skip the first (matching) parts and only
			// output the final part(s) of the completed id as completion suggestion:
			if (argsCount > 1) {
				String[] idStringParts = ARGUMENTS_SEPARATOR_PATTERN.split(idString, argsCount);
				// This should usually be true for valid (consistent) argsCount and suggestions:
				if (idStringParts.length == argsCount) {
					idString = idStringParts[argsCount - 1];
				} // Else: Fallback to using the complete idString.
			}
			suggestions.add(idString);
		}
		return Collections.unmodifiableList(suggestions);
	}
}
