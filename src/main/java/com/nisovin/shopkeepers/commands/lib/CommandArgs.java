package com.nisovin.shopkeepers.commands.lib;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import com.nisovin.shopkeepers.util.Validate;

/**
 * The arguments passed during command invocation together with the current parsing state.
 */
public class CommandArgs {

	public static final CommandArgs EMPTY = new CommandArgs(Collections.emptyList());

	private final List<String> args; // unmodifiable
	private int curIndex = -1;

	// the args array is expected to not change during the lifetime of this CommandArgs
	public CommandArgs(String[] args) {
		this(args == null ? Collections.emptyList() : Arrays.asList(args));
	}

	// the args list is expected to not change during the lifetime of this CommandArgs
	public CommandArgs(List<String> args) {
		Validate.notNull(args, "Arguments are null!");
		Validate.isTrue(!args.contains(null), "Arguments contain null!");
		// new object instance is important here, since its identity is used by #setState(State)
		this.args = Collections.unmodifiableList(args);
	}

	/**
	 * Creates a copy of the given {@link CommandArgs}.
	 * <p>
	 * Note: Since it is expected that the underlying arguments do not change during the lifetime of the
	 * {@link CommandArgs}, it is sufficient to directly reference the arguments of the given {@link CommandArgs}
	 * instead of actually copying them.
	 * 
	 * @param otherArgs
	 *            the other command args
	 */
	protected CommandArgs(CommandArgs otherArgs) {
		Validate.notNull(otherArgs, "The other CommandArgs is null!");
		// directly references the parent's args, which are expected to not change
		this.args = otherArgs.args;
		this.curIndex = otherArgs.curIndex;
	}

	/**
	 * Gets all arguments.
	 * 
	 * @return an unmodifiable view on all arguments
	 */
	public List<String> getArgs() {
		return args;
	}

	/**
	 * Gets the total number of arguments.
	 * 
	 * @return the total number of arguments
	 */
	public int getSize() {
		return args.size();
	}

	/**
	 * Gets the number of remaining arguments, which can be retrieved by subsequent calls to {@link #next()}.
	 * 
	 * @return the number of remaining arguments
	 */
	public int getRemainingSize() {
		return this.getSize() - (curIndex + 1);
	}

	/**
	 * Gets the cursor's current position.
	 * <p>
	 * The first argument is at index <code>0</code>. The cursor initially starts before the first argument, at position
	 * <code>-1</code>.
	 * 
	 * @return the cursor's current position
	 */
	public int getCurrentIndex() {
		return curIndex;
	}

	/**
	 * Checks whether there is a current argument.
	 * 
	 * @return <code>true</code> if there is a current argument
	 */
	public boolean hasCurrent() {
		return (curIndex >= 0);
	}

	private void checkHasCurrent() throws NoSuchElementException {
		if (!hasCurrent()) {
			throw new NoSuchElementException("No current argument available!");
		}
	}

	/**
	 * Gets the current argument.
	 * <p>
	 * This keeps the cursor at its current position.
	 * 
	 * @return the current argument
	 * @throws NoSuchElementException
	 *             if there is no current argument
	 */
	public String current() throws NoSuchElementException {
		this.checkHasCurrent();
		return args.get(curIndex);
	}

	/**
	 * Gets the current argument, if there is one.
	 * <p>
	 * This keeps the cursor at its current position.
	 * 
	 * @return the current argument, <code>null</code> if none is available
	 */
	public String currentIfPresent() {
		if (!this.hasCurrent()) {
			return null;
		}
		return args.get(curIndex);
	}

	/**
	 * Checks whether there are more arguments remaining to be read.
	 * 
	 * @return <code>true</code> if there are more arguments
	 */
	public boolean hasNext() {
		return (curIndex + 1) < args.size();
	}

	private void checkHasNext() throws NoSuchElementException {
		if (!hasNext()) {
			throw new NoSuchElementException("No next argument available!");
		}
	}

	/**
	 * Gets the next argument and moves the cursor one further.
	 * 
	 * @return the next argument
	 * @throws NoSuchElementException
	 *             if there is no next argument
	 */
	public String next() throws NoSuchElementException {
		this.checkHasNext();
		return args.get(++curIndex);
	}

	/**
	 * Gets the next argument and moves the cursor one further, if there is one.
	 * 
	 * @return the next argument, <code>null</code> if none is available
	 */
	public String nextIfPresent() {
		if (!this.hasNext()) {
			return null;
		}
		return args.get(++curIndex);
	}

	/**
	 * Gets the next argument without moving the cursor one further.
	 * 
	 * @return the next argument
	 * @throws NoSuchElementException
	 *             if there is no next argument
	 */
	public String peek() throws NoSuchElementException {
		this.checkHasNext();
		return args.get(curIndex + 1);
	}

	/**
	 * Gets the next argument without moving the cursor one further.
	 * 
	 * @return the next argument, <code>null</code> if none is available
	 */
	public String peekIfPresent() {
		if (!this.hasNext()) {
			return null;
		}
		return args.get(curIndex + 1);
	}

	/**
	 * Returns the index of the element that would be returned by a subsequent call to {@link #next()}.
	 * 
	 * @return the index of the next argument, or the number of total arguments if none is available
	 */
	public int nextIndex() {
		return curIndex + 1;
	}

	/**
	 * Sets the next index.
	 * <p>
	 * This index affects which argument is returned by a subsequent call {@link #next()}. It ranges from <code>0</code>
	 * to the number of total arguments (in which case no next argument can be retrieved by {@link #next()}).
	 * 
	 * @param nextIndex
	 *            the next index
	 */
	public void setNextIndex(int nextIndex) {
		Validate.isTrue(nextIndex >= 0 && nextIndex <= args.size(), "Index out of bounds!");
		curIndex = (nextIndex - 1);
	}

	/**
	 * Creates a copy of this {@link CommandArgs}.
	 * <p>
	 * Note: The underlying arguments are expected to not change during the lifetime of both this {@link CommandArgs}
	 * and the returned copy.
	 * 
	 * @return the copied command args
	 */
	public CommandArgs copy() {
		return new CommandArgs(this);
	}

	// Get and set state: In case we ever add more state specific data, besides the index.

	/**
	 * Marker interface for state objects.
	 */
	public interface State {
	}

	protected static final class StateImpl implements State {

		protected final CommandArgs commandArgs;
		protected final int curIndex;

		private StateImpl(CommandArgs commandArgs, int curIndex) {
			this.commandArgs = commandArgs;
			this.curIndex = curIndex;
		}
	}

	/**
	 * Gets the current state of this {@link CommandArgs}, which can be used to reset with {@link #setState(Object)}.
	 * 
	 * @return the current state
	 */
	public State getState() {
		return new StateImpl(this, curIndex);
	}

	/**
	 * Restores the state of this {@link CommandArgs} to a state previously captured via {@link #getState()}.
	 * 
	 * @param stateObject
	 *            the state to reset to
	 */
	public void setState(State stateObject) {
		Validate.isTrue(stateObject instanceof StateImpl, "Invalid state!");
		StateImpl state = (StateImpl) stateObject;
		// state is applicable to the original CommandArgs, as well as all its copies (if the underlying arguments are
		// the same):
		Validate.isTrue(state.commandArgs.args == this.args, "The given state is not applicable for this CommandArgs instance!");
		this.curIndex = state.curIndex;
	}
}
