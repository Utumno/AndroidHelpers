package gr.uoa.di.android.helpers;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import gr.uoa.di.java.helpers.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.util.zip.GZIPOutputStream;

/**
 * Needed manifest permissions :
 *
 * <uses-permission android:name="android.permission.INTERNET" />
 * <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 *
 * Test functions :
 *
 * Utils.getMACAddress("wlan0"); Utils.getMACAddress("eth0");
 * Utils.getIPAddress(true); // IPv4 Utils.getIPAddress(false); // IPv6
 *
 * @see <a href= "http://stackoverflow.com/questions/6064510>How to get
 *      ip-address of the device</a >
 */
public final class Net {

	private Net() {}

	private static final String TAG = Net.class.getName();
	// multipart values
	private static final CharSequence CRLF = "\r\n";
	private static final String charsetForMultipartHeaders = Utils.UTF8;

	public static boolean isWifiConnected(Context ctx) {
		ConnectivityManager connec = (ConnectivityManager) ctx
			.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi.isAvailable() && wifi.isConnected();
	}

	public static boolean isWifiConnectedOrConnecting(Context ctx) {
		ConnectivityManager connec = (ConnectivityManager) ctx
			.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi.isAvailable() && wifi.isConnectedOrConnecting();
	}

	// =========================================================================
	// Multipart
	// =========================================================================
	public static void flushMultiPartData(File file,
			OutputStream serverOutputStream, String boundary, boolean isGunzip)
			throws FileNotFoundException, IOException {
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
			if (writer != null) writer.close();
		}
	}

	public static void appendBinary(File file, String boundary,
			PrintWriter writer, OutputStream output, boolean isGzip)
			throws FileNotFoundException, IOException {
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

	@SuppressWarnings("unused")
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

	@SuppressWarnings("unused")
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

	// =========================================================================
	// helpers
	// =========================================================================
	private static void w(String message) {
		Log.w(TAG, message);
	}
}
