package com.nisovin.shopkeepers.commands.lib.argument;

import java.util.List;
import java.util.NoSuchElementException;

import org.checkerframework.checker.nullness.qual.Nullable;

import com.nisovin.shopkeepers.util.java.Validate;

/**
 * An unmodifiable view on another {@link ArgumentsReader}.
 */
public class ArgumentsReaderView {

	private final ArgumentsReader argsReader;

	/**
	 * Creates a new {@link ArgumentsReaderView} that provides a read-only view on the given
	 * {@link ArgumentsReader}.
	 * 
	 * @param argsReader
	 *            the underlying arguments reader
	 */
	public ArgumentsReaderView(ArgumentsReader argsReader) {
		Validate.notNull(argsReader, "argsReader is null");
		this.argsReader = argsReader;
	}

	/**
	 * Gets all arguments.
	 * 
	 * @return an unmodifiable view on all arguments
	 */
	public List<? extends String> getArgs() {
		return argsReader.getArgs();
	}

	/**
	 * Gets the total number of arguments.
	 * 
	 * @return the total number of arguments
	 */
	public int getSize() {
		return argsReader.getSize();
	}

	/**
	 * Gets the number of remaining arguments.
	 * 
	 * @return the number of remaining arguments
	 */
	public int getRemainingSize() {
		return argsReader.getRemainingSize();
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
		return argsReader.getCursor();
	}

	/**
	 * Checks whether the cursor points to an argument currently.
	 * 
	 * @return <code>true</code> if there is a current argument
	 */
	public boolean hasCurrent() {
		return argsReader.hasCurrent();
	}

	/**
	 * Gets the argument the cursor currently points at.
	 * 
	 * @return the current argument
	 * @throws NoSuchElementException
	 *             if there is no current argument
	 */
	public String current() throws NoSuchElementException {
		return argsReader.current();
	}

	/**
	 * Gets the argument the cursor currently points at, if there is one.
	 * 
	 * @return the current argument, or <code>null</code> if there is none
	 */
	public @Nullable String currentIfPresent() {
		return argsReader.currentIfPresent();
	}

	/**
	 * Checks whether there are more arguments remaining to be read.
	 * 
	 * @return <code>true</code> if there are more arguments
	 */
	public boolean hasNext() {
		return argsReader.hasNext();
	}

	/**
	 * Gets the next argument without moving the cursor one position further.
	 * 
	 * @return the next argument
	 * @throws NoSuchElementException
	 *             if there is no next argument
	 */
	public String peek() throws NoSuchElementException {
		return argsReader.peek();
	}

	/**
	 * Gets the next argument without moving the cursor one position further.
	 * 
	 * @return the next argument, or <code>null</code> if there is none
	 */
	public @Nullable String peekIfPresent() {
		return argsReader.peekIfPresent();
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ArgumentsReaderView [argsReader=");
		builder.append(argsReader);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		return argsReader.hashCode();
	}

	@Override
	public boolean equals(@Nullable Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (!(obj instanceof ArgumentsReaderView)) return false;
		ArgumentsReaderView other = (ArgumentsReaderView) obj;
		return argsReader.equals(other.argsReader);
	}
}
