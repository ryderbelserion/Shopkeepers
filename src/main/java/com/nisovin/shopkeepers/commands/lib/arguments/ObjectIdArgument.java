package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentRejectedException;
import com.nisovin.shopkeepers.commands.lib.Command;
import com.nisovin.shopkeepers.commands.lib.CommandArgs;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContext;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.Utils;
import com.nisovin.shopkeepers.util.Validate;

/**
 * Base class for arguments that accept some form of identifier (eg. UUID or name) when there is a limited set of known
 * applicable identifiers.
 * <p>
 * By default this argument actually accepts any identifier that can be parsed by the wrapped identifier argument.
 * However, the {@link #getKnownIds()} get used to provide suggestions for partial inputs.
 * <p>
 * If the option <code>matchKnownIds</code> is used and a known identifier matches the given input (according to the
 * used matching function), that identifier will be returned instead of the input.
 *
 * @param <I>
 *            the identifier type
 */
public abstract class ObjectIdArgument<Id> extends CommandArgument<Id> {

	protected final CommandArgument<Id> idArgument;
	protected final ArgumentFilter<Id> filter; // not null
	protected final boolean matchKnownIds;
	// completions are only provided after at least that many matching input characters:
	protected final int minimalCompletionInput; // <= 0 to deactivate

	public ObjectIdArgument(CommandArgument<Id> idArgument, ArgumentFilter<Id> filter, boolean matchKnownIds, int minimalCompletionInput) {
		super(Validate.notNull(idArgument).getName());
		this.idArgument = idArgument;
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		this.matchKnownIds = matchKnownIds;
		this.minimalCompletionInput = minimalCompletionInput;
	}

	// gets used for the suggestions (after passing the id filter)
	protected abstract Iterable<Id> getKnownIds();

	protected final Iterable<Id> getFilteredIds() {
		return Utils.stream(this.getKnownIds()).filter(filter)::iterator;
	}

	// returns the identifier to use (may be the input identifier, or a different but matching one)
	// may for example be used to normalize the returned id if a matching id is known
	protected abstract Id matchKnownId(Id input);

	// toString(Id) and normalize(String) are used to match partial ids to the known ids when checking for completions
	protected abstract String toString(Id id);

	// idString is potentially partial
	protected abstract String normalize(String idString);

	@Override
	public Id parseValue(CommandInput input, CommandArgs args) throws ArgumentParseException {
		// prefer this class's missing-argument exception over the id-argument's exception:
		if (!args.hasNext()) {
			throw this.missingArgument();
		}
		int startIndex = args.getCurrentIndex();
		// throws exceptions with appropriate messages if the id cannot be parsed:
		Id id = idArgument.parseValue(input, args);

		// check if the id matches a known id and then use that instead:
		if (matchKnownIds) {
			id = this.matchKnownId(id);
		}

		// check if id is accepted:
		if (!filter.test(id)) {
			int endIndex = args.getCurrentIndex();
			List<String> parsedArgs = args.getArgs().subList(startIndex + 1, endIndex + 1);
			String parsedArgsString = String.join(Command.ARGUMENTS_SEPARATOR, parsedArgs);
			throw new ArgumentRejectedException(filter.getInvalidArgumentErrorMsg(this, parsedArgsString, id));
		}
		return id;
	}

	@Override
	public List<String> complete(CommandInput input, CommandContext context, CommandArgs args) {
		// Note: By default this also provides suggestions if there a no remaining args (empty partial input).
		// Some types of id-arguments (eg. if there are lots of candidate ids) might want to limit their suggestions to
		// the case that there is at least a minimum sized input (see minimalCompletionInput).
		int startIndex = args.getCurrentIndex();
		// parse id as far as possible:
		try {
			idArgument.parseValue(input, args);
		} catch (ArgumentParseException e) {
		}
		if (args.getRemainingSize() > 0) {
			// there are still arguments left, so this is not consuming the last argument
			return Collections.emptyList();
		}

		int endIndex = args.getCurrentIndex();
		List<String> parsedArgs = args.getArgs().subList(startIndex + 1, endIndex + 1);
		String parsedArgsString = String.join(Command.ARGUMENTS_SEPARATOR, parsedArgs);
		if (parsedArgsString.length() < minimalCompletionInput) {
			// only provide suggestions if there is a minimal length input
			return Collections.emptyList();
		}
		String partialIdString = this.normalize(parsedArgsString);
		List<String> suggestions = new ArrayList<>();
		// using the filtered ids, because we don't want to show suggestions that don't get accepted
		// TODO support for aliases, that can be completed, but only include at most one of them in the suggestions
		// see for eg. player's with name and display name
		for (Id id : this.getFilteredIds()) {
			if (suggestions.size() >= MAX_SUGGESTIONS) break;
			String idString = this.toString(id);
			if (this.normalize(idString).startsWith(partialIdString)) {
				// TODO only add the part of the name past the matching parts as suggestion (in case of joined remaining
				// args)
				suggestions.add(idString); // add the unnormalized id string
			}
		}
		return Collections.unmodifiableList(suggestions);
	}
}
