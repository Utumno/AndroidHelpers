package gr.uoa.di.android.helpers;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.util.Log;

import gr.uoa.di.java.helpers.Utils;

import org.apache.http.conn.util.InetAddressUtils;

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
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URLConnection;
import java.util.Collections;
import java.util.List;
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

	private static final String TAG = Net.class.getName();
	// multipart values
	private static final CharSequence CRLF = "\r\n";
	private static final String charsetForMultipartHeaders = Utils.UTF8;

	private Net() {}

	/**
	 * Returns MAC address of the given interface name. Use only in GINGERBREAD
	 * and above builds
	 *
	 * @param interfaceName
	 *            eth0, wlan0 or NULL=use first interface
	 * @return mac address or empty string
	 * @throws SocketException
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static String getMACAddress(String interfaceName)
			throws SocketException {
		if (android.os.Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
			throw new IllegalStateException("You can use getMACAddress() only "
				+ "after API " + Build.VERSION_CODES.GINGERBREAD);
		}
		List<NetworkInterface> interfaces = Collections.list(NetworkInterface
			.getNetworkInterfaces());
		for (NetworkInterface intf : interfaces) {
			if (interfaceName != null) {
				if (!intf.getName().equalsIgnoreCase(interfaceName)) continue;
			}
			byte[] mac = intf.getHardwareAddress();
			if (mac == null) return "";
			StringBuilder buf = new StringBuilder();
			for (int idx = 0; idx < mac.length; idx++)
				buf.append(String.format("%02X:", mac[idx]));
			if (buf.length() > 0) buf.deleteCharAt(buf.length() - 1);
			return buf.toString();
		}
		return "";
		/*
		 * try { // this is so Linux hack return
		 * loadFileAsString("/sys/class/net/" +interfaceName +
		 * "/address").toUpperCase().trim(); } catch (IOException ex) { return
		 * null; }
		 */
	}

	/**
	 * Get IP address from first non-localhost interface
	 *
	 * @param useIPv4
	 *            true=return ipv4, false=return ipv6
	 * @return address or empty string
	 * @throws SocketException
	 */
	public static String getIPAddress(boolean useIPv4) throws SocketException {
		List<NetworkInterface> interfaces = Collections.list(NetworkInterface
			.getNetworkInterfaces());
		for (NetworkInterface intf : interfaces) {
			List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
			for (InetAddress addr : addrs) {
				if (!addr.isLoopbackAddress()) {
					String sAddr = addr.getHostAddress();
					boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
					if (useIPv4) {
						if (isIPv4) return sAddr;
					} else {
						if (!isIPv4) {
							// drop ip6 port suffix
							int delim = sAddr.indexOf('%');
							return delim < 0 ? sAddr : sAddr
								.substring(0, delim);
						}
					}
				}
			}
		}
		return "";
	}

	public static boolean hasWifiConnection(Context ctx) {
		ConnectivityManager connec = (ConnectivityManager) ctx
			.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo wifi = connec.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
		return wifi.isAvailable() && wifi.isConnected();
	}

	public static String getCurrentSsid(Context ctx) {
		String ssid = null;
		if (hasWifiConnection(ctx)) {
			WifiManager wm = (WifiManager) ctx
				.getSystemService(Context.WIFI_SERVICE);
			final WifiInfo connectionInfo = wm.getConnectionInfo();
			if (connectionInfo != null) {
				ssid = connectionInfo.getSSID();
				if (ssid != null && "".equals(ssid.trim())) ssid = null;
			}
		}
		return ssid;
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
