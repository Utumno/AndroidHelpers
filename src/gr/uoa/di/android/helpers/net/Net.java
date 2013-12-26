package gr.uoa.di.android.helpers.net;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import gr.uoa.di.java.helpers.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URLConnection;
import java.util.zip.GZIPOutputStream;

public final class Net {

	private Net() {}

	private static final String TAG = Net.class.getName();
	// multipart values
	private static final CharSequence CRLF = "\r\n";
	private static final String charsetForMultipartHeaders = Utils.UTF8;

	/**
	 * Check if connected to a wireless network
	 *
	 * @param ctx
	 *            Context needed to retrieve the system services
	 * @return true if connected to a wireless network, false otherwise
	 * @throws SecurityException
	 *             if android.permission.ACCESS_NETWORK_STATE is not held
	 */
	public static boolean isWifiConnected(Context ctx) {
		// assertPermission(ctx, permission.ACCESS_NETWORK_STATE);
		ConnectivityManager connec = (ConnectivityManager) ctx
			.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi.isAvailable() && wifi.isConnected();
	}

	/**
	 * Check if connected or in the process of connecting to a wireless network
	 *
	 * @param ctx
	 *            Context needed to retrieve the system services
	 * @return true if connected or in the process of connecting to a wireless
	 *         network, false otherwise
	 * @throws SecurityException
	 *             if android.permission.ACCESS_NETWORK_STATE is not held
	 */
	public static boolean isWifiConnectedOrConnecting(Context ctx) {
		// assertPermission(ctx, permission.ACCESS_NETWORK_STATE);
		ConnectivityManager connec = (ConnectivityManager) ctx
			.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi.isAvailable() && wifi.isConnectedOrConnecting();
	}

	/**
	 * Returns an int for high performance mode of the wifi lock. This was made
	 * public after HONEYCOMB_MR1 but the mode existed before then so...
	 *
	 * @return an int for high performance mode of the wifi lock
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
	public static int modeHighPerformanse() {
		if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
			return WifiManager.WIFI_MODE_FULL_HIGH_PERF;
		}
		return 0x3;
	}

	// =========================================================================
	// Multipart
	// =========================================================================
	/**
	 * Sends a binary file as part of a multipart form data over a socket
	 * connection and closes the output stream of the connection.
	 *
	 * @param file
	 *            the binary file to send
	 * @param serverOutputStream
	 *            the server connection output stream - WILL BE CLOSED by this
	 *            method
	 * @param boundary
	 *            multipart boundary - needed cause must be shared with code
	 *            that creates the server connection
	 * @param isGunzip
	 *            if true the content will be gunzipped
	 * @throws IOException
	 *             if it can't create the writer, the file was not found or an
	 *             IO exception was thrown writing the data
	 */
	public static void flushMultiPartData(File file,
			OutputStream serverOutputStream, String boundary, boolean isGunzip)
			throws IOException {
		// connection.setRequestProperty("accept", "text/html,application/xhtml"
		// + "+xml,application/xml;q=0.9,*/*;q=0.8");
		// TODO : chunks
		PrintWriter writer = null;
		try {
			// http://stackoverflow.com/a/2793153/281545
			// true = autoFlush, important!
			writer = new PrintWriter(new OutputStreamWriter(serverOutputStream,
				charsetForMultipartHeaders), true);
			appendBinary(file, boundary, writer, serverOutputStream, isGunzip);
			// End of multipart/form-data.
			writer.append("--" + boundary + "--").append(CRLF);
		} finally {
			if (writer != null) writer.close(); // closes the serverOutputStream
		}
	}

	private static void appendBinary(File file, String boundary,
			PrintWriter writer, OutputStream output, boolean isGzip)
			throws IOException {
		// Send binary file.
		writer.append("--" + boundary).append(CRLF);
		writer.append(
			"Content-Disposition: form-data; name=\"binaryFile\"; filename=\""
				+ file.getName() + "\"").append(CRLF);
		writer.append(
			"Content-Type: "
				+ ((isGzip) ? "application/gzip" : URLConnection
					.guessContentTypeFromName(file.getName()))).append(CRLF);
		writer.append("Content-Transfer-Encoding: binary").append(CRLF);
		writer.append(CRLF).flush();
		InputStream input = null;
		OutputStream output2 = output;
		if (isGzip) {
			output2 = new GZIPOutputStream(output);
		}
		try {
			input = new FileInputStream(file);
			byte[] buffer = new byte[1024];
			for (int length = 0; (length = input.read(buffer)) > 0;) {
				output2.write(buffer, 0, length);
			}
			if (isGzip) {
				// Write the compressed parts,
				// http://stackoverflow.com/a/18858420/281545
				((GZIPOutputStream) output2).finish();
			}
			output2.flush(); // Important! Output cannot be closed. Close of
			// writer will close output as well.
		} finally {
			if (input != null) try {
				input.close();
			} catch (IOException logOrIgnore) {
				w(logOrIgnore.getMessage());
			}
		}
		writer.append(CRLF).flush(); // CRLF is important! It indicates end of
		// binary boundary.
	}

	// formatter:off
	/*
	private static void appendTextFile(String boundary, PrintWriter writer,
			File textFile) throws UnsupportedEncodingException,
			FileNotFoundException, IOException {
		// Send text file.
		writer.append("--" + boundary).append(CRLF);
		writer.append(
			"Content-Disposition: form-data; name=\"textFile\"; filename=\""
				+ textFile.getName() + "\"").append(CRLF);
		writer.append(
			"Content-Type: text/plain; charset=" + charsetForMultipartHeaders)
			.append(CRLF);
		writer.append(CRLF).flush();
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(
				new FileInputStream(textFile), charsetForMultipartHeaders));
			for (String line; (line = reader.readLine()) != null;) {
				writer.append(line).append(CRLF);
			}
		} finally {
			if (reader != null) try {
				reader.close();
			} catch (IOException logOrIgnore) {
				w(logOrIgnore.getMessage());
			}
		}
		writer.flush();
	}

	private static void appendParameter(String boundary, PrintWriter writer,
			CharSequence param) {
		// Send normal param.
		writer.append("--" + boundary).append(CRLF);
		writer.append("Content-Disposition: form-data; name=\"param\"").append(
			CRLF);
		writer.append(
			"Content-Type: text/plain; charset=" + charsetForMultipartHeaders)
			.append(CRLF);
		writer.append(CRLF);
		writer.append(param).append(CRLF).flush();
	}
	 */
	// formatter:on
	// =========================================================================
	// helpers
	// =========================================================================
	private static void w(String message) {
		Log.w(TAG, message);
	}
}
