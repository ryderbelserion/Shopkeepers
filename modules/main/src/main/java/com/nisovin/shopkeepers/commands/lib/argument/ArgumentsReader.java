package com.nisovin.shopkeepers.commands.lib.argument;

import java.util.List;
import java.util.NoSuchElementException;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.api.internal.util.Unsafe;
import com.nisovin.shopkeepers.commands.lib.CommandInput;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * Tracks the parsing state of the input arguments passed during command invocation.
 */
public class ArgumentsReader {

	// Directly references to the underlying (unmodifiable) input arguments:
	private final List<? extends String> args;
	private int cursor = -1; // 0 points to the first argument

	private final ArgumentsReaderView view = new ArgumentsReaderView(Unsafe.initialized(this));

	public ArgumentsReader(CommandInput commandInput) {
		Validate.notNull(commandInput, "commandInput is null");
		// CommandInput guarantees: Not null, does not contain null, unmodifiable, arguments don't
		// change during command processing, always returns the same list instance (important for
		// identity-based checks).
		this.args = commandInput.getArguments();
	}

	protected ArgumentsReader(List<? extends String> args) {
		this.args = args;
	}

	/**
	 * Gets all arguments.
	 * 
	 * @return an unmodifiable view on all arguments
	 */
	public List<? extends String> getArgs() {
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
	 * Gets the number of remaining arguments, which can be retrieved by subsequent calls to
	 * {@link #next()}.
	 * 
	 * @return the number of remaining arguments
	 */
	public int getRemainingSize() {
		return this.getSize() - (cursor + 1);
	}

	/**
	 * Gets the cursor's current position.
	 * <p>
	 * The first argument is at index <code>0</code>. The cursor initially starts at position
	 * <code>-1</code> and points at no argument.
	 * 
	 * @return the cursor's current position
	 */
	public int getCursor() {
		return cursor;
	}

	/**
	 * Sets the cursor's current position.
	 * <p>
	 * This affects which argument is returned by a subsequent call to {@link #current()} or
	 * {@link #next()}.
	 * <p>
	 * The first argument is at index <code>0</code>. The cursor initially starts at position
	 * <code>-1</code> and points at no argument. The valid range for the cursor is
	 * <code>[-1, {@link #getSize() size})</code>.
	 * 
	 * @param cursor
	 *            the new cursor position
	 */
	public void setCursor(int cursor) {
		Validate.isTrue(cursor >= -1 && cursor < args.size(), "cursor is out of bounds");
		this.internalSetCursor(cursor);
	}

	private void internalSetCursor(int cursor) {
		assert cursor >= -1 && cursor < args.size();
		this.cursor = cursor;
	}

	/**
	 * Checks whether the cursor points to an argument currently.
	 * 
	 * @return <code>true</code> if there is a current argument
	 */
	public boolean hasCurrent() {
		return (cursor >= 0);
	}

	private void checkHasCurrent() throws NoSuchElementException {
		if (!this.hasCurrent()) {
			throw new NoSuchElementException("No current argument available!");
		}
	}

	/**
	 * Gets the argument the cursor currently points at.
	 * <p>
	 * This keeps the cursor at its current position.
	 * 
	 * @return the current argument
	 * @throws NoSuchElementException
	 *             if there is no current argument
	 */
	public String current() throws NoSuchElementException {
		this.checkHasCurrent();
		return args.get(cursor);
	}

	/**
	 * Gets the argument the cursor currently points at, if there is one.
	 * <p>
	 * This keeps the cursor at its current position.
	 * 
	 * @return the current argument, or <code>null</code> if there is none
	 */
	public @Nullable String currentIfPresent() {
		if (!this.hasCurrent()) {
			return null;
		}
		return args.get(cursor);
	}

	/**
	 * Checks whether there are more arguments remaining to be read.
	 * 
	 * @return <code>true</code> if there are more arguments
	 */
	public boolean hasNext() {
		return (cursor + 1) < args.size();
	}

	private void checkHasNext() throws NoSuchElementException {
		if (!this.hasNext()) {
			throw new NoSuchElementException("No next argument available!");
		}
	}

	/**
	 * Gets the next argument and moves the cursor one position further.
	 * 
	 * @return the next argument
	 * @throws NoSuchElementException
	 *             if there is no next argument
	 */
	public String next() throws NoSuchElementException {
		this.checkHasNext();
		this.internalSetCursor(cursor + 1);
		return args.get(cursor);
	}

	/**
	 * Gets the next argument and moves the cursor one position further, if there is one.
	 * 
	 * @return the next argument, or <code>null</code> if there is none
	 */
	public @Nullable String nextIfPresent() {
		if (!this.hasNext()) {
			return null;
		}
		this.internalSetCursor(cursor + 1);
		return args.get(cursor);
	}

	/**
	 * Gets the next argument without moving the cursor one position further.
	 * 
	 * @return the next argument
	 * @throws NoSuchElementException
	 *             if there is no next argument
	 */
	public String peek() throws NoSuchElementException {
		this.checkHasNext();
		return args.get(cursor + 1);
	}

	/**
	 * Gets the next argument without moving the cursor one position further.
	 * 
	 * @return the next argument, or <code>null</code> if there is none
	 */
	public @Nullable String peekIfPresent() {
		if (!this.hasNext()) {
			return null;
		}
		return args.get(cursor + 1);
	}

	/**
	 * Creates an {@link ArgumentsReader} that copies the current state of this ArgumentsReader.
	 * <p>
	 * This can be used to later reset the state of this ArgumentsReader via
	 * {@link #setState(ArgumentsReader)}.
	 * <p>
	 * Note: The copy references the same underlying input arguments.
	 * 
	 * @return a copy of this ArgumentsReader
	 */
	public ArgumentsReader createSnapshot() {
		ArgumentsReader copy = new ArgumentsReader(this.args);
		copy.cursor = this.cursor;
		return copy;
	}

	/**
	 * Applies the state of the given other {@link ArgumentsReader} to this ArgumentsReader.
	 * <p>
	 * This is only applicable if both ArgumentsReaders reference the same input arguments.
	 * 
	 * @param otherReader
	 *            the other ArgumentsReader
	 */
	public void setState(ArgumentsReader otherReader) {
		Validate.notNull(otherReader, "otherReader is null");
		// only applicable if the readers reference the same arguments:
		Validate.isTrue(otherReader.args == this.args, "otherReader references different arguments");
		this.internalSetCursor(otherReader.cursor);
	}

	/**
	 * Gets an unmodifiable view on this arguments reader.
	 * 
	 * @return an unmodifiable view on this arguments reader
	 */
	public ArgumentsReaderView getView() {
		return view;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ArgumentsReader [args=");
		builder.append(args);
		builder.append(", cursor=");
		builder.append(cursor);
		builder.append("]");
		return builder.toString();
	}

	// Comparisons use the identity of the args list: This should be quick and sufficient for our
	// needs.

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + System.identityHashCode(args); // Also handles null
		result = prime * result + cursor;
		return result;
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ArgumentsReader)) return false;
		ArgumentsReader other = (ArgumentsReader) obj;
		if (args != other.args) return false;
		if (cursor != other.cursor) return false;
		return true;
	}
}
