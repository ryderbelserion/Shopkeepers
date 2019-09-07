package com.nisovin.shopkeepers.commands.lib;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import com.nisovin.shopkeepers.util.Validate;

public class CommandArgs {

	public static final CommandArgs EMPTY = new CommandArgs(Collections.emptyList());

	private final List<String> args; // unmodifiable
	private int curIndex = -1;

	public CommandArgs(String[] args) {
		this(args == null ? Collections.emptyList() : Arrays.asList(args));
	}

	public CommandArgs(List<String> args) {
		Validate.notNull(args, "Arguments cannot be null!");
		Validate.isTrue(!args.contains(null), "Arguments contain null!");
		this.args = Collections.unmodifiableList(args);
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

	// Get and set state: In case we ever add more state specific data, besides the index.

	public static final class State {

		private final CommandArgs args;
		private final int curIndex;

		private State(CommandArgs args, int curIndex) {
			this.args = args;
			this.curIndex = curIndex;
		}
	}

	/**
	 * Gets the current state of this {@link CommandArgs}, which can be used to reset with {@link #setState(Object)}.
	 * 
	 * @return the current state
	 */
	public Object getState() {
		return new State(this, curIndex);
	}

	/**
	 * Restores the state of this {@link CommandArgs} to a state previously captured via {@link #getState()}.
	 * 
	 * @param stateObject
	 *            the state to reset to
	 */
	public void setState(Object stateObject) {
		Validate.isTrue(stateObject instanceof State, "Invalid state!");
		State state = (State) stateObject;
		Validate.isTrue(state.args == this, "The given state wasn't created from this CommandArgs instance!");
		this.curIndex = state.curIndex;
	}
}
