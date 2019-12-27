package com.nisovin.shopkeepers.commands.lib.arguments;

import java.util.List;

import com.nisovin.shopkeepers.commands.lib.ArgumentFilter;
import com.nisovin.shopkeepers.commands.lib.ArgumentParseException;
import com.nisovin.shopkeepers.commands.lib.ArgumentRejectedException;
import com.nisovin.shopkeepers.commands.lib.ArgumentsReader;
import com.nisovin.shopkeepers.commands.lib.CommandArgument;
import com.nisovin.shopkeepers.commands.lib.CommandContextView;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.text.Text;

/**
 * Base class for arguments that accept some form of identifier (eg. UUID or name) for specifying a corresponding
 * object.
 *
 * @param <I>
 *            the identifier type
 * @param <O>
 *            the object type
 */
public abstract class ObjectByIdArgument<I, O> extends CommandArgument<O> {

	protected final ArgumentFilter<O> filter; // not null
	protected final ObjectIdArgument<I> idArgument;

	public ObjectByIdArgument(String name, ArgumentFilter<O> filter, int minimalCompletionInput) {
		super(name);
		this.filter = (filter == null) ? ArgumentFilter.acceptAny() : filter;
		this.idArgument = this.createIdArgument(name + ":id", minimalCompletionInput);
		this.idArgument.setParent(this);
	}

	// Implementation note: Usually we don't use an id filter here. Instead we filter directly which objects are
	// involved in generating the suggestions. To achieve that, the created id-argument has to delegate its
	// ObjectIdArgument#getCompletionSuggestions(String) implementation to
	// ObjectByIdArgument#getCompletionSuggestions(String), which should take this argument's object filter into
	// account.
	protected abstract ObjectIdArgument<I> createIdArgument(String name, int minimalCompletionInput);

	@Override
	public Text getMissingArgumentErrorMsg() {
		return idArgument.getMissingArgumentErrorMsg();
	}

	// implementation note: consider overriding #getInvalidArgumentErrorMsg

	/**
	 * Gets the object corresponding to the given id.
	 * 
	 * @param id
	 *            the id
	 * @return the corresponding object, or <code>null</code>
	 * @throws ArgumentParseException
	 *             if the id is ambiguous
	 */
	protected abstract O getObject(I id) throws ArgumentParseException;

	@Override
	public O parseValue(CommandInput input, CommandContextView context, ArgumentsReader argsReader) throws ArgumentParseException {
		// parse id: this deals with invalid and missing input
		I id = idArgument.parseValue(input, context, argsReader);
		O object = this.getObject(id);
		if (object == null) {
			// no corresponding object found:
			throw this.invalidArgumentError(idArgument.toString(id));
		}
		if (!filter.test(object)) {
			// rejected by filter:
			throw new ArgumentRejectedException(this, filter.getInvalidArgumentErrorMsg(this, idArgument.toString(id), object));
		}
		return object;
	}

	/**
	 * Gets the completion suggestions for the given id prefix.
	 * <p>
	 * This should take this argument's object filter into account.
	 * <p>
	 * The id-argument created by {@link #createIdArgument(String, int)} should delegate to this method.
	 * 
	 * @param idPrefix
	 *            the id prefix, may be empty, not <code>null</code>
	 * @return the suggestions
	 */
	protected abstract Iterable<I> getCompletionSuggestions(String idPrefix);

	@Override
	public List<String> complete(CommandInput input, CommandContextView context, ArgumentsReader argsReader) {
		return idArgument.complete(input, context, argsReader);
	}
}
