package gr.uoa.di.android.helpers.files;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Class for writing to files. Uses the <a href=
 * "http://stackoverflow.com/questions/341971/what-is-the-execute-around-idiom"
 * >Execute around idiom</a>. I only kept the append methods I use. Commented
 * out are more generic write methods as well as some wrappers around internal
 * (application) files
 */
public final class Writer {

	private static final boolean APPEND = true;
	private static final int OUTPUT_BUFFER_SIZE = 8192;

	private Writer() {}

	// =========================================================================
	// Write file using the Java 6 API for files
	// =========================================================================
	/**
	 * Appends to file the given string. Will create the file if not existent.
	 * Notice that if the encoding is not suitable for the given string the
	 * string will be corrupted
	 *
	 * @param file
	 *            the File instance to write to
	 * @param data
	 *            the string to be written
	 * @param charsetName
	 *            the name of the charset the string is encoded in
	 * @throws UnsupportedEncodingException
	 *             if the encoding given is not supported by the JVM
	 * @throws IOException
	 *             if an error occurs during the write operation (including
	 *             flushing the stream)
	 */
	public static void append(final File file, final String data,
			final String charsetName) throws UnsupportedEncodingException,
			IOException {
		final byte[] bytes = data.getBytes(charsetName);
		_write(file, bytes, APPEND);
	}

	/**
	 * Appends to file the given bytes. Will create the file if not existent.
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

	// @formatter:off
	/**
	 * Writes to file the given string. Will create the file if not existent. If
	 * append is false and the file exists it will be truncated. Notice that if
	 * the encoding is not suitable for the given string the string will be
	 * corrupted
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
	 *             if the encoding given is not supported by the JVM
	 * @throws IOException
	 *             if an error occurs during the write operation (including
	 *             flushing the stream)
	 */
	/*public static void write(final File file, final String data,
			final String charsetName, final boolean append)
			throws UnsupportedEncodingException, IOException {
		final byte[] bytes = data.getBytes(charsetName);
		_write(file, bytes, append);
	}*/

	/**
	 * Writes to file the given bytes. Will create the file if not existent. If
	 * append is false and the file exists it will be truncated.
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
	/*public static void write(final File file, final byte[] bytes,
			final boolean append) throws IOException {
		_write(file, bytes, append);
	}*/

	// =========================================================================
	// Write to internal storage in directory - apparently with standard Java IO
	// =========================================================================
	/*public static void append(final Context context, final String filename,
			final String dirname, final byte[] bytes)
			throws FileNotFoundException, IOException {
		final File dir = FileUtils.createDirInternal(context, dirname); // throws
		final File file = new File(dir.getAbsolutePath(), filename);
		append(file, bytes);
	}*/

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
	/*public static void append(final Context context, final String filename,
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
	}*/

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
	/*public static void writeFileInternal(final Context ctx,
			final String filename, final OutputStreamAction action,
			final int mode) throws IOException {
		assertValidMode(mode);
		OutputStream stream = ctx.openFileOutput(filename, mode);
		try {
			action.useStream(stream);
		} finally {
			FileUtils.close(stream);
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
	}*/
	// @formatter:on
	// =========================================================================
	// Private execute around methods - the data is supplied in the public API
	// =========================================================================
	/**
	 * The core method that actually performs the write - TODO : do I need a
	 * BufferedOutputStream ? Should I let the client close it ?
	 *
	 * @param file
	 * @param bytes
	 * @param append
	 *            if true it will append to the file - otherwise the file will
	 *            be truncated if it exists
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
				BufferedOutputStream baos = new BufferedOutputStream(stream,
					OUTPUT_BUFFER_SIZE);
				baos.write(bytes); // <=> write(bytes, 0, bytes.length)
				// You do typically need to flush the decorator in the
				// non-exception case - see :
				// http://stackoverflow.com/a/2732760/281545
				baos.flush();
			}
		}, append);
	}

	/**
	 * Writes to file and closes it. Will create the file if not existent.
	 *
	 * @param file
	 *            the File instance to write to
	 * @param action
	 *            should be a write action
	 * @param append
	 *            if true it will append to the file - otherwise the file will
	 *            be truncated if it exists
	 * @throws IOException
	 *             if the file cannot be opened for writing
	 *             (FileNotFoundException) or if an IO error occurs during the
	 *             action
	 */
	private static void writeToFile(final File file,
			final OutputStreamAction action, final boolean append)
			throws IOException {
		final OutputStream stream = new FileOutputStream(file, append);
		try {
			action.useStream(stream);
		} finally {
			FileUtils.close(stream);
		}
	}
}

interface OutputStreamAction {

	void useStream(final OutputStream stream) throws IOException;
}
