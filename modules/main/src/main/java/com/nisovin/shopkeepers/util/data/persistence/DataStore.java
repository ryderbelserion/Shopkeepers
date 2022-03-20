package com.nisovin.shopkeepers.util.data.persistence;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Path;

import com.nisovin.shopkeepers.util.data.container.DataContainer;

/**
 * A {@link DataContainer} that provides means to save and load the stored data to and from some
 * concrete storage format.
 */
public interface DataStore extends DataContainer {

	/**
	 * Loads the contents of this data container from the specified {@link File}.
	 * <p>
	 * The file contents are expected to be UTF-8 encoded.
	 * <p>
	 * All currently stored values of this data container are removed and subsequently replaced with
	 * the newly loaded values.
	 * 
	 * @param file
	 *            the file, not <code>null</code>
	 * @throws IOException
	 *             if the file cannot be read
	 * @throws InvalidDataFormatException
	 *             if the content of the specified file is not in a supported format
	 */
	public void load(File file) throws IOException, InvalidDataFormatException;

	/**
	 * Loads the contents of this data container from the file at the specified {@link Path}.
	 * <p>
	 * The file contents are expected to be UTF-8 encoded.
	 * <p>
	 * All currently stored values of this data container are removed and subsequently replaced with
	 * the newly loaded values.
	 * 
	 * @param path
	 *            the file path, not <code>null</code>
	 * @throws IOException
	 *             if the file cannot be read
	 * @throws InvalidDataFormatException
	 *             if the content of the specified file is not in a supported format
	 */
	public void load(Path path) throws IOException, InvalidDataFormatException;

	/**
	 * Loads the contents of this data container from the given {@link Reader}.
	 * <p>
	 * All currently stored values of this data container are removed and subsequently replaced with
	 * the newly loaded values.
	 * <p>
	 * This operation closes the given {@link Reader} after its contents have been read.
	 * 
	 * @param reader
	 *            the reader, not <code>null</code>
	 * @throws IOException
	 *             if the reader cannot be read
	 * @throws InvalidDataFormatException
	 *             if the content of the given reader is not in a supported format
	 */
	public void load(Reader reader) throws IOException, InvalidDataFormatException;

	/**
	 * Loads the contents of this data container from the given {@link String}.
	 * <p>
	 * All currently stored values of this data container are removed and subsequently replaced with
	 * the newly loaded values.
	 * 
	 * @param data
	 *            the data String, not <code>null</code>
	 * @throws InvalidDataFormatException
	 *             if the content of the given String is not in a supported format
	 */
	public void loadFromString(String data) throws InvalidDataFormatException;

	/**
	 * Saves the contents of this data container to the specified {@link File}.
	 * 
	 * @param file
	 *            the file, not <code>null</code>
	 * @throws IOException
	 *             if the file cannot be written to
	 */
	public void save(File file) throws IOException;

	/**
	 * Saves the contents of this data container to the file at the specified {@link Path}.
	 * 
	 * @param path
	 *            the file path, not <code>null</code>
	 * @throws IOException
	 *             if the file cannot be written to
	 */
	public void save(Path path) throws IOException;

	/**
	 * Writes the contents of this data container to the specified {@link Writer}.
	 * <p>
	 * This operation closes the given {@link Writer} after the contents have been written.
	 * 
	 * @param writer
	 *            the writer, not <code>null</code>
	 * @throws IOException
	 *             if the writer cannot be written to
	 */
	public void save(Writer writer) throws IOException;

	/**
	 * Saves the contents of this data container to a String.
	 * 
	 * @return the contents of this data container as a String, not <code>null</code>
	 */
	public String saveToString();
}
