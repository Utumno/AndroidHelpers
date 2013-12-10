package gr.uoa.di.android.helpers.files;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public final class Reader {

	private Reader() {}

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

	private static byte[] readFile(final String filename,
			final InputStreamAction action) throws IOException {
		InputStream stream = new FileInputStream(filename);
		try {
			return action.useStream(stream);
		} finally {
			FileIO.close(stream);
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
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		final int length = 8192;
		byte[] buffer = new byte[length];
		int read;
		while ((read = stream.read(buffer, 0, length)) > 0) {
			baos.write(buffer, 0, read);
		}
		return baos.toByteArray();
	}
}
