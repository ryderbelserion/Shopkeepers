package com.nisovin.shopkeepers.util.java;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.logging.Logger;

import com.nisovin.shopkeepers.util.logging.NullLogger;

/**
 * File related utilities.
 * <p>
 * Several methods in this class wrap underlying IO exceptions into IO exceptions that replicate the original exception
 * messages but prepend it with a more general and therefore user-friendly description of the failed operation.
 */
public final class FileUtils {

	/**
	 * Checks if the specified file is {@link Files#isWritable(Path) writable} and throws an {@link IOException} if this
	 * is not the case.
	 * <p>
	 * This assumes that the given path refers to an existing file.
	 * 
	 * @param file
	 *            the file path
	 * @throws IOException
	 *             if the file is not writable
	 */
	public static void checkIsFileWritable(Path file) throws IOException {
		if (!Files.isWritable(file)) {
			throw new IOException("Missing write permission for file " + file);
		}
	}

	/**
	 * Checks if the specified directory is both {@link Files#isWritable(Path) writable} and
	 * {@link Files#isExecutable(Path) executable} (i.e. accessible) and throws an {@link IOException} if this is not
	 * the case.
	 * <p>
	 * This assumes that the given path refers to an existing directory.
	 * <p>
	 * This set of permissions is for example required to rename files within the directory.
	 * 
	 * @param directory
	 *            the directory path
	 * @throws IOException
	 *             if the directory is not writable or executable (i.e. accessible)
	 */
	public static void checkIsDirectoryWritable(Path directory) throws IOException {
		if (!Files.isWritable(directory)) {
			throw new IOException("Missing write permission for directory " + directory);
		}
		try {
			if (!Files.isExecutable(directory)) {
				throw new IOException("Missing execute (i.e. access) permission for directory " + directory);
			}
		} catch (SecurityException e) {
			// Some SecurityManager implementations blindly deny the 'execute' permission without differentiating
			// between files and directories. This should have no effect on whether we can write to the directory, so we
			// can safely ignore it.
		}
	}

	/**
	 * Ensures that any pending writes to the file at the specified path are physically persisted to the underlying
	 * storage device (i.e. instructs the operating system to flush any write buffers related to that file).
	 * <p>
	 * For some file / operating systems it may also be required to invoke fsync on directories to ensure that the names
	 * of any contained newly created or renamed files have been properly persisted. However, not all operating systems
	 * (e.g. Windows) support to open or fsync directories. We therefore ignore any thrown {@link IOException} if the
	 * given path points to a directory.
	 * 
	 * @param path
	 *            the file path
	 * @throws IOException
	 *             if the operation fails
	 */
	public static void fsync(Path path) throws IOException {
		// References regarding data consistency and fsync:
		// http://blog.httrack.com/blog/2013/11/15/everything-you-always-wanted-to-know-about-fsync/
		// https://thunk.org/tytso/blog/2009/03/15/dont-fear-the-fsync/
		// http://danluu.com/file-consistency/
		boolean isDirectory = Files.isDirectory(path); // Only true if the directory exists
		// Directories are opened in read-only mode, whereas regular files require write mode:
		StandardOpenOption fileAccess = isDirectory ? StandardOpenOption.READ : StandardOpenOption.WRITE;
		// Note: This also checks for file existence.
		try (FileChannel file = FileChannel.open(path, fileAccess)) {
			file.force(true);
		} catch (IOException e) {
			if (isDirectory) {
				// Ignored for directories, since this is not supported on all operating systems (e.g. Windows):
				return;
			}
			throw new IOException("Could not fsync file '" + path + "': " + ThrowableUtils.getDescription(e), e);
		}
	}

	/**
	 * Invokes {@link #fsync(Path)} for the given path's parent, if it has a parent.
	 * 
	 * @param path
	 *            the path
	 * @throws IOException
	 *             if the operation fails
	 */
	public static void fsyncParentDirectory(Path path) throws IOException {
		Validate.notNull(path, "path is null");
		Path parent = path.getParent();
		if (parent != null) {
			fsync(parent);
		}
	}

	/**
	 * Creates the specified directory and any not yet existing parents.
	 * <p>
	 * Does not throw an exception if the directory already exists.
	 * 
	 * @param directory
	 *            the directory path
	 * @throws IOException
	 *             if the operation fails
	 * @see Files#createDirectories(Path, java.nio.file.attribute.FileAttribute...)
	 */
	public static void createDirectories(Path directory) throws IOException {
		try {
			// This does nothing if the directory already exists.
			Files.createDirectories(directory);
		} catch (IOException e) {
			throw new IOException("Could not create directory '" + directory + "': " + ThrowableUtils.getDescription(e), e);
		}
	}

	/**
	 * Creates all not yet existing parent directories for the specified path.
	 * <p>
	 * Does not throw an exception if the parent directories already exist. Does nothing if the specified path does not
	 * have a parent.
	 * 
	 * @param path
	 *            the path
	 * @throws IOException
	 *             if the operation fails
	 */
	public static void createParentDirectories(Path path) throws IOException {
		Path parent = path.getParent(); // Can be null
		if (parent != null) {
			createDirectories(parent);
		}
	}

	/**
	 * Deletes the specified file.
	 * 
	 * @param path
	 *            the file path
	 * @throws IOException
	 *             if the operation fails
	 * @see Files#delete(Path)
	 */
	public static void delete(Path path) throws IOException {
		try {
			Files.delete(path);
		} catch (IOException e) {
			throw new IOException("Could not delete file '" + path + "': " + ThrowableUtils.getDescription(e), e);
		}
	}

	/**
	 * Deletes the specified file if it exists.
	 * 
	 * @param path
	 *            the file path
	 * @return <code>true</code> if the file existed and has been removed, <code>false</code> if it did not exist
	 * @throws IOException
	 *             if the operation fails
	 * @see Files#deleteIfExists(Path)
	 */
	public static boolean deleteIfExists(Path path) throws IOException {
		try {
			return Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new IOException("Could not delete file '" + path + "': " + ThrowableUtils.getDescription(e), e);
		}
	}

	/**
	 * Moves the specified source file to the given target path, replacing any already existing file at that path.
	 * <p>
	 * This attempts to atomically rename the file, but may fall back to a non-atomic move operation. In the latter
	 * case, any occurring IO exception or severe system failure (crash, power loss, etc.) may leave the target file in
	 * an undefined state. This logs a warning for each attempted fallback solution using the given {@link Logger}.
	 * <p>
	 * To account for transient issues that may occasionally prevent this operation from succeeding (such as in the
	 * presence of other processes concurrently interacting with these files), it is recommended to wrap this operation
	 * into a suitable retry loop.
	 * <p>
	 * This method does not guarantee that the file name changes are actually persisted to disk once the method returns.
	 * To ensure that the caller has to subsequently invoke {@link #fsync(Path)} on the directory containing the target
	 * path.
	 * 
	 * @param source
	 *            the path of the source file
	 * @param target
	 *            the path of the target file
	 * @param logger
	 *            the logger used to log warnings when atomic moving is not possible, can be {@link NullLogger} to not
	 *            log anything
	 * @throws IOException
	 *             if the operation fails for some reason and we are not able to recover by applying some fallback
	 */
	public static void moveFile(Path source, Path target, Logger logger) throws IOException {
		Validate.notNull(source, "source is null");
		Validate.notNull(target, "target is null");
		Validate.notNull(logger, "logger is null");

		// Create the parent directories if necessary:
		createParentDirectories(target);

		try {
			// Attempt atomic move / rename:
			try {
				Files.move(source, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				// Attempt non-atomic move:
				// TODO Turn this into a debug message? Might spam if this is logged repeatedly on a system that is
				// known to not support atomic moves. Or maybe only print this once as warning, and then only in debug
				// mode.
				logger.warning(() -> "Could not atomically move file '" + source + "' to '" + target
						+ "' (" + ThrowableUtils.getDescription(e) + ")! Attempting non-atomic move.");
				Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			// Attempt File#renameTo(File):
			if (!source.toFile().renameTo(target.toFile())) {
				// Attempt copy and delete:
				// TODO Turn this into a debug message?
				logger.warning(() -> "Could not move file '" + source + "' to '" + target + "' ("
						+ ThrowableUtils.getDescription(e) + ")! Attempting copy and delete.");
				try {
					copyAndDelete(source, target);
				} catch (IOException e2) {
					throw new IOException("Could not copy-and-delete file '" + source + "' to '" + target + "': "
							+ ThrowableUtils.getDescription(e2), e2);
				}
			}
		}
	}

	private static void copyAndDelete(Path source, Path target) throws IOException {
		assert source != null && target != null;
		Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
		// Ensure that the data has been written to disk before we remove the source file:
		fsync(target);
		// Also fsync the containing directory since we might have freshly created the target file:
		fsyncParentDirectory(target);
		delete(source);
	}

	/**
	 * Opens or creates a file for writing.
	 * <p>
	 * This mimics {@link Files#newBufferedWriter(Path, Charset, OpenOption...)}, but returns an unbuffered
	 * {@link Writer}.
	 * 
	 * @param path
	 *            the path to the file
	 * @param cs
	 *            the charset to use for encoding
	 * @param options
	 *            options specifying how the file is opened
	 * @return the unbuffered writer to write text to the file
	 * @throws IOException
	 *             see {@link Files#newBufferedWriter(Path, Charset, OpenOption...)}
	 */
	public static Writer newUnbufferedWriter(Path path, Charset cs, OpenOption... options) throws IOException {
		// Unlike the OutputStreamWriter constructor that accepts a Charset directly, which creates an encoder that
		// removes or replaces invalid data from the input, this encoder throws exceptions when it encounters invalid
		// data.
		CharsetEncoder encoder = cs.newEncoder();
		Writer writer = new OutputStreamWriter(Files.newOutputStream(path, options), encoder);
		return writer;
	}

	private FileUtils() {
	}
}
