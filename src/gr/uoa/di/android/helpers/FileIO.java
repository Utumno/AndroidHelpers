package gr.uoa.di.android.helpers;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * WIP ! I use the Execute around idiom -
 * http://stackoverflow.com/questions/341971/what-is-the-execute-around-idiom
 * Have kept its use private - one must make OutputStreamAction and
 * InputStreamAction public - I also need methods returning all kinds of things
 * - working on it. TODO docs
 *
 * @author MrD
 */
public final class FileIO {

	private FileIO() {}

	private static final String TAG = FileIO.class.getSimpleName();
	private static final int OUTPUT_BUFFER_SIZE = 8192;
	private static final boolean APPEND = true;
	private static final boolean WARN = false;

	// =========================================================================
	// Read file from external storage - uses the Java API for files
	// =========================================================================
	public static String read(final String filename, final String csName)
			throws IOException {
		final Charset cs = Charset.forName(csName);
		final byte[] ba = readFile(filename, new InputStreamAction());
		return cs.newDecoder().decode(ByteBuffer.wrap(ba)).toString();
	}

	public static byte[] read(final String filename) throws IOException {
		return readFile(filename, new InputStreamAction());
	}

	// =========================================================================
	// Write file in external storage - uses the Java API for files
	// =========================================================================
	/**
	 * Appends to file the given string. Will create the file if not existent.
	 * It uses Java 6 API - not the android calls - for getting the various
	 * predefined directories. So should be used for writing to external storage
	 * only
	 *
	 * @param file
	 *            the File instance to write to
	 * @param data
	 *            the string to be written
	 * @param charsetName
	 *            the name of the charset the string is encoded in
	 * @throws UnsupportedEncodingException
	 *             if the string cannot be encoded to bytes in the given charset
	 * @throws IOException
	 *             if an error occurs during the write operation (including
	 *             flushing the stream)
	 */
	public static void append(final File file, final String data,
			final String charsetName) throws UnsupportedEncodingException,
			IOException {
		byte[] bytes = data.getBytes(charsetName);
		_write(file, bytes, APPEND);
	}

	/**
	 * Appends to file the given bytes. Will create the file if not existent. It
	 * uses Java 6 API - not the android calls - for getting the various
	 * predefined directories. So should be used for writing to external storage
	 * only
	 *
	 * @param file
	 *            the File instance to write to
	 * @param bytes
	 *            the bytes to be written
	 * @throws IOException
	 *             if an error occurs during the write operation (including
	 *             flushing the stream)
	 */
	public static void append(final File file, final byte[] bytes)
			throws FileNotFoundException, IOException {
		_write(file, bytes, APPEND);
	}

	/**
	 * Writes to file the given string. Will create the file if not existent. If
	 * append is false and the file exists it will be truncated. It uses Java 6
	 * API - not the android calls - for getting the various predefined
	 * directories. So should be used for writing to external storage only
	 *
	 * @param file
	 *            the File instance to write to
	 * @param data
	 *            the string to be written
	 * @param charsetName
	 *            the name of the charset the string is encoded in
	 * @param append
	 *            if true it will append to the file - otherwise the file will
	 *            be truncated if it exists
	 * @throws UnsupportedEncodingException
	 *             if the string cannot be encoded to bytes in the given charset
	 * @throws IOException
	 *             if an error occurs during the write operation (including
	 *             flushing the stream)
	 */
	public static void write(final File file, final String data,
			final String charsetName, final boolean append)
			throws UnsupportedEncodingException, IOException {
		byte[] bytes = data.getBytes(charsetName);
		_write(file, bytes, append);
	}

	/**
	 * Writes to file the given bytes. Will create the file if not existent. If
	 * append is false and the file exists it will be truncated. It uses Java 6
	 * API - not the android calls - for getting the various predefined
	 * directories. So should be used for writing to external storage only
	 *
	 * @param file
	 *            the File instance to write to
	 * @param data
	 *            the bytes to be written
	 * @param append
	 *            if true it will append to the file - otherwise the file will
	 *            be truncated if it exists
	 * @throws IOException
	 *             if an error occurs during the write operation (including
	 *             flushing the stream)
	 */
	public static void write(final File file, final byte[] bytes,
			final boolean append) throws IOException {
		_write(file, bytes, append);
	}

	/**
	 * The core method that actually performs the write - TODO : do I need a
	 * BufferedOutputStream ? Should I let the client close it ? see :
	 *
	 * @param file
	 * @param bytes
	 * @param append
	 *            if true it will append to the file - otherwise the file will
	 *            be truncated if it exists
	 *
	 * @throws IOException
	 *             if an error occurs attempting to flush the
	 *             BufferedOutputStream or if an error occurs during the write
	 *             operation
	 */
	private static void _write(final File file, final byte[] bytes,
			final boolean append) throws IOException {
		writeToFile(file, new OutputStreamAction() {

			@Override
			public void useStream(final OutputStream stream) throws IOException {
				BufferedOutputStream buffer = new BufferedOutputStream(stream,
					OUTPUT_BUFFER_SIZE);
				// FIXME writes them all ?
				buffer.write(bytes);
				w(file.getAbsolutePath());
				// You do typically need to flush the decorator in the
				// non-exception case - see :
				// http://stackoverflow.com/a/2732760/281545
				buffer.flush();
			}
		}, append);
	}

	// =========================================================================
	// Write to internal storage in directory - apparently with standard Java IO
	// =========================================================================
	public static void append(final Context context, final String filename,
			final String dirname, final byte[] bytes)
			throws FileNotFoundException, IOException {
		final File dir = createDirInternal(context, dirname); // throws
		final File file = new File(dir.getAbsolutePath(), filename);
		append(file, bytes);
	}

	// =========================================================================
	// Application's storage - those methods need a Context
	// =========================================================================
	/**
	 * Append the data to the file named filename written in the application's
	 * internal storage
	 *
	 * @param context
	 * @param filename
	 * @param data
	 * @param charsetName
	 * @param mode
	 *
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static void append(final Context context, final String filename,
			final String data, final String charsetName, final int mode)
			throws FileNotFoundException, IOException {
		_write(context, filename, data, mode | Context.MODE_APPEND, charsetName);
	}

	public static void write(final Context context, final String filename,
			final String data, final String charsetName,
			final int writeOrAppendMode) throws FileNotFoundException,
			IOException {
		_write(context, filename, data, writeOrAppendMode, charsetName);
	}

	private static void _write(final Context ctx, final String filename,
			final String data, final int mode, final String charsetName)
			throws IOException {
		writeFileInternal(ctx, filename, new OutputStreamAction() {

			@Override
			public void useStream(final OutputStream stream) throws IOException {
				OutputStreamWriter wrt = new OutputStreamWriter(stream,
					charsetName);
				BufferedWriter buffer = new BufferedWriter(wrt,
					OUTPUT_BUFFER_SIZE);
				buffer.write(data);
				buffer.flush();
			}
		}, mode);
	}

	// =========================================================================
	// Android IO methods wrappers
	// =========================================================================
	public static void deleteFile(final Context context, final String filename) {
		context.deleteFile(filename);
	}

	/**
	 * Wrapper around {@link Environment#getExternalStorageState()}.
	 *
	 * @return true if External Storage is both available and writable
	 * @see <a
	 *      href=http://developer.android.com/guide/topics/data/data-storage.html
	 *      #filesExternal>Using the External Storage</a>
	 */
	public static boolean isExternalStoragePresent() {
		boolean externalStorageAvailable = false;
		boolean externalStorageWriteable = false;
		String state = Environment.getExternalStorageState();
		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			externalStorageAvailable = externalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			externalStorageAvailable = true;
			externalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need to know is we can neither read nor write
			externalStorageAvailable = externalStorageWriteable = false;
		}
		return (externalStorageAvailable) && (externalStorageWriteable);
	}

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
		File directory = new File(ctx.getFilesDir(), directoryName);
		if (directory.mkdirs() || directory.isDirectory()) return directory;
		throw new IOException("Cannot create directory " + directoryName
			+ " in internal storage");
	}

	/**
	 * Creates the File instance where the data is persisted in *external
	 * storage* (debugging purposes). This is the external storage private to
	 * the application meaning the files will be removed on application
	 * uninstall. If the external folder is not available or if the directory
	 * can not be created an exception is thrown forcing the caller to deal with
	 * it. External storage must be used sparingly
	 *
	 * @param ctx
	 *            a context belonging to the application
	 * @param rootDir
	 *            the root directory of the file to be created relative to
	 *            getExternalFilesDir(null) - do not give an absolute path
	 * @param filename
	 * @return the File instance corresponding to the rootPath/filename relative
	 *         to External Files dir for the application storage or null on
	 *         failure
	 * @throws IOException
	 *             if the external storage is unavailable or the dir can't be
	 *             created
	 */
	public static File fileExternalApplicationStorage(final Context ctx,
			final String rootDir, final String filename) throws IOException {
		// create a File object for the parent directory
		if (isExternalStoragePresent()) {
			File logdir = new File(ctx.getExternalFilesDir(null)
				.getAbsolutePath() + File.separator + rootDir);
			// have the object build the directory structure, if needed.
			if (FileIO.createDirExternal(logdir)) {
				// Log.w(TAG, "logdir : " + logdir.getAbsolutePath());
				// create a *File object* for the output file - can't fail
				return new File(logdir, filename);
			}
			throw new IOException("Can not create folder "
				+ logdir.getAbsolutePath());
		}
		throw new IOException("External storage not present or not writable");
	}

	/**
	 * Creates the File instance where the data is persisted in *external
	 * storage* along with user files (debugging purposes *but not in
	 * production*). The files will NOT be removed on application uninstall - so
	 * use this while installing and uninstalling to keep the logs around. If
	 * the external folder is not available or if the directory can not be
	 * created an exception is thrown forcing the caller to deal with it.
	 * External storage must be used sparingly
	 *
	 * @param rootDir
	 *            the directory the file will be created relative to the
	 *            directory returned depending on the value of
	 *            {@code dirInPublicStorage} - do not give an absolute path
	 * @param filename
	 * @param dirInPublicStorage
	 *            specifies the root relative to which the {@code rootDir}/
	 *            {@code filename} path is relative. If this is one of
	 *            DIRECTORY_MUSIC, DIRECTORY_PODCASTS, DIRECTORY_RINGTONES,
	 *            DIRECTORY_ALARMS, DIRECTORY_NOTIFICATIONS, DIRECTORY_PICTURES,
	 *            DIRECTORY_MOVIES, DIRECTORY_DOWNLOADS, or DIRECTORY_DCIM then
	 *            the relevant directory is returned (as per the docs of
	 *            {@code getExternalStoragePublicDirectory()}). If it is null
	 *            then the root of external storage is returned as per
	 *            {@code getExternalStorageDirectory()}. See the section
	 *            "Accessing files on external storage" in {@link http
	 *            ://developer.android.com/guide/topics/data/data-storage.html}
	 * @return the File instance corresponding to the rootPath/filename relative
	 *         to External Files dir for the application storage or null on
	 *         failure
	 * @throws IOException
	 *             if the external storage is unavailable or the dir can't be
	 *             created
	 */
	public static File fileExternalPublicStorage(final String rootDir,
			final String filename, final String dirInPublicStorage)
			throws IOException {
		// create a File object for the parent directory
		if (isExternalStoragePresent()) {
			File logdir;
			if (dirInPublicStorage != null) {
				logdir = new File(Environment
					.getExternalStoragePublicDirectory(dirInPublicStorage)
					.getAbsolutePath()
					+ File.separator + rootDir);
			} else {
				logdir = new File(Environment.getExternalStorageDirectory()
					.getAbsolutePath() + File.separator + rootDir);
			}
			// have the object build the directory structure, if needed.
			if (FileIO.createDirExternal(logdir)) {
				// create a File *instance* for the output file - can't fail
				return new File(logdir, filename);
			}
			throw new IOException("Can not create folder "
				+ logdir.getAbsolutePath());
		}
		throw new IOException("External storage not present or not writable");
	}

	/**
	 * Copies the inputFilename (an absolute path) to destinationFilename (a
	 * filename) in *external public storage* in the directory destinationDir
	 * relative to the root of external public storage
	 *
	 * @param inputFilename
	 *            must be an *absolute path* to a file in internal storage
	 * @param destinationDir
	 * @param destinationFilename
	 * @throws FileNotFoundException
	 *             if either the destination or the inputFileName strings do not
	 *             point to an existent path
	 * @throws IOException
	 *             if the copying operation fails
	 */
	public static void copyFileFromInternalToExternalStorage(
			String inputFilename, String destinationDir,
			String destinationFilename) throws IOException,
			FileNotFoundException {
		FileInputStream fis = new FileInputStream(inputFilename);
		try {
			File dest = fileExternalPublicStorage(destinationDir,
				destinationFilename, null);
			OutputStream output = new FileOutputStream(dest);
			try {
				// transfer bytes from the inputfile to the outputfile
				byte[] buffer = new byte[1024];
				int length;
				while ((length = fis.read(buffer)) > 0) {
					output.write(buffer, 0, length);
				}
			} finally {
				close(output);
			}
		} finally {
			close(fis);
		}
	}

	// =========================================================================
	// IO Utils
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
	public static long sizeOfDirectory(File directory) {
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
		for (int i = 0; i < files.length; i++) {
			File file = files[i];
			if (file.isDirectory()) size += sizeOfDirectory(file);
			else size += file.length();
		}
		return size;
	}

	/**
	 * Returns true if the directory exists and is not empty *OR* if it does not
	 * exist. Will also return true if the directory is security restricted.
	 * Will throw IllegalArgumentException if the directory is not a directory.
	 *
	 * @param directory
	 *            must be a directory
	 * @return true if the directory does not exist or is empty or not
	 *         accessible, false otherwise
	 * @throws IllegalArgumentException
	 *             if the directory is not a directory
	 */
	public static boolean isEmptyOrAbsent(File directory) {
		if (!directory.exists()) {
			return true;
		}
		return sizeOfDirectory(directory) == 0;
	}

	public static List<File> listFiles(File directory) {
		if (!directory.exists()) {
			String message = directory + " does not exist";
			throw new IllegalArgumentException(message);
		}
		if (!directory.isDirectory()) {
			String message = directory + " is not a directory";
			throw new IllegalArgumentException(message);
		}
		return Arrays.asList(directory.listFiles());// empty array => empty List
	}

	public static boolean delete(final File file) {
		return file.delete();
	}

	/**
	 * Given a File which corresponds to a _directory_ path creates this path if
	 * it does not exists. The directory path must lie in EXTERNAL storage
	 *
	 * @param directory
	 *            the File instance whose path must be created
	 * @return true if the path already exists and is a directory or was created
	 *         successfully as a directory, false otherwise
	 */
	public static boolean createDirExternal(final File directory) {
		return directory.isDirectory() || directory.mkdirs();
	}

	// =========================================================================
	// Helpers
	// =========================================================================
	private static void close(final Closeable closeable) {
		if (closeable == null)
			throw new NullPointerException("Trying to close a null Closeable");
		try {
			closeable.close();
		} catch (IOException e) {
			w("Exception thrown while closing " + closeable, e);
		}
	}

	private static void w(String string, Throwable t) {
		if (WARN) Log.w(TAG, string, t);
	}

	private static void w(final String string) {
		if (WARN) Log.w(TAG, string);
	}

	// =========================================================================
	// Private execute around methods - the data is supplied in the public API
	// =========================================================================
	/**
	 * Writes to file and closes it. Will create the file if not existent. It
	 * uses Java 6 API - not the android calls - for getting the various
	 * predefined directories. So should be used for writing to external storage
	 * only
	 *
	 * @param file
	 *            the File instance to write to
	 * @param action
	 *            should be a write action
	 * @param append
	 *            if true it will append to the file - otherwise the file will
	 *            be truncated if it exists
	 * @throws IOException
	 */
	private static void writeToFile(final File file,
			final OutputStreamAction action, final boolean append)
			throws IOException {
		OutputStream stream = new FileOutputStream(file, append);
		try {
			action.useStream(stream);
		} finally {
			close(stream);
		}
	}

	private static byte[] readFile(final String filename,
			final InputStreamAction action) throws IOException {
		InputStream stream = new FileInputStream(filename);
		try {
			return action.useStream(stream);
		} finally {
			close(stream);
		}
	}

	/**
	 * Writes to a FileOutputStream and closes it. The stream is retrieved via
	 * the openFileOutput() method for the given context. Notice the file won't
	 * be accessible either on DDMS or the phone filesystem on a non rooted
	 * phone. It is saved in internal storage. To get the directory the file is
	 * saved to call ctx.getFilesDir().getPath() on the context passed in (see
	 * stackoverflow.com/questions/4926027/what-file-system-path-is-used-by
	 * -androids-context-openfileoutput)
	 *
	 * @param ctx
	 *            the context openFileOutput() will be called upon
	 * @param filename
	 *            the filename of the file to write to
	 * @param action
	 *            should be a write action
	 * @param mode
	 *            should be one of the modes supported by openFileOutput()
	 *
	 * @throws IOException
	 *             if some IO operation failed (including opening the stream but
	 *             not closing it)
	 * @throws IllegalArgumentException
	 *             if the passed in mode is not a valid openFileOutput() mode
	 */
	private static void writeFileInternal(final Context ctx,
			final String filename, final OutputStreamAction action,
			final int mode) throws IOException {
		assertValidMode(mode);
		OutputStream stream = ctx.openFileOutput(filename, mode);
		try {
			action.useStream(stream);
		} finally {
			close(stream);
		}
	}

	private static void assertValidMode(final int mode) {
		@SuppressWarnings("deprecation")
		final int allModes = Context.MODE_WORLD_READABLE
			| Context.MODE_WORLD_WRITEABLE | Context.MODE_APPEND
			| Context.MODE_PRIVATE;
		// w(Integer.toBinaryString(allModes));
		int modeOr = mode | allModes;
		// w(Integer.toBinaryString(modeOr));
		// w(Integer.toBinaryString(mode));
		int modeXor = modeOr ^ allModes;
		if (modeXor != 0) {
			throw new IllegalArgumentException("Invalid mode : " + mode);
		}
	}
}

class InputStreamAction {

	/**
	 * Based on code by Skeet for reading a file to a string :
	 * http://stackoverflow.com/a/326531/281545
	 *
	 * @param stream
	 * @return
	 * @throws IOException
	 */
	byte[] useStream(final InputStream stream) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final int length = 8192;
		byte[] buffer = new byte[length];
		int read;
		while ((read = stream.read(buffer, 0, length)) > 0) {
			baos.write(buffer, 0, read);
		}
		return baos.toByteArray();
	}
}

interface OutputStreamAction {

	void useStream(final OutputStream stream) throws IOException;
}
