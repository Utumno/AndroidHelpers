package gr.uoa.di.android.helpers.files;

import android.content.Context;
import android.util.Log;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/** Common File IO Utils */
public final class FileIO {

	private FileIO() {}

	private static final String TAG = FileIO.class.getSimpleName();
	private static final boolean WARN = false;

	// =========================================================================
	// Android IO methods wrappers
	// =========================================================================
	/**
	 * Given a string which corresponds to a directory path relative to the root
	 * of *internal* storage creates this directory path if it does not exists.
	 * Returns this path or throws IOException on failure
	 *
	 * @param directoryName
	 *            the path to the desired directory *relative* to the root of
	 *            *internal* storage
	 * @return the File that corresponds to the path if it was created
	 *         successfully or the path already existed and is a directory
	 * @throws IOException
	 */
	public static File createDirInternal(final Context ctx,
			final String directoryName) throws IOException {
		final File directory = new File(ctx.getFilesDir(), directoryName);
		if (createDir(directory)) return directory;
		throw new IOException("Cannot create directory " + directoryName
			+ " in internal storage");
	}

	// =========================================================================
	// IO Utils
	// =========================================================================
	/**
	 * Returns true if the directory exists and is empty *OR* if it does not
	 * exist. Will also return true if the directory is security restricted.
	 * Will throw IllegalArgumentException if the directory is not a directory.
	 *
	 * @param directory
	 *            must be a directory
	 * @return true if the directory does not exist or is empty or not
	 *         accessible, false otherwise
	 * @throws NullPointerException
	 *             if the directory is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the directory is not a directory
	 */
	public static boolean isEmptyOrAbsent(final File directory) {
		if (!directory.exists()) {
			return true;
		}
		return sizeOfDirectory(directory) == 0;
	}

	/**
	 * List the files in the directory. Wrapper around {@link File#listFiles()}.
	 *
	 * @param directory
	 *            directory to inspect, must not be <code>null</code>
	 * @return a List<File> of the files contained in the directory
	 * @throws NullPointerException
	 *             if the directory is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the directory does not exist or is not a directory
	 */
	public static List<File> listFiles(final File directory) {
		final File[] listFiles = directory.listFiles();
		if (listFiles == null) {
			String message = directory
				+ ((directory.exists()) ? " is not a directory"
						: " does not exist");
			throw new IllegalArgumentException(message);
		}
		return Arrays.asList(listFiles); // empty array => empty List
	}

	// =========================================================================
	// Package helpers
	// =========================================================================
	/**
	 * Given a File which corresponds to a _directory_ path creates this path if
	 * it does not exists.
	 *
	 * @param directory
	 *            the File instance whose path must be created
	 * @return true if the path already exists and is a directory or was created
	 *         successfully as a directory, false otherwise
	 */
	static boolean createDir(final File directory) {
		return directory.isDirectory() || directory.mkdirs();
	}

	// uses android Logging
	static void close(final Closeable closeable) {
		if (closeable == null)
			throw new NullPointerException("Trying to close a null Closeable");
		try {
			closeable.close();
		} catch (IOException e) {
			w("Exception thrown while closing " + closeable, e);
		}
	}

	// =========================================================================
	// Helpers
	// =========================================================================
	/**
	 * Counts the size of a directory recursively (sum of the length of all
	 * files). Modifies Apache commons <a href=
	 * "http://commons.apache.org/proper/commons-io/javadocs/api-release/org/apache/commons/io/FileUtils.html#sizeOfDirectory%28java.io.File%29"
	 * >FileUtils#sizeOfDirectory(java.io.File)</a> TODO : <a
	 * href="http://stackoverflow.com/a/3169970/281545">circular paths</a>
	 *
	 * @param directory
	 *            directory to inspect, must not be <code>null</code>
	 * @return size of directory in bytes, 0 if directory is security restricted
	 * @throws NullPointerException
	 *             if the directory is <code>null</code>
	 * @throws IllegalArgumentException
	 *             if the directory does not exist or is not a directory
	 */
	private static long sizeOfDirectory(File directory) {
		if (!directory.exists()) {
			String message = directory + " does not exist";
			throw new IllegalArgumentException(message);
		}
		if (!directory.isDirectory()) {
			String message = directory + " is not a directory";
			throw new IllegalArgumentException(message);
		}
		long size = 0;
		File[] files = directory.listFiles();
		if (files == null) { // null if security restricted
			return 0L;
		}
		for (int i = 0; i < files.length; ++i) {
			File file = files[i];
			if (file.isDirectory()) size += sizeOfDirectory(file);
			else size += file.length();
		}
		return size;
	}

	private static void w(String string, Throwable t) {
		if (WARN) Log.w(TAG, string, t);
	}
}
